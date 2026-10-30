package kekmech.ru.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kekmech.ru.lib_bars_client.BarsClient
import kekmech.ru.lib_bars_client.KeychainBarsSessionStore
import platform.UIKit.UIViewController

/** Called from Swift (iosApp/iosApp/ContentView.swift): `MainViewControllerKt.MainViewController()` */
@Suppress("FunctionName")
public fun MainViewController(): UIViewController = ComposeUIViewController {
    // The session of BARS lives in the Keychain, so the student stays signed in after the app is closed
    val bars = remember { BarsClient(sessionStore = KeychainBarsSessionStore()) }
    App(bars)
}
