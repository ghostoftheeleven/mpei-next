@file:OptIn(ExperimentalTime::class)

package kekmech.ru.shared

import kekmech.ru.feature_map_shared.MapMarkersRepository
import kekmech.ru.feature_notes_shared.NotesStore
import kekmech.ru.feature_schedule_shared.FavoriteSchedules
import kekmech.ru.feature_schedule_shared.ScheduleCache
import kekmech.ru.feature_schedule_shared.ScheduleService
import kekmech.ru.lib_bars_client.BarsClient
import kekmech.ru.lib_bars_client.KeychainBarsSessionStore
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_mpei_api.MpeiScheduleApi
import kekmech.ru.lib_settings.UserDefaultsKeyValueStore
import kekmech.ru.shared.map.MapDependencies
import kekmech.ru.shared.reminders.IosChangeNotifier
import kekmech.ru.shared.reminders.IosReminderScheduler
import kekmech.ru.ui_map.tileFetcher
import kekmech.ru.ui_map.tileHttpClient
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages
import kotlin.time.ExperimentalTime

private const val APP_VERSION = "0.5.2"
private const val KEY_DEVICE_ID = "mpeix_device_id"

/**
 * What the iOS app is made of. It is made once: the screens and the work in the background (when iOS wakes the app up
 * without a screen) take the same objects from here.
 */
internal object IosAppGraph {

    val dependencies: AppDependencies by lazy {
        // the session of BARS lives in the Keychain, so the student stays signed in after the app is closed
        val bars = BarsClient(sessionStore = KeychainBarsSessionStore())
        val store = UserDefaultsKeyValueStore()
        val client = createHttpClient(clientIdentity())
        val scheduleCache = ScheduleCache(store)
        AppDependencies(
            bars = bars,
            scheduleService = ScheduleService(MpeiScheduleApi(client)),
            scheduleCache = scheduleCache,
            favorites = FavoriteSchedules(store),
            notesStore = NotesStore(store),
            map = MapDependencies(
                markers = MapMarkersRepository(client, store),
                // the tiles are loaded by a client that does not tell the id of the device to the tile servers
                tiles = tileHttpClient("MPEI-Hub/$APP_VERSION (iOS)").tileFetcher(),
                settings = store,
            ),
            reminderScheduler = IosReminderScheduler(),
            changeNotifier = IosChangeNotifier(),
            settings = store,
        )
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
}
