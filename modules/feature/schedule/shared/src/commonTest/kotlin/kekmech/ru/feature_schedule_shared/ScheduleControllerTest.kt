package kekmech.ru.feature_schedule_shared

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_mpei_api.MpeiScheduleApi
import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ScheduleControllerTest {

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    // Friday 30 October 2026 in Moscow: the current week starts on Monday 26 October
    private val clock = FixedClock(Instant.parse("2026-10-30T09:00:00Z"))

    private val store = InMemoryKeyValueStore()
    private var online = true
    private var requestCount = 0

    private val searchAnswer = """[{"id":9,"label":"А-01-22","description":"Группа","type":"group"}]"""
    private val classesAnswer =
        """[{"beginLesson":"09:20","endLesson":"10:55","date":"2026.10.27","discipline":"Физика","kindOfWork":"Лекция"}]"""

    private val found = SearchResult("9", "А-01-22", "Группа", ScheduleType.GROUP)

    private companion object {
        const val SETTLE_ATTEMPTS = 500
        const val SETTLE_STEP_MILLIS = 10L
    }

    private fun controller(scope: TestScope): ScheduleController {
        val engine = MockEngine { request ->
            requestCount++
            when {
                !online -> respond("", HttpStatusCode.ServiceUnavailable)
                request.url.encodedPath.endsWith("/search") -> respond(searchAnswer, HttpStatusCode.OK, jsonHeaders)
                else -> respond(classesAnswer, HttpStatusCode.OK, jsonHeaders)
            }
        }
        val api = MpeiScheduleApi(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            baseUrl = { "https://ts.mpei.ru/api/" },
        )
        return ScheduleController(
            service = ScheduleService(api, clock),
            cache = ScheduleCache(store),
            scope = scope.backgroundScope,
            clock = clock,
        )
    }

    /**
     * Waits until [controller] has no request going. The mock engine answers on a real dispatcher, so the virtual time
     * of the test (the debounce of the search) and the real time both have to move.
     */
    private suspend fun TestScope.settle(controller: ScheduleController) {
        repeat(SETTLE_ATTEMPTS) {
            advanceUntilIdle()
            val idle = when (val state = controller.state.value) {
                is ScheduleUiState.Search -> !state.busy
                is ScheduleUiState.Week -> !state.loading
            }
            if (idle) return
            withContext(Dispatchers.Default) { delay(SETTLE_STEP_MILLIS) }
        }
        error("The controller is still busy: ${controller.state.value}")
    }

    @Test
    fun `without a chosen schedule the search opens`() = runTest {
        assertIs<ScheduleUiState.Search>(controller(this).state.value)
    }

    @Test
    fun `a short query does not search`() = runTest {
        val controller = controller(this)

        controller.onQueryChange("А")
        settle(controller)

        assertEquals(ScheduleUiState.Search(query = "А"), controller.state.value)
        assertEquals(0, requestCount)
    }

    @Test
    fun `the query is searched and the results are shown`() = runTest {
        val controller = controller(this)

        controller.onQueryChange("А-01")
        assertEquals(true, (controller.state.value as ScheduleUiState.Search).busy)
        settle(controller)

        assertEquals(ScheduleUiState.Search(query = "А-01", results = listOf(found)), controller.state.value)
    }

    @Test
    fun `a failed search shows an error`() = runTest {
        val controller = controller(this)
        online = false

        controller.onQueryChange("А-01")
        settle(controller)

        val state = controller.state.value as ScheduleUiState.Search
        assertNotNull(state.error)
        assertEquals(false, state.busy)
    }

    @Test
    fun `a chosen result loads the week and is remembered`() = runTest {
        val controller = controller(this)
        controller.onQueryChange("А-01")
        settle(controller)

        controller.select(found)
        settle(controller)

        val state = controller.state.value as ScheduleUiState.Week
        assertEquals(SelectedSchedule(ScheduleType.GROUP, "А-01-22"), state.selected)
        assertEquals(false, state.loading)
        assertEquals("Физика", state.schedule?.weeks?.single()?.days?.get(1)?.classes?.single()?.name)
        assertEquals(SelectedSchedule(ScheduleType.GROUP, "А-01-22"), ScheduleCache(store).selected())
    }

    @Test
    fun `the next start opens the chosen week at once`() = runTest {
        val first = controller(this)
        first.onQueryChange("А-01")
        settle(first)
        first.select(found)
        settle(first)

        val second = controller(this)
        val opened = second.state.value as ScheduleUiState.Week
        assertEquals(true, opened.loading)
        assertNull(opened.schedule)

        second.start()
        settle(second)

        assertNotNull((second.state.value as ScheduleUiState.Week).schedule)
    }

    @Test
    fun `without the network a loaded week is shown from the copy`() = runTest {
        val controller = controller(this)
        controller.onQueryChange("А-01")
        settle(controller)
        controller.select(found)
        settle(controller)

        online = false
        controller.refresh()
        settle(controller)

        val state = controller.state.value as ScheduleUiState.Week
        assertTrue(state.offline)
        assertNotNull(state.schedule)
        assertNull(state.error)
    }

    @Test
    fun `without the network a week that was never loaded is an error`() = runTest {
        val controller = controller(this)
        controller.onQueryChange("А-01")
        settle(controller)
        controller.select(found)
        settle(controller)

        online = false
        controller.shiftWeek(1)
        settle(controller)

        val state = controller.state.value as ScheduleUiState.Week
        assertEquals(1, state.weekOffset)
        assertNull(state.schedule)
        assertNotNull(state.error)
        assertEquals(false, state.offline)
    }

    @Test
    fun `the week moves forward and back`() = runTest {
        val controller = controller(this)
        controller.onQueryChange("А-01")
        settle(controller)
        controller.select(found)
        settle(controller)

        controller.shiftWeek(1)
        settle(controller)
        assertEquals(1, (controller.state.value as ScheduleUiState.Week).weekOffset)

        controller.shiftWeek(-1)
        settle(controller)
        assertEquals(0, (controller.state.value as ScheduleUiState.Week).weekOffset)
    }

    @Test
    fun `changing the schedule goes back to the search and forgets the choice`() = runTest {
        val controller = controller(this)
        controller.onQueryChange("А-01")
        settle(controller)
        controller.select(found)
        settle(controller)

        controller.changeSchedule()

        assertEquals(ScheduleUiState.Search(), controller.state.value)
        assertNull(ScheduleCache(store).selected())
    }

    @Test
    fun `the cache keeps only the last weeks`() = runTest {
        val cache = ScheduleCache(store)
        val selected = SelectedSchedule(ScheduleType.GROUP, "А-01-22")
        val controller = controller(this)
        controller.onQueryChange("А-01")
        settle(controller)
        controller.select(found)
        settle(controller)
        val schedule = (controller.state.value as ScheduleUiState.Week).schedule!!
        val monday = schedule.weeks.single().firstDayOfWeek

        repeat(ScheduleCache.MAX_WEEKS + 3) { index ->
            val week = schedule.weeks.single().copy(
                firstDayOfWeek = kotlinx.datetime.LocalDate(2030, 1, 1 + index),
            )
            cache.saveWeek(selected, schedule.copy(weeks = listOf(week)))
        }

        assertNull(cache.week(selected, monday))
        assertNotNull(cache.week(selected, kotlinx.datetime.LocalDate(2030, 1, 1 + ScheduleCache.MAX_WEEKS + 2)))
    }
}
