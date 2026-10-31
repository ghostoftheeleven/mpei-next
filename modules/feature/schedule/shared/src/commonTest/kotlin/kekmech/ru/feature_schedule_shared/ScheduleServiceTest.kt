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
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime

internal class ScheduleServiceTest {

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    // Friday 30 October 2026 in Moscow: the current week starts on Monday 26 October
    private val clock = FixedClock(Instant.parse("2026-10-30T09:00:00Z"))

    private val requests = mutableListOf<String>()

    private val searchAnswer = """[{"id":9,"label":"А-01-22","description":"Группа","type":"group"}]"""
    private val classesAnswer =
        """[{"beginLesson":"09:20","endLesson":"10:55","date":"2026.10.27","discipline":"Физика","kindOfWork":"Лекция"}]"""

    private fun service(search: String = searchAnswer): ScheduleService {
        val engine = MockEngine { request ->
            requests += request.url.encodedPath + "?" + request.url.encodedQuery
            when {
                request.url.encodedPath.endsWith("/search") -> respond(search, HttpStatusCode.OK, jsonHeaders)
                else -> respond(classesAnswer, HttpStatusCode.OK, jsonHeaders)
            }
        }
        val api = MpeiScheduleApi(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            baseUrl = { "https://ts.mpei.ru/api/" },
        )
        return ScheduleService(api, clock)
    }

    @Test
    fun `search maps the results`() = runTest {
        val results = service().search("А-01", ScheduleType.GROUP)
        assertEquals(listOf(SearchResult("9", "А-01-22", "Группа", ScheduleType.GROUP)), results)
    }

    @Test
    fun `a week of this week is loaded for the monday of the current week`() = runTest {
        val schedule = service().getWeek(ScheduleType.GROUP, "А-01-22")

        val week = schedule.weeks.single()
        assertEquals(LocalDate(2026, 10, 26), week.firstDayOfWeek)
        assertEquals("Физика", week.days[1].classes.single().name)
        assertEquals("9", schedule.id)
        assertEquals(
            listOf(
                "/api/search?term=%D0%90-01-22&type=group",
                "/api/schedule/group/9?start=2026.10.26&finish=2026.11.01&lng=1",
            ),
            requests,
        )
    }

    @Test
    fun `the week offset moves the week`() = runTest {
        service().getWeek(ScheduleType.GROUP, "А-01-22", weekOffset = 1)
        assertEquals(true, requests.last().contains("start=2026.11.02&finish=2026.11.08"))
    }

    @Test
    fun `the id that search found is remembered`() = runTest {
        val service = service()
        service.search("А-01")
        requests.clear()

        service.getWeek(ScheduleType.GROUP, "а-01-22")

        assertEquals(1, requests.size)
        assertEquals(true, requests.single().startsWith("/api/schedule/group/9"))
    }

    @Test
    fun `a schedule that is not found is an error`() = runTest {
        assertFailsWith<ScheduleNotFoundException> {
            service(search = "[]").getWeek(ScheduleType.GROUP, "нет такой")
        }
    }
}
