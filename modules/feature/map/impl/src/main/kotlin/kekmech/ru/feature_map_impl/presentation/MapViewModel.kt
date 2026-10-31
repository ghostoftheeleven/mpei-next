package kekmech.ru.feature_map_impl.presentation

import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import kekmech.ru.feature_map_shared.AppearanceStorage
import kekmech.ru.feature_map_shared.MapController
import kekmech.ru.feature_map_shared.MapMarkersRepository
import kekmech.ru.ui_map.TileLoader
import kekmech.ru.ui_map.tileFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The state of the map screen. It lives in the view model of the activity, so the camera, the tab and the loaded tiles stay
 * while the screen is hidden by the other tabs of the app and while the screen is turned.
 */
internal class MapViewModel(
    repository: MapMarkersRepository,
    appearance: AppearanceStorage,
    private val tileClient: HttpClient,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val controller = MapController(repository, appearance, scope)
    val tileLoader = TileLoader(scope, tileClient.tileFetcher())

    override fun onCleared() {
        scope.cancel()
        tileClient.close()
    }
}
