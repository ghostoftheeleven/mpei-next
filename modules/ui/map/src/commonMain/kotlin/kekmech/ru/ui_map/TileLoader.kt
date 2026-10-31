package kekmech.ru.ui_map

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kekmech.ru.feature_map_shared.TileId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private data class TileKey(val layer: String, val id: TileId)

private const val DEFAULT_CACHED_TILES = 400
private const val DEFAULT_PARALLEL_REQUESTS = 6
private val RETRY_AFTER = 15.seconds

/** The function that [TileLoader] loads a tile with: the bytes of the image at the address. */
public typealias TileFetcher = suspend (url: String) -> ByteArray

/** The tiles through the client of the app. A bad status is an error, like in the other clients of the app. */
public fun HttpClient.tileFetcher(): TileFetcher = { url -> get(url).body<ByteArray>() }

/**
 * Loads the tiles and keeps the last ones in the memory. [get] is called while the map is drawn: it returns the tile if
 * it is here, and starts loading it if it is not; when a tile comes, the map is drawn again. A tile that failed is
 * tried again after a while, when the map is drawn the next time.
 *
 * It is used from the main thread only (the drawing and the [scope] are on it).
 *
 * @param scope where the loading runs; the tiles that are not loaded yet are abandoned with it
 */
@Stable
public class TileLoader(
    private val scope: CoroutineScope,
    private val fetch: TileFetcher,
    private val maxCachedTiles: Int = DEFAULT_CACHED_TILES,
    maxParallelRequests: Int = DEFAULT_PARALLEL_REQUESTS,
) {

    private val cache = LinkedHashMap<TileKey, ImageBitmap>()
    private val loading = HashSet<TileKey>()
    private val failed = HashMap<TileKey, ComparableTimeMark>()
    private val gate = Semaphore(maxParallelRequests)

    /** Changes when a tile comes: the drawing that reads it is drawn again. */
    private var revision by mutableIntStateOf(0)

    /** The tile, or `null` while it loads (the loading is started). */
    public fun get(layer: TileLayer, id: TileId): ImageBitmap? {
        revision // the reading is what makes the drawing depend on the arrival of the tiles
        val key = TileKey(layer.id, id)
        val tile = cache.remove(key)
        if (tile != null) {
            cache[key] = tile // the most recently used goes to the end
            return tile
        }
        load(layer, id, key)
        return null
    }

    /** The tile if it is here; nothing is loaded. For drawing a coarser tile while the right one comes. */
    public fun peek(layer: TileLayer, id: TileId): ImageBitmap? {
        revision
        return cache[TileKey(layer.id, id)]
    }

    private fun load(layer: TileLayer, id: TileId, key: TileKey) {
        if (key in loading) return
        val failedMark = failed[key]
        if (failedMark != null && failedMark.elapsedNow() < RETRY_AFTER) return
        loading += key
        scope.launch {
            try {
                val bitmap = gate.withPermit {
                    val bytes = fetch(layer.url(id))
                    withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() }
                }
                put(key, bitmap)
                failed.remove(key)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                failed[key] = TimeSource.Monotonic.markNow()
            } finally {
                loading -= key
            }
        }
    }

    private fun put(key: TileKey, bitmap: ImageBitmap) {
        cache[key] = bitmap
        while (cache.size > maxCachedTiles) {
            cache.remove(cache.keys.first())
        }
        revision++
    }
}
