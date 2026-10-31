package kekmech.ru.feature_map_shared

import kotlin.math.max
import kotlin.math.min

/** The default view of the map: the campus of MPEI. */
public val CampusCenter: Location = Location(lat = 55.755060, lng = 37.708431)

public const val DEFAULT_ZOOM: Double = 17.5
public const val MIN_ZOOM: Double = 14.0
public const val MAX_ZOOM: Double = 20.0

private const val MAX_LATITUDE = 85.0

/**
 * Where the map looks: the [center] and the [zoom] (fractional). The changes return a new camera, so the math can be
 * tested without a screen; the screen only keeps the current camera and gives it its size.
 */
public data class MapCamera(
    val center: Location = CampusCenter,
    val zoom: Double = DEFAULT_ZOOM,
) {

    /** The camera after the map was dragged by [dx] and [dy] pixels (the finger moved right and down). */
    public fun panned(dx: Float, dy: Float, widthPx: Float, heightPx: Float): MapCamera {
        val middle = WebMercator.project(center, zoom)
        val moved = WebMercator.unproject(WorldPoint(middle.x - dx, middle.y - dy), zoom)
        return copy(center = moved.clamped())
    }

    /**
     * The camera after the zoom was changed to [newZoom] around the point ([focusX], [focusY]) of the screen: the place
     * under that point stays under it (for a pinch it is the middle between the fingers).
     */
    public fun zoomedAround(
        newZoom: Double,
        focusX: Float,
        focusY: Float,
        widthPx: Float,
        heightPx: Float,
        minZoom: Double = MIN_ZOOM,
        maxZoom: Double = MAX_ZOOM,
    ): MapCamera {
        val zoomed = newZoom.coerceIn(minZoom, maxZoom)
        val place = WebMercator.fromScreen(focusX, focusY, center, zoom, widthPx, heightPx)
        val focus = WebMercator.project(place, zoomed)
        val newCenter = WebMercator.unproject(
            WorldPoint(focus.x - (focusX - widthPx / 2), focus.y - (focusY - heightPx / 2)),
            zoomed,
        )
        return MapCamera(center = newCenter.clamped(), zoom = zoomed)
    }

    /** The camera that looks at [location] with the same zoom, or with [zoom]. */
    public fun lookingAt(location: Location, zoom: Double = this.zoom): MapCamera =
        MapCamera(center = location.clamped(), zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))

    private fun Location.clamped() = Location(
        lat = max(-MAX_LATITUDE, min(MAX_LATITUDE, lat)),
        lng = lng,
    )
}

/**
 * The marker under the point ([x], [y]) of the screen, or `null`: the closest one that is not further than
 * [radiusPx] pixels from the point.
 */
public fun markerAt(
    markers: List<MapMarker>,
    x: Float,
    y: Float,
    camera: MapCamera,
    widthPx: Float,
    heightPx: Float,
    radiusPx: Float,
): MapMarker? =
    markers
        .map { marker ->
            val (mx, my) = WebMercator.toScreen(marker.location, camera.center, camera.zoom, widthPx, heightPx)
            marker to ((mx - x) * (mx - x) + (my - y) * (my - y))
        }
        .filter { (_, distanceSquared) -> distanceSquared <= radiusPx * radiusPx }
        .minByOrNull { (_, distanceSquared) -> distanceSquared }
        ?.first
