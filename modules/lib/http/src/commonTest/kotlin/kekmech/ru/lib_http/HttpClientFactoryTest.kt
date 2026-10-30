package kekmech.ru.lib_http

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.lib_network.AppEnvironment
import kekmech.ru.lib_network.BackendServiceUrl
import kekmech.ru.lib_network.ServiceUrlResolver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class HttpClientFactoryTest {

    private val identity = ClientIdentity(
        userAgent = "mpeixTestClient/1.0",
        deviceId = "device-1",
        deviceOs = "iOS",
        deviceLocale = "ru",
        appVersion = "0.5.2",
    )

    @Serializable
    private data class Item(val id: Long, val name: String? = null)

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @AfterTest
    fun resetEnvironment() {
        ServiceUrlResolver.setAppEnvironment(AppEnvironment.PROD)
    }

    @Test
    fun `every request carries the headers of the app`() = runTest {
        var seen: HttpRequestData? = null
        val engine = MockEngine { request ->
            seen = request
            respond("[]", HttpStatusCode.OK, jsonHeaders)
        }
        createHttpClient(identity, engine).get("https://example.org/ping")

        val headers = seen!!.headers
        assertEquals("mpeixTestClient/1.0", headers[HttpHeaders.UserAgent])
        assertEquals("device-1", headers["X-Device-Id"])
        assertEquals("iOS", headers["X-Device-Os"])
        assertEquals("ru", headers["X-Device-Locale"])
        assertEquals("0.5.2", headers["X-App-Version"])
    }

    @Test
    fun `json is decoded and unknown fields are ignored`() = runTest {
        val engine = MockEngine {
            respond("""[{"id":1,"name":"A","extra":true},{"id":2}]""", HttpStatusCode.OK, jsonHeaders)
        }
        val items: List<Item> = createHttpClient(identity, engine).get("https://example.org/items").body()
        assertEquals(listOf(Item(1, "A"), Item(2, null)), items)
    }

    @Test
    fun `server error is an exception`() = runTest {
        val engine = MockEngine { respond("oops", HttpStatusCode.InternalServerError) }
        assertFailsWith<ServerResponseException> {
            createHttpClient(identity, engine).get("https://example.org/fail")
        }
    }

    @Test
    fun `log goes to the logger when it is given`() = runTest {
        val lines = mutableListOf<String>()
        val engine = MockEngine { respond("[]", HttpStatusCode.OK, jsonHeaders) }
        createHttpClient(identity, engine, logger = { lines += it }).get("https://example.org/log")
        assertEquals(true, lines.any { "example.org/log" in it })
    }

    @Test
    fun `service url depends on the environment`() {
        assertEquals("https://ts.mpei.ru/api/", ServiceUrlResolver.resolve(BackendServiceUrl.MPEI_TIMETABLE))
        ServiceUrlResolver.setAppEnvironment(AppEnvironment.MOCK)
        assertEquals("http://localhost:8080/api/", ServiceUrlResolver.resolve(BackendServiceUrl.MPEI_TIMETABLE))
    }
}
