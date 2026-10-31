package kekmech.ru.feature_map_impl.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kekmech.lib_fragment.ComposeFragment
import kekmech.ru.feature_app_settings_api.data.AppSettingsRepository
import kekmech.ru.feature_app_settings_api.domain.model.isDarkTheme
import kekmech.ru.feature_map_impl.launcher.DeeplinkDelegate
import kekmech.ru.feature_map_impl.launcher.MapLink
import kekmech.ru.feature_map_shared.MarkerType
import kekmech.ru.lib_analytics_android.ext.screenAnalytics
import kekmech.ru.lib_navigation.features.ScrollToTop
import kekmech.ru.lib_navigation.features.TabScreenStateSaver
import kekmech.ru.lib_navigation.features.TabScreenStateSaverImpl
import kekmech.ru.ui_map.MapScreen
import kekmech.ru.ui_map.MapScreenEvent
import kekmech.ru.ui_theme.theme.MpeixScreenTheme
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

/**
 * The map of the campus. The screen itself is the common one (`MapScreen` of :ui_map), the same as in the iOS app; this
 * fragment only gives it the state, the theme and the links, and counts the taps.
 */
internal class MapFragment :
    ComposeFragment(),
    ScrollToTop,
    TabScreenStateSaver by TabScreenStateSaverImpl("map") {

    private val analytics by screenAnalytics("Map")
    private val deeplinkDelegate by inject<DeeplinkDelegate>()
    private val appSettings by inject<AppSettingsRepository>()

    private val viewModel: MapViewModel by lazy {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = get<MapViewModel>() as T
        }
        ViewModelProvider(requireActivity(), factory)[MapViewModel::class.java]
    }

    @Composable
    override fun Screen() {
        val isSystemDark = isSystemInDarkTheme()
        val darkTheme = remember(isSystemDark) {
            derivedStateOf { appSettings.getAppSettings().isDarkTheme(isSystemDark) }
        }
        MpeixScreenTheme(darkTheme = darkTheme.value) {
            val controller = viewModel.controller
            val link by deeplinkDelegate.link.collectAsState()
            LaunchedEffect(link) {
                when (val current = link) {
                    is MapLink.Place -> controller.openPlace(current.uid)
                    is MapLink.Tab -> controller.openTab(current.tab)
                    null -> return@LaunchedEffect
                }
                deeplinkDelegate.consume()
            }
            MapScreen(
                controller = controller,
                tileLoader = viewModel.tileLoader,
                // the status bar is the only inset here: the bottom bar of the app takes the rest
                insets = WindowInsets.statusBars,
                onEvent = ::onEvent,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // the look of the map can be changed in the settings while the map is in the background
        viewModel.controller.syncAppearance()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) viewModel.controller.syncAppearance()
    }

    override fun onScrollToTop() {
        viewModel.controller.resetView()
    }

    private fun onEvent(event: MapScreenEvent) {
        when (event) {
            is MapScreenEvent.TabSelected -> analytics.sendClick("Tab_${event.tab.analyticsName()}")
            is MapScreenEvent.PlaceSelected -> analytics.sendClick("ListMarker_(${event.place.name})")
            is MapScreenEvent.AppearanceChanged -> analytics.sendClick("MapAppearance_${event.appearance}")
            MapScreenEvent.Reload -> analytics.sendClick("MapReload")
        }
    }

    /** The names of the tabs in the analytics are the same as before the map became the common screen. */
    private fun MarkerType.analyticsName() = when (this) {
        MarkerType.FOOD -> "FOOD"
        MarkerType.BUILDING -> "BUILDINGS"
        MarkerType.HOSTEL -> "HOSTELS"
        MarkerType.STRUCTURE -> "STRUCTURES"
        MarkerType.OTHER, MarkerType.UNDEFINED -> "OTHERS"
    }
}
