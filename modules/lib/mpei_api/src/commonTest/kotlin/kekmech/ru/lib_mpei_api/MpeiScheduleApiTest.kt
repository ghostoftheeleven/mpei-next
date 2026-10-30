package kekmech.ru.lib_mpei_api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MpeiScheduleApiTest {

    private val identity = ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(body: String, status: HttpStatusCode = HttpStatusCode.OK): MpeiScheduleApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, jsonHeaders)
        }
        return MpeiScheduleApi(createHttpClient(identity, engine), baseUrl = { "https://ts.mpei.ru/api/" })
    }

    @Test
    fun `search sends the term and the type and reads the results`() = runTest {
        val results = api("""[{"id":9,"label":"А-01-22","description":"Группа","type":"group","extra":1}]""")
            .search(term = "А-01", type = "group")

        assertEquals(listOf(MpeiSearchResultDto(9, "А-01-22", "Группа", "group")), results)
        val url = requests.single().url
        assertEquals("/api/search", url.encodedPath)
        assertEquals("А-01", url.parameters["term"])
        assertEquals("group", url.parameters["type"])
    }

    @Test
    fun `search without a type does not send it`() = runTest {
        api("[]").search(term = "Иванов")
        assertEquals(null, requests.single().url.parameters["type"])
    }

    @Test
    fun `getSchedule puts the type and the id into the path`() = runTest {
        val classes = api(
            """[{"beginLesson":"09:20","endLesson":"10:55","date":"2026.10.30","discipline":"Физика","auditorium":"Е-101"}]""",
        ).getSchedule(type = "group", id = 9, start = "2026.10.26", finish = "2026.11.01")

        assertEquals("Физика", classes.single().discipline)
        assertEquals("Е-101", classes.single().auditorium)
        val url = requests.single().url
        assertEquals("/api/schedule/group/9", url.encodedPath)
        assertEquals("2026.10.26", url.parameters["start"])
        assertEquals("2026.11.01", url.parameters["finish"])
        assertEquals("1", url.parameters["lng"])
    }

    @Test
    fun `getWeek asks for Monday to Sunday`() = runTest {
        api("[]").getWeek(type = "person", id = 5, monday = LocalDate(2026, 10, 26))
        val url = requests.single().url
        assertEquals("2026.10.26", url.parameters["start"])
        assertEquals("2026.11.01", url.parameters["finish"])
    }

    @Test
    fun `formatMpeiDate pads the month and the day`() {
        assertEquals("2026.03.07", formatMpeiDate(LocalDate(2026, 3, 7)))
    }

    @Test
    fun `an error status is an exception`() = runTest {
        assertFailsWith<ClientRequestException> { api("{}", HttpStatusCode.NotFound).search("x") }
    }
}
