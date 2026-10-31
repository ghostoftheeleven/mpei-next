package kekmech.ru.feature_map_shared

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

/** The size of a tile in the world pixels of its zoom level. */
public const val TILE_SIZE: Int = 256

/** The highest latitude that Web Mercator can draw. */
private const val MAX_LATITUDE = 85.05112878

private const val DEGREES_IN_HALF_TURN = 180.0
private const val DEGREES_IN_TURN = 360.0

/** A tile of the pyramid of the tiles: [x] and [y] are counted at the zoom level [z]. */
public data class TileId(val z: Int, val x: Int, val y: Int)

/** A tile that is placed on the screen: the position of its top left corner and its size, in the pixels of the screen. */
public data class PlacedTile(val id: TileId, val left: Float, val top: Float, val size: Float)

/** A point in the pixels of the world at some zoom level; one tile is [TILE_SIZE] pixels. */
public data class WorldPoint(val x: Double, val y: Double)

/** The math of the Web Mercator projection (the one the tiles of Esri, OpenStreetMap and Google use). */
public object WebMercator {

    /** The size of the whole world in the pixels at [zoom] (the zoom can be fractional). */
    public fun worldSize(zoom: Double): Double = TILE_SIZE * 2.0.pow(zoom)

    public fun project(location: Location, zoom: Double): WorldPoint {
        val lat = location.lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val size = worldSize(zoom)
        val x = (location.lng + DEGREES_IN_HALF_TURN) / DEGREES_IN_TURN * size
        val tanLat = tan(lat * PI / DEGREES_IN_HALF_TURN)
        val mercator = ln(tanLat + sqrt(1 + tanLat * tanLat))
        val y = (1 - mercator / PI) / 2 * size
        return WorldPoint(x, y)
    }

    public fun unproject(point: WorldPoint, zoom: Double): Location {
        val size = worldSize(zoom)
        val lng = point.x / size * DEGREES_IN_TURN - DEGREES_IN_HALF_TURN
        val n = PI * (1 - 2 * point.y / size)
        val lat = atan(sinh(n)) * DEGREES_IN_HALF_TURN / PI
        return Location(lat = lat, lng = lng)
    }

    /**
     * The tiles that cover a screen of [widthPx] by [heightPx] pixels whose center is [center] at [zoom]. The zoom is
     * fractional: the tiles are of the level `floor(zoom)` and are scaled. The tiles to the left and to the right of the
     * world are the same tiles again (the world repeats), the ones above and below it do not exist.
     */
    public fun visibleTiles(center: Location, zoom: Double, widthPx: Float, heightPx: Float): List<PlacedTile> {
        val level = floor(zoom).toInt().coerceAtLeast(0)
        val scale = 2.0.pow(zoom - level)
        val centerPoint = project(center, level.toDouble())
        val left = centerPoint.x - widthPx / (2 * scale)
        val top = centerPoint.y - heightPx / (2 * scale)
        val right = left + widthPx / scale
        val bottom = top + heightPx / scale

        val tilesInRow = 1 shl level
        val firstColumn = floor(left / TILE_SIZE).toInt()
        val lastColumn = floor(right / TILE_SIZE).toInt()
        val firstRow = max(0, floor(top / TILE_SIZE).toInt())
        val lastRow = min(tilesInRow - 1, floor(bottom / TILE_SIZE).toInt())

        val tiles = mutableListOf<PlacedTile>()
        for (row in firstRow..lastRow) {
            for (column in firstColumn..lastColumn) {
                tiles += PlacedTile(
                    id = TileId(level, column.mod(tilesInRow), row),
                    left = ((column * TILE_SIZE - left) * scale).toFloat(),
                    top = ((row * TILE_SIZE - top) * scale).toFloat(),
                    size = (TILE_SIZE * scale).toFloat(),
                )
            }
        }
        return tiles
    }

    /** Where a [location] is on the screen of [widthPx] by [heightPx] pixels whose center is [center] at [zoom]. */
    public fun toScreen(location: Location, center: Location, zoom: Double, widthPx: Float, heightPx: Float): Pair<Float, Float> {
        val point = project(location, zoom)
        val middle = project(center, zoom)
        return Pair((widthPx / 2 + (point.x - middle.x)).toFloat(), (heightPx / 2 + (point.y - middle.y)).toFloat())
    }

    /** The location under a point of the screen; the opposite of [toScreen]. */
    public fun fromScreen(x: Float, y: Float, center: Location, zoom: Double, widthPx: Float, heightPx: Float): Location {
        val middle = project(center, zoom)
        return unproject(WorldPoint(middle.x + (x - widthPx / 2), middle.y + (y - heightPx / 2)), zoom)
    }
}
