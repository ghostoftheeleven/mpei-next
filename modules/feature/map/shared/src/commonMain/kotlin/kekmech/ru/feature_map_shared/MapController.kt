package kekmech.ru.feature_map_shared

import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The tabs of the map, in the order the screen shows them. */
public val MapTabs: List<MarkerType> =
    listOf(MarkerType.FOOD, MarkerType.BUILDING, MarkerType.HOSTEL, MarkerType.STRUCTURE, MarkerType.OTHER)

public data class MapUiState(
    val tab: MarkerType = MarkerType.FOOD,
    val query: String = "",
    /** All the markers that were loaded. */
    val markers: List<MapMarker> = emptyList(),
    val selected: MapMarker? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** `true` when the markers are the copy on the device because the network failed. */
    val offline: Boolean = false,
    /** `scheme` or `hybrid`; the screen knows what they draw. */
    val appearance: String = APPEARANCE_SCHEME,
) {

    /**
     * The markers that the map and the list show: the ones of the tab, or the ones that suit the search, whatever their
     * tab is, when something is typed in it.
     */
    val visible: List<MapMarker>
        get() = if (query.isBlank()) filterMarkers(markers, type = tab) else filterMarkers(markers, query = query)
}

public const val APPEARANCE_SCHEME: String = "scheme"
public const val APPEARANCE_HYBRID: String = "hybrid"

/**
 * What the map screen does: it keeps the [state] that the screen draws and changes it when the screen reports a tap. The
 * markers come from the network and from the copy on the device when the network fails; the chosen look of the map
 * (scheme or hybrid) is kept on the device.
 *
 * @param scope where the requests run; the controller does not outlive it
 */
public class MapController(
    private val repository: MapMarkersRepository,
    private val store: KeyValueStore,
    private val scope: CoroutineScope,
) {

    private val mutableState = MutableStateFlow(
        MapUiState(appearance = store.getString(KEY_APPEARANCE) ?: APPEARANCE_SCHEME),
    )
    public val state: StateFlow<MapUiState> = mutableState.asStateFlow()

    private var started = false

    /** Loads the markers. Call it when the screen appears; it does nothing the second time unless it failed. */
    public fun start() {
        if (started && mutableState.value.error == null) return
        started = true
        reload()
    }

    /** The "try again" of the error. */
    public fun reload() {
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        scope.launch {
            val next = try {
                val loaded = repository.load()
                mutableState.value.copy(markers = loaded.markers, offline = loaded.fromCache, loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                mutableState.value.copy(loading = false, error = "Не удалось загрузить места. Проверьте подключение к интернету")
            }
            mutableState.value = next
        }
    }

    public fun selectTab(tab: MarkerType) {
        mutableState.value = mutableState.value.copy(tab = tab, query = "", selected = null)
    }

    public fun setQuery(query: String) {
        mutableState.value = mutableState.value.copy(query = query, selected = null)
    }

    /** `null` takes the selection off. */
    public fun select(marker: MapMarker?) {
        mutableState.value = mutableState.value.copy(selected = marker)
    }

    public fun setAppearance(appearance: String) {
        store.putString(KEY_APPEARANCE, appearance)
        mutableState.value = mutableState.value.copy(appearance = appearance)
    }

    private companion object {
        const val KEY_APPEARANCE = "mpeix_map_appearance"
    }
}
