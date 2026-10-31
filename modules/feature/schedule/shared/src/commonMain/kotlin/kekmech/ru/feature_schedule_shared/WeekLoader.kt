package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.moscowDate
import kekmech.ru.ext_datetime.startOfWeek
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A week of a schedule; [offline] is `true` when it comes from the copy on the device because the network failed. */
public class LoadedWeek(public val schedule: Schedule, public val offline: Boolean)

/**
 * Loads a week of a schedule: from the network, and from the copy on the device when the network fails. A week that
 * was loaded is kept in the [cache] for the next time.
 */
@OptIn(ExperimentalTime::class)
public class WeekLoader(
    private val service: ScheduleService,
    private val cache: ScheduleCache,
    private val clock: Clock = Clock.System,
) {

    /**
     * @param weekOffset 0 is this week, 1 is the next one
     * @throws Exception the error of the network, when there is no copy of the week either
     */
    public suspend fun load(selected: SelectedSchedule, weekOffset: Int): LoadedWeek =
        try {
            val schedule = service.getWeek(selected.type, selected.name, weekOffset)
            cache.saveWeek(selected, schedule)
            LoadedWeek(schedule, offline = false)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val cached = cache.week(selected, mondayOf(weekOffset)) ?: throw e
            LoadedWeek(cached, offline = true)
        }

    private fun mondayOf(weekOffset: Int): LocalDate =
        moscowDate(clock).startOfWeek().plus(weekOffset, DateTimeUnit.WEEK)
}
