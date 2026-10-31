@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_shared

import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** What the platform does for the reminders: it shows a notification at the time that was given to it. */
public interface ReminderScheduler {

    /** Asks the student for the permission to show the notifications (once the system asks, then it only reports). */
    public suspend fun requestPermission(): Boolean

    /** `true` if the notifications can be shown now. */
    public suspend fun isPermissionGranted(): Boolean

    /** Makes [reminders] the only reminders that are planned: the ones that were planned before are removed. */
    public fun replaceAll(reminders: List<ClassReminder>)

    public fun cancelAll()
}

/** The settings of the reminders on the device. */
public class ClassReminderSettings(
    private val store: KeyValueStore,
    /** What the switch is before the student touches it. */
    private val enabledByDefault: Boolean = false,
) {

    public var enabled: Boolean
        get() = store.getBoolean(KEY_ENABLED) ?: enabledByDefault
        set(value) = store.putBoolean(KEY_ENABLED, value)

    public fun isKindEnabled(kind: ReminderKind): Boolean = store.getBoolean(keyOf(kind)) ?: true

    public fun setKindEnabled(kind: ReminderKind, enabled: Boolean) {
        store.putBoolean(keyOf(kind), enabled)
    }

    /** The kinds that have to be planned now: none when the whole feature is off. */
    public fun activeKinds(): Set<ReminderKind> =
        if (enabled) ReminderKind.entries.filterTo(mutableSetOf()) { isKindEnabled(it) } else emptySet()

    private fun keyOf(kind: ReminderKind) = when (kind) {
        ReminderKind.DAY_BEFORE -> "mpeix_reminder_24h"
        ReminderKind.HOURS_BEFORE -> "mpeix_reminder_5h"
    }

    private companion object {
        const val KEY_ENABLED = "mpeix_reminders_enabled"
    }
}

public data class RemindersUiState(
    val enabled: Boolean,
    val dayBefore: Boolean,
    val hoursBefore: Boolean,
    /** The student switched the reminders on, but the system does not let the app show notifications. */
    val permissionDenied: Boolean = false,
    /** The notifications about the changes in the schedule are on. */
    val changeAlerts: Boolean = false,
    /** How many reminders are planned now. */
    val planned: Int = 0,
)

/**
 * What the settings of the reminders do: it keeps the [state] and keeps the reminders that the platform shows in step with
 * the schedule that is chosen and with the switches.
 *
 * @param loadWeek a week of the schedule (0 is this week) or `null` if it could not be loaded
 * @param scope where the work runs; the controller does not outlive it
 */
public class RemindersController(
    private val settings: ClassReminderSettings,
    private val changeSettings: ScheduleChangeSettings,
    private val scheduler: ReminderScheduler,
    private val loadWeek: suspend (SelectedSchedule, Int) -> Schedule?,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {

    private val mutableState = MutableStateFlow(readState())
    public val state: StateFlow<RemindersUiState> = mutableState.asStateFlow()

    private var selected: SelectedSchedule? = null
    private var job: Job? = null

    /** Call it when the app starts and when the chosen schedule changes: the reminders are planned again. */
    public fun sync(schedule: SelectedSchedule?) {
        selected = schedule
        plan()
    }

    /** Remembers the chosen schedule without planning: for the screens that plan the reminders in another way. */
    public fun attach(schedule: SelectedSchedule?) {
        selected = schedule
    }

    public fun setEnabled(enabled: Boolean) {
        job?.cancel()
        if (!enabled) {
            settings.enabled = false
            scheduler.cancelAll()
            mutableState.value = readState()
            return
        }
        job = scope.launch {
            if (scheduler.requestPermission()) {
                settings.enabled = true
                mutableState.value = readState()
                runPlan()
            } else {
                settings.enabled = false
                mutableState.value = readState().copy(permissionDenied = true)
            }
        }
    }

    /** Switches the notifications about the changes in the schedule; they need the permission of the system too. */
    public fun setChangeAlerts(enabled: Boolean) {
        if (!enabled) {
            changeSettings.enabled = false
            mutableState.value = readState().copy(permissionDenied = mutableState.value.permissionDenied)
            return
        }
        scope.launch {
            val granted = scheduler.requestPermission()
            changeSettings.enabled = granted
            mutableState.value = readState().copy(permissionDenied = !granted)
        }
    }

    public fun setKindEnabled(kind: ReminderKind, enabled: Boolean) {
        settings.setKindEnabled(kind, enabled)
        mutableState.value = readState().copy(permissionDenied = mutableState.value.permissionDenied)
        plan()
    }

    private fun plan() {
        job?.cancel()
        job = scope.launch { runPlan() }
    }

    private suspend fun runPlan() {
        val kinds = settings.activeKinds()
        val schedule = selected
        if (kinds.isEmpty() || schedule == null) {
            scheduler.cancelAll()
            mutableState.value = readState().copy(permissionDenied = false)
            return
        }
        if (!scheduler.isPermissionGranted()) {
            scheduler.cancelAll()
            mutableState.value = readState().copy(permissionDenied = true)
            return
        }
        val schedules = (0 until WEEKS_TO_PLAN).mapNotNull { offset ->
            try {
                loadWeek(schedule, offset)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                null
            }
        }
        // nothing was loaded: the reminders that were planned before stay, the next start tries again
        if (schedules.isEmpty()) return
        val reminders = ClassReminderPlanner.plan(schedules, clock.now(), kinds)
        scheduler.replaceAll(reminders)
        mutableState.value = readState().copy(planned = reminders.size)
    }

    private fun readState() = RemindersUiState(
        enabled = settings.enabled,
        dayBefore = settings.isKindEnabled(ReminderKind.DAY_BEFORE),
        hoursBefore = settings.isKindEnabled(ReminderKind.HOURS_BEFORE),
        changeAlerts = changeSettings.enabled,
    )

    private companion object {
        /** This week and the two next ones, as on Android. */
        const val WEEKS_TO_PLAN = 3
    }
}
