package kekmech.ru.ui_map

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kekmech.ru.feature_map_shared.MapCamera
import kekmech.ru.feature_map_shared.MapMarker
import kekmech.ru.feature_map_shared.MarkerType
import kekmech.ru.feature_map_shared.PlacedTile
import kekmech.ru.feature_map_shared.TileId
import kekmech.ru.feature_map_shared.WebMercator
import kekmech.ru.feature_map_shared.markerAt
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToInt

/** The camera of a map. The screen changes it with the fingers; the code changes it to show a place. */
@Stable
public class MapState(initial: MapCamera = MapCamera()) {

    public var camera: MapCamera by mutableStateOf(initial)
}

private const val TAP_RADIUS_DP = 28
private const val MARKER_RADIUS_DP = 8
private const val SELECTED_RADIUS_DP = 12
private const val MAX_PARENT_LEVELS = 4
private const val LN_2 = 0.6931471805599453
private val BACKGROUND = Color(0xFFE5E3DF)

/**
 * The map of the campus: the tiles of [appearance] and the [markers] on them. One finger drags the map, two fingers
 * zoom it, a double tap zooms it in by one level, a tap on a marker calls [onMarkerClick] with it (a tap on the empty
 * place calls it with `null`).
 *
 * @param selected the marker that is drawn bigger and with its name
 */
@Composable
public fun MpeiMap(
    state: MapState,
    markers: List<MapMarker>,
    selected: MapMarker?,
    appearance: MapAppearance,
    tileLoader: TileLoader,
    onMarkerClick: (MapMarker?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()
    Box(modifier.clipToBounds().background(BACKGROUND).onSizeChanged { size = it }) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        var camera = state.camera.panned(pan.x, pan.y, width, height)
                        if (zoomChange != 1f) {
                            val newZoom = camera.zoom + ln(zoomChange.toDouble()) / LN_2
                            camera = camera.zoomedAround(newZoom, centroid.x, centroid.y, width, height)
                        }
                        state.camera = camera
                    }
                }
                .pointerInput(markers) {
                    detectTapGestures(
                        onDoubleTap = { point ->
                            val camera = state.camera
                            state.camera = camera.zoomedAround(
                                newZoom = camera.zoom + 1,
                                focusX = point.x,
                                focusY = point.y,
                                widthPx = size.width.toFloat(),
                                heightPx = size.height.toFloat(),
                            )
                        },
                        onTap = { point ->
                            onMarkerClick(
                                markerAt(
                                    markers = markers,
                                    x = point.x,
                                    y = point.y,
                                    camera = state.camera,
                                    widthPx = size.width.toFloat(),
                                    heightPx = size.height.toFloat(),
                                    radiusPx = TAP_RADIUS_DP * density,
                                ),
                            )
                        },
                    )
                },
        ) {
            val camera = state.camera
            for (layer in appearance.layers) {
                drawLayer(layer, camera, tileLoader)
            }
            drawMarkers(markers, selected, camera, textMeasurer)
        }
        Text(
            text = appearance.attribution,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            color = Color.Black,
        )
    }
}

private fun DrawScope.drawLayer(layer: TileLayer, camera: MapCamera, loader: TileLoader) {
    val tiles = WebMercator.visibleTiles(camera.center, camera.zoom, size.width, size.height, layer.maxLevel)
    for (tile in tiles) {
        val bitmap = loader.get(layer, tile.id)
        if (bitmap != null) {
            drawTile(bitmap, tile)
        } else {
            drawCoarserTile(layer, tile, loader)
        }
    }
}

/** The tile is drawn on whole pixels: the neighbours touch each other and there is no seam between them. */
private fun DrawScope.drawTile(bitmap: androidx.compose.ui.graphics.ImageBitmap, tile: PlacedTile) {
    val left = tile.left.roundToInt()
    val top = tile.top.roundToInt()
    drawImage(
        image = bitmap,
        dstOffset = IntOffset(left, top),
        dstSize = IntSize((tile.left + tile.size).roundToInt() - left, (tile.top + tile.size).roundToInt() - top),
    )
}

/** While the tile loads, the part of a tile of a lower level that covers it is drawn, blurry but in its place. */
private fun DrawScope.drawCoarserTile(layer: TileLayer, tile: PlacedTile, loader: TileLoader) {
    val id = tile.id
    for (levels in 1..MAX_PARENT_LEVELS) {
        if (id.z - levels < 0) return
        val parent = TileId(id.z - levels, id.x shr levels, id.y shr levels)
        val bitmap = loader.peek(layer, parent) ?: continue
        val part = bitmap.width shr levels
        val mask = (1 shl levels) - 1
        val left = tile.left.roundToInt()
        val top = tile.top.roundToInt()
        drawImage(
            image = bitmap,
            srcOffset = IntOffset((id.x and mask) * part, (id.y and mask) * part),
            srcSize = IntSize(part, part),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(ceil(tile.size.toDouble()).toInt(), ceil(tile.size.toDouble()).toInt()),
        )
        return
    }
}

private fun DrawScope.drawMarkers(
    markers: List<MapMarker>,
    selected: MapMarker?,
    camera: MapCamera,
    textMeasurer: TextMeasurer,
) {
    val margin = SELECTED_RADIUS_DP * density
    for (marker in markers) {
        if (marker.uid == selected?.uid) continue
        val (x, y) = WebMercator.toScreen(marker.location, camera.center, camera.zoom, size.width, size.height)
        if (x < -margin || y < -margin || x > size.width + margin || y > size.height + margin) continue
        drawMarker(Offset(x, y), marker.type.color(), MARKER_RADIUS_DP * density)
    }
    if (selected != null) {
        val (x, y) = WebMercator.toScreen(selected.location, camera.center, camera.zoom, size.width, size.height)
        val center = Offset(x, y)
        drawMarker(center, selected.type.color(), SELECTED_RADIUS_DP * density)
        drawLabel(selected.name, center, textMeasurer)
    }
}

private fun DrawScope.drawMarker(center: Offset, color: Color, radius: Float) {
    drawCircle(Color.White, radius = radius + 2 * density, center = center)
    drawCircle(color, radius = radius, center = center)
    drawCircle(Color.Black.copy(alpha = 0.25f), radius = radius, center = center, style = Stroke(width = density))
}

private fun DrawScope.drawLabel(text: String, center: Offset, textMeasurer: TextMeasurer) {
    val layout = textMeasurer.measure(text, androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 13.sp))
    val padding = 6 * density
    val width = layout.size.width + 2 * padding
    val height = layout.size.height + 2 * padding
    val left = (center.x - width / 2).coerceIn(4 * density, (size.width - width - 4 * density).coerceAtLeast(4 * density))
    val top = center.y - (SELECTED_RADIUS_DP + 6) * density - height
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8 * density),
    )
    drawText(layout, topLeft = Offset(left + padding, top + padding))
}

private fun MarkerType.color(): Color = when (this) {
    MarkerType.FOOD -> Color(0xFFF57C00)
    MarkerType.BUILDING -> Color(0xFF1E5BB8)
    MarkerType.HOSTEL -> Color(0xFF7B1FA2)
    MarkerType.STRUCTURE -> Color(0xFF2E7D32)
    MarkerType.OTHER, MarkerType.UNDEFINED -> Color(0xFF616161)
}

