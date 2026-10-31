package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.moscowDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A class that has not ended yet; [inProgress] is `true` when it is going now. */
public data class UpcomingClass(
    val date: LocalDate,
    val classes: Classes,
    val inProgress: Boolean,
)

public sealed interface DashboardUiState {

    /** No schedule is chosen yet. */
    public data object NoSchedule : DashboardUiState

    public data class Loading(val selected: SelectedSchedule) : DashboardUiState

    /**
     * @param weekOfSemester the week of the semester that is going now
     * @param upcoming the classes of the next [UPCOMING_DAYS] days that have not ended, in the order they go
     * @param offline `true` when the data is the copy on the device because the network failed
     */
    public data class Content(
        val selected: SelectedSchedule,
        val today: LocalDate,
        val weekOfSemester: WeekOfSemester?,
        val upcoming: List<UpcomingClass>,
        val offline: Boolean,
    ) : DashboardUiState

    public data class Failed(val selected: SelectedSchedule, val message: String) : DashboardUiState
}

/** How many days ahead the dashboard looks, today included. */
public const val UPCOMING_DAYS: Int = 7

private const val MAX_UPCOMING = 12

/** The classes of [weeks] that have not ended by [now] and start within [UPCOMING_DAYS] days, in the order they go. */
public fun upcomingClasses(weeks: List<Week>, now: LocalDateTime): List<UpcomingClass> {
    val lastDay = now.date.plus(UPCOMING_DAYS - 1, DateTimeUnit.DAY)
    return weeks
        .flatMap { it.days }
        .filter { it.date >= now.date && it.date <= lastDay }
        .flatMap { day -> day.classes.map { day.date to it } }
        .filter { (date, classes) -> LocalDateTime(date, classes.time.end) > now }
        .sortedWith(compareBy({ it.first }, { it.second.time.start }))
        .map { (date, classes) ->
            UpcomingClass(
                date = date,
                classes = classes,
                inProgress = LocalDateTime(date, classes.time.start) <= now,
            )
        }
        .take(MAX_UPCOMING)
}

/**
 * What the home screen does: it loads this week and the next one of the chosen schedule and keeps the [state] with the
 * classes that are going and the ones that come next. A week that fails to load comes from the copy on the device.
 *
 * @param scope where the requests run; the controller does not outlive it
 */
@OptIn(ExperimentalTime::class)
public class DashboardController(
    private val loader: WeekLoader,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {

    private val mutableState = MutableStateFlow<DashboardUiState>(DashboardUiState.NoSchedule)
    public val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    private var selected: SelectedSchedule? = null
    private var job: Job? = null

    /** Call it when the chosen schedule changes (`null` when none is chosen) and when the screen appears again. */
    public fun show(schedule: SelectedSchedule?) {
        selected = schedule
        if (schedule == null) {
            job?.cancel()
            mutableState.value = DashboardUiState.NoSchedule
            return
        }
        val current = mutableState.value
        // keep what is on the screen while the new data comes, unless it is of another schedule
        if (current !is DashboardUiState.Content || current.selected != schedule) {
            mutableState.value = DashboardUiState.Loading(schedule)
        }
        load(schedule)
    }

    public fun refresh() {
        selected?.let(::show)
    }

    private fun load(schedule: SelectedSchedule) {
        job?.cancel()
        job = scope.launch {
            mutableState.value = try {
                val thisWeek = loader.load(schedule, weekOffset = 0)
                // the next week is for the classes of the coming days: without it the dashboard still works
                val nextWeek = runCatchingNotCancelled { loader.load(schedule, weekOffset = 1) }
                val now = moscowDateTime(clock)
                val weeks = thisWeek.schedule.weeks + nextWeek?.schedule?.weeks.orEmpty()
                DashboardUiState.Content(
                    selected = schedule,
                    today = now.date,
                    weekOfSemester = thisWeek.schedule.weeks.firstOrNull()?.weekOfSemester,
                    upcoming = upcomingClasses(weeks, now),
                    offline = thisWeek.offline || nextWeek?.offline == true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                DashboardUiState.Failed(schedule, describe(e))
            }
        }
    }

    private inline fun <T> runCatchingNotCancelled(block: () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            null
        }

    private fun describe(error: Exception): String = when (error) {
        is ScheduleNotFoundException -> "Расписание не найдено"
        else -> "Не удалось загрузить данные. Проверьте подключение к интернету"
    }
}
