package kekmech.ru.feature_map_shared

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import kekmech.ru.lib_network.BackendServiceUrl
import kekmech.ru.lib_network.ServiceUrlResolver
import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The markers and where they came from: [fromCache] is `true` when the network failed and the copy was used. */
public class LoadedMarkers(public val markers: List<MapMarker>, public val fromCache: Boolean)

/**
 * The markers of the campus map. They are loaded from `markers.json` of the repository (the same file as the Android
 * app reads); the last answer is kept on the device and is used when the network fails.
 *
 * @param baseUrl is asked for every request, because the environment of the app can change at run time.
 */
public class MapMarkersRepository(
    private val client: HttpClient,
    private val store: KeyValueStore,
    private val baseUrl: () -> String = { ServiceUrlResolver.resolve(BackendServiceUrl.MAP) },
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true },
) {

    /** @throws Exception the error of the network, when there is no copy on the device either */
    public suspend fun load(): LoadedMarkers =
        try {
            // GitHub serves the file as text/plain, so it is not decoded by the content negotiation of the client
            val text = client.get("${baseUrl()}markers.json").bodyAsText()
            val markers = json.decodeFromString(serializer, text)
            store.putString(KEY_MARKERS, json.encodeToString(serializer, markers))
            LoadedMarkers(markers, fromCache = false)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val cached = cached() ?: throw e
            LoadedMarkers(cached, fromCache = true)
        }

    /** The copy on the device, or `null` if there is none. */
    public fun cached(): List<MapMarker>? {
        val text = store.getString(KEY_MARKERS) ?: return null
        return runCatching { json.decodeFromString(serializer, text) }.getOrNull()
    }

    private companion object {
        const val KEY_MARKERS = "mpeix_map_markers"
        val serializer = ListSerializer(MapMarker.serializer())
    }
}
