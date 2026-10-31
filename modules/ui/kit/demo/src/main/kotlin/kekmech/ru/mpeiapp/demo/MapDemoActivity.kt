package kekmech.ru.mpeiapp.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kekmech.ru.feature_map_shared.MapController
import kekmech.ru.feature_map_shared.MapMarkersRepository
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kekmech.ru.ui_map.MapScreen
import kekmech.ru.ui_map.TileLoader
import kekmech.ru.ui_map.tileFetcher
import kekmech.ru.ui_map.tileHttpClient

/** The screen of the campus map as it is in the iOS app, to look at it on a device without the rest of the app. */
class MapDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { MapDemo() }
            }
        }
    }
}

@Composable
private fun MapDemo() {
    val scope = rememberCoroutineScope()
    val client = remember { createHttpClient(ClientIdentity("MPEI-Hub-demo/1.0", "demo", "Android", "ru", "1.0")) }
    val store = remember { InMemoryKeyValueStore() }
    val controller = remember { MapController(MapMarkersRepository(client, store), store, scope) }
    val loader = remember { TileLoader(scope, tileHttpClient("MPEI-Hub-demo/1.0").tileFetcher()) }
    MapScreen(controller, loader)
}
