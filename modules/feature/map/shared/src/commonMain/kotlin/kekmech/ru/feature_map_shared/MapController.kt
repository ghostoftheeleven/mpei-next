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

public const val APPEARANCE_SCHEME: String = "scheme"
public const val APPEARANCE_HYBRID: String = "hybrid"

/** Where the look of the map (`scheme` or `hybrid`) is kept: the platform decides, the settings of Android have it too. */
public interface AppearanceStorage {

    /** `null` if it was never saved. */
    public fun get(): String?

    public fun set(appearance: String)
}

/** The look of the map in a [KeyValueStore]. */
public class KeyValueAppearanceStorage(private val store: KeyValueStore) : AppearanceStorage {

    override fun get(): String? = store.getString(KEY_APPEARANCE)

    override fun set(appearance: String) {
        store.putString(KEY_APPEARANCE, appearance)
    }

    private companion object {
        const val KEY_APPEARANCE = "mpeix_map_appearance"
    }
}

/** A request to move the camera to [location]; [token] is different for every request, so the same place can be asked again. */
public data class MapFocus(val location: Location, val token: Int)

public data class MapUiState(
    val tab: MarkerType = MarkerType.FOOD,
    val query: String = "",
    /** All the markers that were loaded. */
    val markers: List<MapMarker> = emptyList(),
    val selected: MapMarker? = null,
    val focus: MapFocus? = null,
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

/**
 * What the map screen does: it keeps the [state] that the screen draws and changes it when the screen reports a tap. The
 * markers come from the network and from the copy on the device when the network fails.
 *
 * @param scope where the requests run; the controller does not outlive it
 */
public class MapController(
    private val repository: MapMarkersRepository,
    private val appearanceStorage: AppearanceStorage,
    private val scope: CoroutineScope,
) {

    public constructor(repository: MapMarkersRepository, store: KeyValueStore, scope: CoroutineScope) :
        this(repository, KeyValueAppearanceStorage(store), scope)

    private val mutableState = MutableStateFlow(
        MapUiState(appearance = appearanceStorage.get() ?: APPEARANCE_SCHEME),
    )
    public val state: StateFlow<MapUiState> = mutableState.asStateFlow()

    private var started = false
    private var focusToken = 0
    private var pendingPlaceUid: String? = null

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
                mutableState.value.copy(
                    loading = false,
                    error = "Не удалось загрузить места. Проверьте подключение к интернету",
                )
            }
            mutableState.value = next
            pendingPlaceUid?.let(::openPlace)
        }
    }

    public fun selectTab(tab: MarkerType) {
        mutableState.value = mutableState.value.copy(tab = tab, query = "", selected = null)
    }

    public fun setQuery(query: String) {
        mutableState.value = mutableState.value.copy(query = query, selected = null)
    }

    /** `null` takes the selection off. With [focus] the camera goes to the place (a tap in the list, not on the map). */
    public fun select(marker: MapMarker?, focus: Boolean = false) {
        val current = mutableState.value
        mutableState.value = current.copy(
            selected = marker,
            focus = if (focus && marker != null) nextFocus(marker.location) else current.focus,
        )
    }

    /**
     * Shows the place with this uid: its tab, the selection and the camera. If the markers are not loaded yet, it is
     * done as soon as they are.
     */
    public fun openPlace(uid: String) {
        val marker = mutableState.value.markers.firstOrNull { it.uid == uid }
        if (marker == null) {
            pendingPlaceUid = uid
            return
        }
        pendingPlaceUid = null
        mutableState.value = mutableState.value.copy(
            tab = marker.type.takeIf { it in MapTabs } ?: mutableState.value.tab,
            query = "",
            selected = marker,
            focus = nextFocus(marker.location),
        )
    }

    /** Shows a tab; for the links to the map. */
    public fun openTab(tab: MarkerType) {
        if (tab in MapTabs) selectTab(tab)
    }

    /** Takes the selection off and brings the camera back to the campus: what the tap on the tab of the map does. */
    public fun resetView() {
        mutableState.value = mutableState.value.copy(selected = null, focus = nextFocus(CampusCenter))
    }

    public fun setAppearance(appearance: String) {
        appearanceStorage.set(appearance)
        mutableState.value = mutableState.value.copy(appearance = appearance)
    }

    /** Reads the look of the map again: it can be changed in the settings while the map is in the background. */
    public fun syncAppearance() {
        val saved = appearanceStorage.get() ?: return
        if (saved != mutableState.value.appearance) {
            mutableState.value = mutableState.value.copy(appearance = saved)
        }
    }

    private fun nextFocus(location: Location) = MapFocus(location, ++focusToken)
}
