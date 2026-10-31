@file:OptIn(ExperimentalTime::class)

package kekmech.ru.shared.reminders

import kekmech.ru.feature_schedule_shared.ClassReminder
import kekmech.ru.feature_schedule_shared.ReminderScheduler
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

/** The most local notifications that an app can have planned at the same time on iOS. */
private const val MAX_PENDING_NOTIFICATIONS = 64

/**
 * The reminders as local notifications of iOS (`UNUserNotificationCenter`). The app plans them when it is opened, for the
 * three weeks ahead; iOS keeps them and shows them even when the app is closed. The app does not use the notifications for
 * anything else, so [replaceAll] clears all of them.
 */
internal class IosReminderScheduler : ReminderScheduler {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            continuation.resume(granted)
        }
    }

    override suspend fun isPermissionGranted(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional)
        }
    }

    override fun replaceAll(reminders: List<ClassReminder>) {
        center.removeAllPendingNotificationRequests()
        val now = Clock.System.now()
        // iOS keeps only 64 of them: the nearest ones; the next start of the app plans the rest
        reminders.sortedBy { it.triggerAt }.take(MAX_PENDING_NOTIFICATIONS).forEach { reminder ->
            val seconds = (reminder.triggerAt - now).toDouble(DurationUnit.SECONDS)
            if (seconds <= 0.0) return@forEach
            val content = UNMutableNotificationContent().apply {
                setTitle(reminder.title)
                setBody(reminder.body)
                setSound(UNNotificationSound.defaultSound)
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier("class-${reminder.id}", content, trigger)
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    override fun cancelAll() {
        center.removeAllPendingNotificationRequests()
    }
}
