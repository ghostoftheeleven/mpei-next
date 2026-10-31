package kekmech.ru.feature_map_impl.launcher

import kekmech.ru.feature_map_shared.MarkerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A request of a link to the map: show a place, or show a tab. */
internal sealed interface MapLink {
    data class Place(val uid: String) : MapLink
    data class Tab(val tab: MarkerType) : MapLink
}

/**
 * Keeps the request of a link until the map screen takes it: the link can come before the screen exists, while it is
 * hidden or while it is on the screen, so the screen watches [link] and calls [consume] when it has shown it.
 */
internal class DeeplinkDelegate {

    private val mutableLink = MutableStateFlow<MapLink?>(null)
    val link: StateFlow<MapLink?> = mutableLink.asStateFlow()

    fun putPlaceUid(placeUid: String) {
        mutableLink.value = MapLink.Place(placeUid)
    }

    /** @param tabName the name of a tab in the links: FOOD, BUILDINGS, HOSTELS, OTHERS or STRUCTURES; others are ignored */
    fun putTab(tabName: String) {
        val tab = when (tabName.uppercase()) {
            "FOOD" -> MarkerType.FOOD
            "BUILDINGS", "BUILDING" -> MarkerType.BUILDING
            "HOSTELS", "HOSTEL" -> MarkerType.HOSTEL
            "OTHERS", "OTHER" -> MarkerType.OTHER
            "STRUCTURES", "STRUCTURE" -> MarkerType.STRUCTURE
            else -> return
        }
        mutableLink.value = MapLink.Tab(tab)
    }

    fun consume() {
        mutableLink.value = null
    }
}
