package kekmech.ru.feature_map_shared

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class WebMercatorTest {

    // the campus of MPEI
    private val campus = Location(lat = 55.755060, lng = 37.708431)

    private fun assertNear(expected: Double, actual: Double, delta: Double = 1e-6) =
        assertTrue(abs(expected - actual) <= delta, "expected $expected but was $actual")

    @Test
    fun `the center of the world is the center of the map`() {
        val point = WebMercator.project(Location(0.0, 0.0), zoom = 0.0)

        assertNear(128.0, point.x)
        assertNear(128.0, point.y)
    }

    @Test
    fun `the campus is in the tile that every map shows`() {
        // the tile of the campus at zoom 17 in OpenStreetMap: 79265 x 40973 (counted with the formula of the tiles, not with the code under test)
        val tile = WebMercator.visibleTiles(campus, zoom = 17.0, widthPx = 1f, heightPx = 1f).single()

        assertEquals(17, tile.id.z)
        assertEquals(79_265, tile.id.x)
        assertEquals(40_973, tile.id.y)
    }

    @Test
    fun `projecting and back gives the same place`() {
        for (zoom in listOf(3.0, 14.0, 17.5, 20.0)) {
            val back = WebMercator.unproject(WebMercator.project(campus, zoom), zoom)

            assertNear(campus.lat, back.lat, 1e-7)
            assertNear(campus.lng, back.lng, 1e-7)
        }
    }

    @Test
    fun `the center is in the middle of the screen`() {
        val (x, y) = WebMercator.toScreen(campus, center = campus, zoom = 17.5, widthPx = 1080f, heightPx = 1920f)

        assertEquals(540f, x)
        assertEquals(960f, y)
    }

    @Test
    fun `a point of the screen gives the place and the other way`() {
        val place = WebMercator.fromScreen(100f, 200f, campus, 17.5, 1080f, 1920f)
        val (x, y) = WebMercator.toScreen(place, campus, 17.5, 1080f, 1920f)

        assertNear(100.0, x.toDouble(), 0.01)
        assertNear(200.0, y.toDouble(), 0.01)
        // to the east and to the north of the center is to the right and up on the screen
        assertTrue(place.lng < campus.lng)
        assertTrue(place.lat > campus.lat)
    }

    @Test
    fun `the tiles cover the whole screen`() {
        val width = 1080f
        val height = 1920f

        val tiles = WebMercator.visibleTiles(campus, zoom = 17.5, widthPx = width, heightPx = height)

        assertTrue(tiles.all { it.id.z == 17 })
        assertTrue(tiles.minOf { it.left } <= 0f)
        assertTrue(tiles.minOf { it.top } <= 0f)
        assertTrue(tiles.maxOf { it.left + it.size } >= width)
        assertTrue(tiles.maxOf { it.top + it.size } >= height)
        // the tiles at 17.5 are bigger than 256 pixels: 256 * 2^0.5
        assertNear(362.039, tiles.first().size.toDouble(), 0.01)
        assertEquals(tiles.size, tiles.map { it.id to it.left to it.top }.toSet().size)
    }

    @Test
    fun `the tiles of the neighbours are one tile apart`() {
        val tiles = WebMercator.visibleTiles(campus, zoom = 17.0, widthPx = 700f, heightPx = 700f)
        val byId = tiles.associateBy { it.id }
        val tile = tiles.first()
        val right = byId[TileId(tile.id.z, tile.id.x + 1, tile.id.y)]

        assertNear((tile.left + tile.size).toDouble(), right?.left?.toDouble() ?: Double.NaN, 0.01)
        assertNear(tile.top.toDouble(), right?.top?.toDouble() ?: Double.NaN, 0.01)
    }

    @Test
    fun `the world repeats to the left and has no tiles above and below`() {
        val west = Location(lat = 0.0, lng = -179.9)
        val tiles = WebMercator.visibleTiles(west, zoom = 2.0, widthPx = 1000f, heightPx = 3000f)

        assertTrue(tiles.all { it.id.x in 0..3 && it.id.y in 0..3 })
        // the world is 1024 pixels wide, so the tile that is left of the first column is the last column again
        assertTrue(tiles.any { it.id.x == 3 && it.left > 0f && it.left < 256f })
    }
}
