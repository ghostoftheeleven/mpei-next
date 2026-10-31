@file:OptIn(ExperimentalTime::class)

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class DashboardTest {

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    // Friday 30 October 2026, 12:00 in Moscow: the current week starts on Monday 26 October
    private val clock = FixedClock(Instant.parse("2026-10-30T09:00:00Z"))
    private val selected = SelectedSchedule(ScheduleType.GROUP, "А-01-22")

    private var thisWeekOnline = true
    private var nextWeekOnline = true

    private val search = """[{"id":9,"label":"А-01-22","description":"Группа","type":"group"}]"""

    private fun classes(date: String, begin: String, end: String, name: String) =
        """{"beginLesson":"$begin","endLesson":"$end","date":"$date","discipline":"$name","kindOfWork":"Лекция"}"""

    private val thisWeekAnswer = "[" + listOf(
        classes("2026.10.30", "09:20", "10:55", "Утренняя"),
        classes("2026.10.30", "13:45", "15:20", "Дневная"),
    ).joinToString(",") + "]"
    private val nextWeekAnswer = "[" + classes("2026.11.02", "09:20", "10:55", "Понедельник") + "]"

    private fun controller(scope: TestScope): DashboardController {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val start = request.url.parameters["start"]
            when {
                path.endsWith("/search") -> respond(search, HttpStatusCode.OK, jsonHeaders)
                start == "2026.10.26" ->
                    if (thisWeekOnline) respond(thisWeekAnswer, HttpStatusCode.OK, jsonHeaders) else unavailable()
                else ->
                    if (nextWeekOnline) respond(nextWeekAnswer, HttpStatusCode.OK, jsonHeaders) else unavailable()
            }
        }
        val api = MpeiScheduleApi(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            baseUrl = { "https://ts.mpei.ru/api/" },
        )
        val loader = WeekLoader(ScheduleService(api, clock), ScheduleCache(InMemoryKeyValueStore()), clock)
        return DashboardController(loader, scope.backgroundScope, clock)
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.unavailable() =
        respond("", HttpStatusCode.ServiceUnavailable)

    /** The mock engine answers on a real dispatcher, so the virtual time and the real time both have to move. */
    private suspend fun TestScope.settle(
        controller: DashboardController,
        until: (DashboardUiState) -> Boolean = { it !is DashboardUiState.Loading },
    ) {
        repeat(SETTLE_ATTEMPTS) {
            advanceUntilIdle()
            if (until(controller.state.value)) return
            withContext(Dispatchers.Default) { delay(SETTLE_STEP_MILLIS) }
        }
        error("The controller did not get there: ${controller.state.value}")
    }

    private fun day(date: LocalDate, vararg classes: Classes) =
        Day(dayOfWeek = date.dayOfWeek.ordinal + 1, date = date, classes = classes.toList())

    private fun lesson(name: String, start: LocalTime, end: LocalTime) = Classes(
        name = name,
        type = ClassesType.LECTURE,
        rawType = null,
        place = "",
        groups = "",
        person = "",
        time = Time(start, end),
        number = 1,
    )

    @Test
    fun `only the classes that have not ended and are near are upcoming`() {
        val today = LocalDate(2026, 10, 30)
        val week = Week(
            weekOfYear = 44,
            weekOfSemester = WeekOfSemester.Studying(9),
            firstDayOfWeek = LocalDate(2026, 10, 26),
            days = listOf(
                day(
                    today,
                    lesson("ended", LocalTime(9, 20), LocalTime(10, 55)),
                    lesson("going", LocalTime(11, 10), LocalTime(12, 45)),
                    lesson("later", LocalTime(13, 45), LocalTime(15, 20)),
                ),
                day(LocalDate(2026, 11, 5), lesson("in a week", LocalTime(9, 20), LocalTime(10, 55))),
                day(LocalDate(2026, 11, 6), lesson("too far", LocalTime(9, 20), LocalTime(10, 55))),
                day(LocalDate(2026, 10, 29), lesson("yesterday", LocalTime(9, 20), LocalTime(10, 55))),
            ),
        )

        val upcoming = upcomingClasses(listOf(week), LocalDateTime(2026, 10, 30, 12, 0))

        assertEquals(listOf("going", "later", "in a week"), upcoming.map { it.classes.name })
        assertEquals(listOf(true, false, false), upcoming.map { it.inProgress })
    }

    @Test
    fun `a class that ends exactly now is over`() {
        val today = LocalDate(2026, 10, 30)
        val week = Week(44, WeekOfSemester.NonStudying, today, listOf(day(today, lesson("x", LocalTime(9, 20), LocalTime(12, 0)))))

        assertEquals(emptyList(), upcomingClasses(listOf(week), LocalDateTime(2026, 10, 30, 12, 0)))
    }

    @Test
    fun `without a chosen schedule there is nothing to show`() = runTest {
        val controller = controller(this)
        controller.show(null)
        assertEquals(DashboardUiState.NoSchedule, controller.state.value)
    }

    @Test
    fun `this week and the next one give the upcoming classes`() = runTest {
        val controller = controller(this)

        controller.show(selected)
        assertEquals(DashboardUiState.Loading(selected), controller.state.value)
        settle(controller)

        val state = assertIs<DashboardUiState.Content>(controller.state.value)
        assertEquals(LocalDate(2026, 10, 30), state.today)
        assertEquals(WeekOfSemester.Studying(9), state.weekOfSemester)
        assertEquals(listOf("Дневная", "Понедельник"), state.upcoming.map { it.classes.name })
        assertEquals(false, state.offline)
    }

    @Test
    fun `the dashboard works without the next week`() = runTest {
        nextWeekOnline = false
        val controller = controller(this)

        controller.show(selected)
        settle(controller)

        val state = assertIs<DashboardUiState.Content>(controller.state.value)
        assertEquals(listOf("Дневная"), state.upcoming.map { it.classes.name })
    }

    @Test
    fun `without the network and a copy it fails and with a copy it is offline`() = runTest {
        thisWeekOnline = false
        nextWeekOnline = false
        val controller = controller(this)
        controller.show(selected)
        settle(controller)
        assertIs<DashboardUiState.Failed>(controller.state.value)

        thisWeekOnline = true
        nextWeekOnline = true
        controller.refresh()
        settle(controller)
        assertEquals(false, assertIs<DashboardUiState.Content>(controller.state.value).offline)

        thisWeekOnline = false
        controller.refresh()
        settle(controller) { it is DashboardUiState.Content && it.offline }
        val state = assertIs<DashboardUiState.Content>(controller.state.value)
        assertTrue(state.offline)
        assertEquals(listOf("Дневная", "Понедельник"), state.upcoming.map { it.classes.name })
    }

    private companion object {
        const val SETTLE_ATTEMPTS = 500
        const val SETTLE_STEP_MILLIS = 10L
    }
}
