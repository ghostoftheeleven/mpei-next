package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.moscowDate
import kekmech.ru.ext_datetime.startOfWeek
import kekmech.ru.lib_mpei_api.MpeiScheduleApi
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/** There is no group or teacher with this name in the database of MPEI. */
public class ScheduleNotFoundException(name: String) : Exception("Schedule not found in MPEI database for name: $name")

/**
 * Searches for the schedules and loads a week of one, through the official API of MPEI. The ids that the search has
 * found are remembered, so a week of a schedule is one request. Call it from one thread (the main one).
 */
public class ScheduleService(
    private val api: MpeiScheduleApi,
    private val clock: Clock = Clock.System,
) {

    private val idCache = mutableMapOf<String, Long>()

    /** @param type `null` searches both the groups and the teachers. */
    public suspend fun search(query: String, type: ScheduleType? = null): List<SearchResult> {
        val results = api.search(term = query, type = type?.pathName)
        for (result in results) {
            idCache[cacheKey(result.label)] = result.id
        }
        return results.map(MpeiScheduleMapper::mapSearchResult)
    }

    /**
     * The week that is [weekOffset] weeks from the current one (0 is this week, 1 is the next) of the schedule that
     * is called [name].
     */
    public suspend fun getWeek(type: ScheduleType, name: String, weekOffset: Int = 0): Schedule {
        val id = idOf(type, name)
        val monday = moscowDate(clock).startOfWeek().plus(weekOffset, DateTimeUnit.WEEK)
        val classes = api.getWeek(type = type.pathName, id = id, monday = monday)
        return MpeiScheduleMapper.mapWeek(name = name, id = id, type = type, monday = monday, classesList = classes)
    }

    private suspend fun idOf(type: ScheduleType, name: String): Long {
        val key = cacheKey(name)
        idCache[key]?.let { return it }
        val found = api.search(term = name, type = type.pathName)
        val match = found.firstOrNull { it.label.equals(name.trim(), ignoreCase = true) }
            ?: found.firstOrNull()
            ?: throw ScheduleNotFoundException(name)
        idCache[key] = match.id
        return match.id
    }

    private fun cacheKey(name: String): String = name.trim().lowercase()
}
