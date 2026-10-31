@file:OptIn(ExperimentalTime::class)

package kekmech.ru.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kekmech.ru.feature_schedule_shared.FavoriteSchedules
import kekmech.ru.feature_schedule_shared.ScheduleCache
import kekmech.ru.feature_schedule_shared.ScheduleService
import kekmech.ru.lib_bars_client.BarsClient
import kekmech.ru.lib_bars_client.KeychainBarsSessionStore
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_mpei_api.MpeiScheduleApi
import kekmech.ru.lib_settings.UserDefaultsKeyValueStore
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import platform.Foundation.preferredLanguages
import platform.UIKit.UIViewController
import kotlin.time.ExperimentalTime

private const val APP_VERSION = "0.5.2"
private const val KEY_DEVICE_ID = "mpeix_device_id"

/** Called from Swift (iosApp/iosApp/ContentView.swift): `MainViewControllerKt.MainViewController()` */
@Suppress("FunctionName")
public fun MainViewController(): UIViewController = ComposeUIViewController {
    // The session of BARS lives in the Keychain, so the student stays signed in after the app is closed
    val bars = remember { BarsClient(sessionStore = KeychainBarsSessionStore()) }
    val store = remember { UserDefaultsKeyValueStore() }
    val scheduleService = remember { ScheduleService(MpeiScheduleApi(createHttpClient(clientIdentity()))) }
    val scheduleCache = remember { ScheduleCache(store) }
    val favorites = remember { FavoriteSchedules(store) }
    App(bars, scheduleService, scheduleCache, favorites)
}

private fun clientIdentity(): ClientIdentity {
    val defaults = NSUserDefaults.standardUserDefaults
    val deviceId = defaults.stringForKey(KEY_DEVICE_ID)
        ?: NSUUID().UUIDString().also { defaults.setObject(it, forKey = KEY_DEVICE_ID) }
    val locale = (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "ru"
    return ClientIdentity(
        userAgent = "MPEI-Hub/$APP_VERSION (iOS)",
        deviceId = deviceId,
        deviceOs = "iOS",
        deviceLocale = locale,
        appVersion = APP_VERSION,
    )
}
