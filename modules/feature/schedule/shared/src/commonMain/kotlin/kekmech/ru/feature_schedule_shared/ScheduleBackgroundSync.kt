@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.moscowDate
import kekmech.ru.ext_datetime.startOfWeek
import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The last version of each week that the app saw when it looked for the changes. It is not the cache that the screens use
 * (that one is rewritten every time a screen loads a week), so a change is not lost because the student opened the week
 * before the check.
 */
public class ScheduleSnapshots(
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    public fun get(selected: SelectedSchedule, monday: LocalDate): Schedule? {
        val text = store.getString(keyOf(selected, monday)) ?: return null
        return runCatching { json.decodeFromString(Schedule.serializer(), text) }.getOrNull()
    }

    public fun put(selected: SelectedSchedule, schedule: Schedule) {
        val monday = schedule.weeks.firstOrNull()?.firstDayOfWeek ?: return
        val key = keyOf(selected, monday)
        store.putString(key, json.encodeToString(Schedule.serializer(), schedule))
        val index = (readIndex() - key) + key
        val dropped = (index.size - MAX_SNAPSHOTS).coerceAtLeast(0)
        index.take(dropped).forEach(store::remove)
        store.putString(KEY_INDEX, json.encodeToString(ListSerializer(String.serializer()), index.drop(dropped)))
    }

    private fun readIndex(): List<String> {
        val text = store.getString(KEY_INDEX) ?: return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(String.serializer()), text) }
            .getOrDefault(emptyList())
    }

    private fun keyOf(selected: SelectedSchedule, monday: LocalDate) =
        "$KEY_PREFIX${selected.type.pathName}:${selected.name.trim().lowercase()}:$monday"

    private companion object {
        const val MAX_SNAPSHOTS = 9
        const val KEY_PREFIX = "mpeix_snapshot:"
        const val KEY_INDEX = "mpeix_snapshot_index"
    }
}

/** The switch of the notifications about the changes in the schedule. */
public class ScheduleChangeSettings(private val store: KeyValueStore) {

    /** Off until the student switches it on, as in the Android app. */
    public var enabled: Boolean
        get() = store.getBoolean(KEY_ENABLED) ?: false
        set(value) = store.putBoolean(KEY_ENABLED, value)

    private companion object {
        const val KEY_ENABLED = "mpeix_schedule_change_alerts"
    }
}

/** What the platform does for the changes: it shows a notification about them. */
public interface ScheduleChangeNotifier {

    /** @param today the date to which "today" and "tomorrow" in the text are counted */
    public fun notify(changes: List<ScheduleChange>, today: LocalDate)
}

/** The result of a look at the network: [fresh] are the weeks that were loaded, [changes] are what is new in them. */
public class ChangeCheck(public val fresh: List<Schedule>, public val changes: List<ScheduleChange>)

/**
 * Looks at the network for the three weeks of the chosen schedule and compares them with the snapshots. A week that fails
 * to load is skipped. The loaded weeks are also given to the cache that the screens use, so the app is not behind.
 */
public class ScheduleChangeChecker(
    private val service: ScheduleService,
    private val snapshots: ScheduleSnapshots,
    private val cache: ScheduleCache,
    private val clock: Clock = Clock.System,
) {

    public suspend fun check(selected: SelectedSchedule): ChangeCheck {
        val today = moscowDate(clock)
        val fresh = mutableListOf<Schedule>()
        val changes = mutableListOf<ScheduleChange>()
        for (offset in 0 until WEEKS_TO_CHECK) {
            val schedule = try {
                service.getWeek(selected.type, selected.name, offset)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                continue
            }
            fresh += schedule
            val monday = today.startOfWeek().plus(offset, DateTimeUnit.WEEK)
            changes += ScheduleChangeDetector
                .detectChanges(oldSchedule = snapshots.get(selected, monday), newSchedule = schedule)
                .filter { it.date >= today }
            snapshots.put(selected, schedule)
            cache.saveWeek(selected, schedule)
        }
        return ChangeCheck(fresh, changes)
    }

    private companion object {
        /** This week and the two next ones, as on Android. */
        const val WEEKS_TO_CHECK = 3
    }
}

/**
 * What the app does in the background and when it opens: it looks for the changes in the chosen schedule (and says about
 * them if the student wants), and plans the reminders by the fresh schedule.
 */
public class ScheduleBackgroundSync(
    private val cache: ScheduleCache,
    private val checker: ScheduleChangeChecker,
    private val changeSettings: ScheduleChangeSettings,
    private val notifier: ScheduleChangeNotifier,
    private val reminderSettings: ClassReminderSettings,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock = Clock.System,
) {

    /**
     * @return `false` if the network did not answer and it is worth trying again later, `true` otherwise (also when there
     * is nothing to do: no schedule is chosen, both the switches are off)
     */
    public suspend fun run(): Boolean {
        val selected = cache.selected() ?: return true
        val alertsOn = changeSettings.enabled
        val kinds = reminderSettings.activeKinds()
        if (!alertsOn && kinds.isEmpty()) return true

        val check = checker.check(selected)
        if (check.fresh.isEmpty()) return false

        if (alertsOn && check.changes.isNotEmpty() && reminderScheduler.isPermissionGranted()) {
            notifier.notify(check.changes, moscowDate(clock))
        }
        if (kinds.isNotEmpty() && reminderScheduler.isPermissionGranted()) {
            reminderScheduler.replaceAll(ClassReminderPlanner.plan(check.fresh, clock.now(), kinds))
        }
        return true
    }
}
