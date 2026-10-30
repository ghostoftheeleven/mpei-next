package kekmech.ru.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kekmech.ru.feature_bars_shared.BarsController
import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.shared.bars.BarsScreen

/**
 * The root of the shared UI. For now the only section that was moved from the Android app is BARS (milestone 6 of the
 * iOS port, see issue #5); the other sections move here module by module.
 *
 * @param bars the client of the BARS portal; the platform decides where its session is kept
 */
@Composable
public fun App(bars: BarsApi) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            val scope = rememberCoroutineScope()
            val controller = remember(bars) { BarsController(bars, scope) }
            LaunchedEffect(controller) { controller.start() }
            BarsScreen(controller)
        }
    }
}
