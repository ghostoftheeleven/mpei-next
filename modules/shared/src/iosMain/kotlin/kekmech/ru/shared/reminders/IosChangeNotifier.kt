package kekmech.ru.shared.reminders

import kekmech.ru.feature_schedule_shared.ScheduleChange
import kekmech.ru.feature_schedule_shared.ScheduleChangeNotifier
import kekmech.ru.feature_schedule_shared.ScheduleChangeText
import kotlinx.datetime.LocalDate
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** The identifier of the notification: a new one replaces the one that is still in the list, as on Android. */
private const val CHANGES_NOTIFICATION_ID = "schedule-changes"

/** The notification about the changes in the schedule: it is shown at once. */
internal class IosChangeNotifier : ScheduleChangeNotifier {

    override fun notify(changes: List<ScheduleChange>, today: LocalDate) {
        if (changes.isEmpty()) return
        val content = UNMutableNotificationContent().apply {
            setTitle(ScheduleChangeText.title(changes.size))
            // the preview is one line; the whole text is what the notification shows when it is opened
            setBody(if (changes.size == 1) ScheduleChangeText.preview(changes, today) else ScheduleChangeText.details(changes, today))
            setSound(UNNotificationSound.defaultSound)
        }
        // no trigger: the notification is delivered at once
        val request = UNNotificationRequest.requestWithIdentifier(CHANGES_NOTIFICATION_ID, content, trigger = null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, withCompletionHandler = null)
    }
}
