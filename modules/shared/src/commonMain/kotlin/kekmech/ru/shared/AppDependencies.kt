@file:OptIn(ExperimentalTime::class)

package kekmech.ru.shared

import kekmech.ru.feature_notes_shared.NotesStore
import kekmech.ru.feature_schedule_shared.ClassReminderSettings
import kekmech.ru.feature_schedule_shared.FavoriteSchedules
import kekmech.ru.feature_schedule_shared.ReminderScheduler
import kekmech.ru.feature_schedule_shared.ScheduleBackgroundSync
import kekmech.ru.feature_schedule_shared.ScheduleCache
import kekmech.ru.feature_schedule_shared.ScheduleChangeChecker
import kekmech.ru.feature_schedule_shared.ScheduleChangeNotifier
import kekmech.ru.feature_schedule_shared.ScheduleChangeSettings
import kekmech.ru.feature_schedule_shared.ScheduleService
import kekmech.ru.feature_schedule_shared.ScheduleSnapshots
import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.lib_settings.KeyValueStore
import kekmech.ru.shared.map.MapDependencies
import kotlin.time.ExperimentalTime

/**
 * Everything the app is made of, that the platform gives: the screens and the work in the background take it from here, so
 * both use the same objects and the same data on the device.
 *
 * @param bars the client of the BARS portal; the platform decides where its session is kept
 * @param scheduleService the timetable of MPEI
 * @param scheduleCache the offline copy of the schedule
 * @param favorites the groups and the teachers that the student has marked
 * @param notesStore the notes of the student for the classes
 * @param map the campus map: the markers, the tiles and the settings
 * @param reminderScheduler shows the reminders about the classes; the platform decides how
 * @param changeNotifier shows the notification about the changes in the schedule
 * @param settings the settings of the student
 */
public class AppDependencies(
    public val bars: BarsApi,
    public val scheduleService: ScheduleService,
    public val scheduleCache: ScheduleCache,
    public val favorites: FavoriteSchedules,
    public val notesStore: NotesStore,
    public val map: MapDependencies,
    public val reminderScheduler: ReminderScheduler,
    public val changeNotifier: ScheduleChangeNotifier,
    public val settings: KeyValueStore,
) {

    public val reminderSettings: ClassReminderSettings = ClassReminderSettings(settings)
    public val changeSettings: ScheduleChangeSettings = ScheduleChangeSettings(settings)

    /** What runs in the background and when the app opens: the changes in the schedule and the reminders. */
    public val backgroundSync: ScheduleBackgroundSync = ScheduleBackgroundSync(
        cache = scheduleCache,
        checker = ScheduleChangeChecker(scheduleService, ScheduleSnapshots(settings), scheduleCache),
        changeSettings = changeSettings,
        notifier = changeNotifier,
        reminderSettings = reminderSettings,
        reminderScheduler = reminderScheduler,
    )
}
