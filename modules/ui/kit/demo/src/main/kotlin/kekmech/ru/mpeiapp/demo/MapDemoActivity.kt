package kekmech.ru.mpeiapp.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_map_shared.MapMarker
import kekmech.ru.feature_map_shared.MapMarkersRepository
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kekmech.ru.ui_map.MapAppearance
import kekmech.ru.ui_map.MapState
import kekmech.ru.ui_map.MpeiMap
import kekmech.ru.ui_map.TileLoader
import kekmech.ru.ui_map.tileFetcher

/** A bare screen with the map of the campus, to look at it on a device without the rest of the app. */
class MapDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MapDemo() }
    }
}

@Composable
private fun MapDemo() {
    val scope = rememberCoroutineScope()
    val client = remember { createHttpClient(ClientIdentity("MPEI-Hub-demo/1.0", "demo", "Android", "ru", "1.0")) }
    val loader = remember { TileLoader(scope, client.tileFetcher()) }
    val state = remember { MapState() }
    var markers by remember { mutableStateOf(emptyList<MapMarker>()) }
    var selected by remember { mutableStateOf<MapMarker?>(null) }
    var appearance by remember { mutableStateOf(MapAppearance.SCHEME) }
    var status by remember { mutableStateOf("loading markers") }

    LaunchedEffect(Unit) {
        status = runCatching { MapMarkersRepository(client, InMemoryKeyValueStore()).load() }
            .fold({ markers = it.markers; "${it.markers.size} markers" }, { "markers failed: ${it.message}" })
    }

    Box(Modifier.fillMaxSize()) {
        MpeiMap(
            state = state,
            markers = markers,
            selected = selected,
            appearance = appearance,
            tileLoader = loader,
            onMarkerClick = { selected = it },
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {
                appearance = if (appearance == MapAppearance.SCHEME) MapAppearance.HYBRID else MapAppearance.SCHEME
            }) { Text(appearance.code) }
            Text(status, color = Color.Black)
        }
    }
}
