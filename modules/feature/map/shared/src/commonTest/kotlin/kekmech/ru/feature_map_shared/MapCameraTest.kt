package kekmech.ru.feature_map_shared

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MapCameraTest {

    private val width = 1080f
    private val height = 1920f
    private val camera = MapCamera()

    private fun assertNear(expected: Double, actual: Double, delta: Double) =
        assertTrue(abs(expected - actual) <= delta, "expected $expected but was $actual")

    @Test
    fun `dragging the map to the right shows the places on the west`() {
        val dragged = camera.panned(dx = 200f, dy = 0f, widthPx = width, heightPx = height)

        assertTrue(dragged.center.lng < camera.center.lng)
        assertNear(camera.center.lat, dragged.center.lat, 1e-9)
        // the old center is now 200 pixels to the right of the middle of the screen
        val (x, y) = WebMercator.toScreen(camera.center, dragged.center, dragged.zoom, width, height)
        assertNear(740.0, x.toDouble(), 0.01)
        assertNear(960.0, y.toDouble(), 0.01)
    }

    @Test
    fun `dragging the map down shows the places on the north`() {
        val dragged = camera.panned(dx = 0f, dy = 300f, widthPx = width, heightPx = height)

        assertTrue(dragged.center.lat > camera.center.lat)
    }

    @Test
    fun `the place under the focus stays under it when the zoom changes`() {
        val focusX = 800f
        val focusY = 400f
        val before = WebMercator.fromScreen(focusX, focusY, camera.center, camera.zoom, width, height)

        val zoomed = camera.zoomedAround(newZoom = 19.0, focusX = focusX, focusY = focusY, widthPx = width, heightPx = height)

        val (x, y) = WebMercator.toScreen(before, zoomed.center, zoomed.zoom, width, height)
        assertEquals(19.0, zoomed.zoom)
        assertNear(focusX.toDouble(), x.toDouble(), 0.01)
        assertNear(focusY.toDouble(), y.toDouble(), 0.01)
    }

    @Test
    fun `zooming around the middle does not move the center`() {
        val zoomed = camera.zoomedAround(18.5, width / 2, height / 2, width, height)

        assertNear(camera.center.lat, zoomed.center.lat, 1e-9)
        assertNear(camera.center.lng, zoomed.center.lng, 1e-9)
    }

    @Test
    fun `the zoom stays between the limits`() {
        assertEquals(MAX_ZOOM, camera.zoomedAround(99.0, 0f, 0f, width, height).zoom)
        assertEquals(MIN_ZOOM, camera.zoomedAround(1.0, 0f, 0f, width, height).zoom)
    }

    @Test
    fun `the camera does not go beyond the poles`() {
        val north = MapCamera(Location(lat = 84.9, lng = 0.0), zoom = 14.0)

        val dragged = north.panned(dx = 0f, dy = 100_000f, widthPx = width, heightPx = height)

        assertTrue(dragged.center.lat <= 85.0)
    }

    @Test
    fun `looking at a place moves the center and keeps the zoom in the limits`() {
        val place = Location(55.75, 37.7)

        assertEquals(MapCamera(place, 17.5), camera.lookingAt(place))
        assertEquals(MAX_ZOOM, camera.lookingAt(place, zoom = 25.0).zoom)
    }

    @Test
    fun `the closest marker within the radius is the one that is tapped`() {
        val near = marker("near", CampusCenter)
        val nearer = marker("nearer", Location(CampusCenter.lat, CampusCenter.lng + 0.00002))
        val far = marker("far", Location(CampusCenter.lat + 0.01, CampusCenter.lng))

        val tapped = markerAt(listOf(near, nearer, far), 540f, 960f, camera, width, height, radiusPx = 60f)

        assertEquals("near", tapped?.uid)
        assertNull(markerAt(listOf(far), 540f, 960f, camera, width, height, radiusPx = 60f))
        assertNull(markerAt(emptyList(), 540f, 960f, camera, width, height, radiusPx = 60f))
    }

    @Test
    fun `tiles of a level that the source does not have are scaled from the highest one`() {
        val tiles = WebMercator.visibleTiles(CampusCenter, zoom = 20.0, widthPx = 1080f, heightPx = 1920f, maxLevel = 19)

        assertTrue(tiles.all { it.id.z == 19 })
        assertNear(512.0, tiles.first().size.toDouble(), 0.01)
    }

    private fun marker(uid: String, location: Location) = MapMarker(uid = uid, name = uid, location = location)
}
