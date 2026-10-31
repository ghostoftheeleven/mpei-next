package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.moscowDate
import kekmech.ru.ext_datetime.startOfWeek
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

public sealed interface ScheduleUiState {

    /** The student looks for a group or a teacher. */
    public data class Search(
        val query: String = "",
        val results: List<SearchResult> = emptyList(),
        val busy: Boolean = false,
        val favorites: List<SelectedSchedule> = emptyList(),
        val error: String? = null,
    ) : ScheduleUiState

    /**
     * A week of the chosen schedule. [schedule] is `null` until the first load ends; [offline] is `true` when the week
     * comes from the copy on the device because the network failed.
     */
    public data class Week(
        val selected: SelectedSchedule,
        val weekOffset: Int = 0,
        val schedule: Schedule? = null,
        val loading: Boolean = true,
        val offline: Boolean = false,
        val favorite: Boolean = false,
        val error: String? = null,
    ) : ScheduleUiState
}

/**
 * What the schedule screen does: it keeps the [state] that the screen draws and changes it when the screen reports a
 * tap. The chosen schedule and the loaded weeks are kept in [cache], so the next start opens the week at once and a
 * week that failed to load is shown from the copy.
 *
 * @param scope where the requests run; the controller does not outlive it
 */
public class ScheduleController(
    private val service: ScheduleService,
    private val cache: ScheduleCache,
    private val favorites: FavoriteSchedules,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {

    private val mutableState = MutableStateFlow<ScheduleUiState>(initialState())
    public val state: StateFlow<ScheduleUiState> = mutableState.asStateFlow()

    private var searchJob: Job? = null
    private var weekJob: Job? = null

    /** Loads the week if the app opens with a chosen schedule. Call once, when the screen appears. */
    public fun start() {
        val current = mutableState.value
        if (current is ScheduleUiState.Week && current.schedule == null) loadWeek(current)
    }

    public fun onQueryChange(query: String) {
        val current = mutableState.value as? ScheduleUiState.Search ?: return
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY_LENGTH) {
            mutableState.value = searchState(query = query)
            return
        }
        mutableState.value = current.copy(query = query, busy = true, error = null)
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            val next = try {
                val results = service.search(query.trim())
                searchState(query = query, results = results)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                searchState(query = query, error = describe(e))
            }
            mutableState.value = next
        }
    }

    public fun select(result: SearchResult) {
        open(SelectedSchedule(type = result.type, name = result.name))
    }

    /** Opens a schedule from the list of the favorites. */
    public fun openFavorite(selected: SelectedSchedule) {
        open(selected)
    }

    /** Adds the shown schedule to the favorites, or removes it from them. */
    public fun toggleFavorite() {
        val current = mutableState.value as? ScheduleUiState.Week ?: return
        mutableState.value = current.copy(favorite = favorites.toggle(current.selected))
    }

    public fun removeFavorite(selected: SelectedSchedule) {
        favorites.remove(selected)
        val current = mutableState.value as? ScheduleUiState.Search ?: return
        mutableState.value = current.copy(favorites = favorites.list())
    }

    private fun open(selected: SelectedSchedule) {
        if (mutableState.value !is ScheduleUiState.Search) return
        searchJob?.cancel()
        cache.saveSelected(selected)
        val week = weekState(selected)
        mutableState.value = week
        loadWeek(week)
    }

    private fun weekState(selected: SelectedSchedule) =
        ScheduleUiState.Week(selected, favorite = favorites.contains(selected))

    private fun searchState(
        query: String = "",
        results: List<SearchResult> = emptyList(),
        error: String? = null,
    ) = ScheduleUiState.Search(query = query, results = results, favorites = favorites.list(), error = error)

    /** [delta] weeks forward (positive) or back (negative) from the shown one. */
    public fun shiftWeek(delta: Int) {
        val current = mutableState.value as? ScheduleUiState.Week ?: return
        val next = current.copy(
            weekOffset = current.weekOffset + delta,
            schedule = null,
            loading = true,
            offline = false,
            error = null,
        )
        mutableState.value = next
        loadWeek(next)
    }

    public fun refresh() {
        val current = mutableState.value as? ScheduleUiState.Week ?: return
        val next = current.copy(loading = true, error = null)
        mutableState.value = next
        loadWeek(next)
    }

    /** Back to the search of the group; the cached weeks of the previous schedule stay on the device. */
    public fun changeSchedule() {
        weekJob?.cancel()
        cache.clearSelected()
        mutableState.value = searchState()
    }

    private fun loadWeek(week: ScheduleUiState.Week) {
        weekJob?.cancel()
        weekJob = scope.launch {
            val monday = mondayOf(week.weekOffset)
            val next = try {
                val schedule = service.getWeek(week.selected.type, week.selected.name, week.weekOffset)
                cache.saveWeek(week.selected, schedule)
                week.copy(schedule = schedule, loading = false, offline = false, error = null)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                val cached = cache.week(week.selected, monday)
                if (cached != null) {
                    week.copy(schedule = cached, loading = false, offline = true, error = null)
                } else {
                    week.copy(schedule = null, loading = false, offline = false, error = describe(e))
                }
            }
            mutableState.value = next
        }
    }

    private fun mondayOf(weekOffset: Int): LocalDate =
        moscowDate(clock).startOfWeek().plus(weekOffset, DateTimeUnit.WEEK)

    private fun initialState(): ScheduleUiState =
        cache.selected()?.let { weekState(it) } ?: searchState()

    private fun describe(error: Exception): String = when (error) {
        is ScheduleNotFoundException -> "Расписание не найдено"
        else -> "Не удалось загрузить данные. Проверьте подключение к интернету"
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
