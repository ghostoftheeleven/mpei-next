package kekmech.ru.feature_map_shared

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class MapMarkersTest {

    // GitHub serves the file as text/plain
    private val plainHeaders = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8")

    private val answer = """[
        {"uid":"1","name":"Корпус Е","address":"Красноказарменная ул., 14","location":{"lat":55.755,"lng":37.7},"type":"BUILDING","tag":"ЭнМИ"},
        {"uid":"2","name":"Столовая","address":"Энергетическая ул., 13","location":{"lat":55.757,"lng":37.71},"type":"FOOD"},
        {"uid":"3","name":"Новое место","location":{"lat":55.75,"lng":37.71},"type":"SOMETHING_NEW"}
    ]"""

    private var online = true
    private val store = InMemoryKeyValueStore()

    private fun repository(): MapMarkersRepository {
        val engine = MockEngine {
            if (online) respond(answer, HttpStatusCode.OK, plainHeaders) else respond("", HttpStatusCode.ServiceUnavailable)
        }
        return MapMarkersRepository(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            store = store,
            baseUrl = { "https://example.org/map/" },
        )
    }

    @Test
    fun `the markers are read from a text file and an unknown type is undefined`() = runTest {
        val loaded = repository().load()

        assertEquals(false, loaded.fromCache)
        assertEquals(listOf("Корпус Е", "Столовая", "Новое место"), loaded.markers.map { it.name })
        assertEquals(MarkerType.UNDEFINED, loaded.markers[2].type)
        assertEquals("", loaded.markers[2].address)
        assertEquals(55.755, loaded.markers[0].location.lat)
    }

    @Test
    fun `without the network the last answer is used`() = runTest {
        repository().load()
        online = false

        val loaded = repository().load()

        assertEquals(true, loaded.fromCache)
        assertEquals(3, loaded.markers.size)
    }

    @Test
    fun `without the network and a copy it is an error`() = runTest {
        online = false

        assertFailsWith<Exception> { repository().load() }
        assertNull(repository().cached())
    }

    @Test
    fun `the copy that comes with the app is used when there is nothing else`() = runTest {
        online = false
        val engine = MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }
        val repository = MapMarkersRepository(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            store = store,
            baseUrl = { "https://example.org/map/" },
            bundled = { answer },
        )

        val loaded = repository.load()

        assertEquals(true, loaded.fromCache)
        assertEquals(3, loaded.markers.size)
    }

    @Test
    fun `the filter looks at the tab and at the name the address and the tag`() = runTest {
        val markers = repository().load().markers

        assertEquals(listOf("Столовая"), filterMarkers(markers, type = MarkerType.FOOD).map { it.name })
        assertEquals(listOf("Корпус Е"), filterMarkers(markers, query = "корпус").map { it.name })
        assertEquals(listOf("Столовая"), filterMarkers(markers, query = "энергетическая").map { it.name })
        assertEquals(listOf("Корпус Е"), filterMarkers(markers, query = "энми").map { it.name })
        assertEquals(emptyList(), filterMarkers(markers, type = MarkerType.FOOD, query = "корпус"))
        assertEquals(3, filterMarkers(markers, query = "   ").size)
    }
}
