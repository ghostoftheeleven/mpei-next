package kekmech.ru.feature_map_shared

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class MapControllerTest {

    private val plainHeaders = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8")

    private val answer = """[
        {"uid":"1","name":"Корпус Е","address":"Красноказарменная ул., 14","location":{"lat":55.755,"lng":37.7},"type":"BUILDING"},
        {"uid":"2","name":"Столовая","address":"Энергетическая ул., 13","location":{"lat":55.757,"lng":37.71},"type":"FOOD"},
        {"uid":"3","name":"Кафе Е","address":"Корпус Е","location":{"lat":55.75,"lng":37.71},"type":"FOOD"},
        {"uid":"4","name":"Общежитие 1","location":{"lat":55.75,"lng":37.72},"type":"HOSTEL"}
    ]"""

    private var online = true
    private val store = InMemoryKeyValueStore()

    private fun controller(scope: TestScope): MapController {
        val engine = MockEngine {
            if (online) respond(answer, HttpStatusCode.OK, plainHeaders) else respond("", HttpStatusCode.ServiceUnavailable)
        }
        val repository = MapMarkersRepository(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            store = store,
            baseUrl = { "https://example.org/map/" },
        )
        return MapController(repository, store, scope.backgroundScope)
    }

    /** The mock engine answers on a real dispatcher, so the virtual time and the real time both have to move. */
    private suspend fun TestScope.settle(controller: MapController) {
        repeat(SETTLE_ATTEMPTS) {
            advanceUntilIdle()
            if (!controller.state.value.loading) return
            withContext(Dispatchers.Default) { delay(SETTLE_STEP_MILLIS) }
        }
        error("The controller is still loading")
    }

    @Test
    fun `the food tab is the first and the markers load`() = runTest {
        val controller = controller(this)

        controller.start()
        assertEquals(true, controller.state.value.loading)
        settle(controller)

        val state = controller.state.value
        assertEquals(MarkerType.FOOD, state.tab)
        assertEquals(4, state.markers.size)
        assertEquals(listOf("Столовая", "Кафе Е"), state.visible.map { it.name })
        assertNull(state.error)
        assertEquals(false, state.offline)
    }

    @Test
    fun `a tab shows its markers and drops the selection and the search`() = runTest {
        val controller = controller(this)
        controller.start()
        settle(controller)
        controller.setQuery("корпус")
        controller.select(controller.state.value.visible.first())

        controller.selectTab(MarkerType.HOSTEL)

        val state = controller.state.value
        assertEquals(listOf("Общежитие 1"), state.visible.map { it.name })
        assertEquals("", state.query)
        assertNull(state.selected)
    }

    @Test
    fun `a search looks in all the tabs`() = runTest {
        val controller = controller(this)
        controller.start()
        settle(controller)

        controller.setQuery("корпус")

        // the building by its name and the cafe by its address, though the tab is the food
        assertEquals(listOf("Корпус Е", "Кафе Е"), controller.state.value.visible.map { it.name })
    }

    @Test
    fun `a marker is selected and the selection is taken off`() = runTest {
        val controller = controller(this)
        controller.start()
        settle(controller)
        val marker = controller.state.value.markers.first()

        controller.select(marker)
        assertEquals(marker, controller.state.value.selected)

        controller.select(null)
        assertNull(controller.state.value.selected)
    }

    @Test
    fun `without the network the copy is used and then the error comes back with a retry`() = runTest {
        online = false
        val failing = controller(this)
        failing.start()
        settle(failing)
        assertNotNull(failing.state.value.error)
        assertEquals(emptyList(), failing.state.value.markers)

        online = true
        failing.start()
        settle(failing)
        assertNull(failing.state.value.error)
        assertEquals(4, failing.state.value.markers.size)

        online = false
        val offline = controller(this)
        offline.start()
        settle(offline)
        assertEquals(true, offline.state.value.offline)
        assertEquals(4, offline.state.value.markers.size)
    }

    @Test
    fun `the look of the map is kept for the next start`() = runTest {
        val controller = controller(this)
        assertEquals(APPEARANCE_SCHEME, controller.state.value.appearance)

        controller.setAppearance(APPEARANCE_HYBRID)

        assertEquals(APPEARANCE_HYBRID, controller(this).state.value.appearance)
    }

    @Test
    fun `a place from a link is shown with its tab and the camera`() = runTest {
        val controller = controller(this)
        controller.start()
        settle(controller)

        controller.openPlace("4")

        val state = controller.state.value
        assertEquals(MarkerType.HOSTEL, state.tab)
        assertEquals("4", state.selected?.uid)
        assertEquals(Location(55.75, 37.72), state.focus?.location)
    }

    @Test
    fun `a place from a link that came before the markers is shown when they are loaded`() = runTest {
        val controller = controller(this)

        controller.openPlace("1")
        controller.start()
        settle(controller)

        assertEquals(MarkerType.BUILDING, controller.state.value.tab)
        assertEquals("1", controller.state.value.selected?.uid)
    }

    @Test
    fun `a tab from a link is shown and an unknown tab is ignored`() = runTest {
        val controller = controller(this)

        controller.openTab(MarkerType.HOSTEL)
        assertEquals(MarkerType.HOSTEL, controller.state.value.tab)

        controller.openTab(MarkerType.UNDEFINED)
        assertEquals(MarkerType.HOSTEL, controller.state.value.tab)
    }

    @Test
    fun `a tap in the list moves the camera and a tap on the map does not`() = runTest {
        val controller = controller(this)
        controller.start()
        settle(controller)
        val marker = controller.state.value.visible.first()

        controller.select(marker)
        assertNull(controller.state.value.focus)

        controller.select(marker, focus = true)
        val first = controller.state.value.focus
        controller.select(marker, focus = true)

        assertEquals(marker.location, first?.location)
        // the same place asked again is a new request
        assertEquals(true, controller.state.value.focus != first)
    }

    @Test
    fun `the reset takes the selection off and goes back to the campus`() = runTest {
        val controller = controller(this)
        controller.start()
        settle(controller)
        controller.select(controller.state.value.visible.first(), focus = true)

        controller.resetView()

        assertNull(controller.state.value.selected)
        assertEquals(CampusCenter, controller.state.value.focus?.location)
    }

    @Test
    fun `the look of the map that was changed elsewhere is read again`() = runTest {
        val controller = controller(this)
        assertEquals(APPEARANCE_SCHEME, controller.state.value.appearance)

        KeyValueAppearanceStorage(store).set(APPEARANCE_HYBRID)
        controller.syncAppearance()

        assertEquals(APPEARANCE_HYBRID, controller.state.value.appearance)
    }

    private companion object {
        const val SETTLE_ATTEMPTS = 500
        const val SETTLE_STEP_MILLIS = 10L
    }
}
