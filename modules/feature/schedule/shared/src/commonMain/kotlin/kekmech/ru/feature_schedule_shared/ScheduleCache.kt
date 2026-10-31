package kekmech.ru.feature_schedule_shared

import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The schedule that the student has chosen: the one the app opens with. */
@Serializable
public data class SelectedSchedule(val type: ScheduleType, val name: String)

/**
 * The offline copy of the schedule: the last chosen schedule and the weeks that were loaded. A week is kept under the
 * date of its Monday, so a week that was loaded once can be shown without the network. Only the last
 * [MAX_WEEKS] weeks are kept.
 */
public class ScheduleCache(
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    public fun selected(): SelectedSchedule? = read(KEY_SELECTED)

    public fun saveSelected(selected: SelectedSchedule) {
        store.putString(KEY_SELECTED, json.encodeToString(SelectedSchedule.serializer(), selected))
    }

    public fun clearSelected() {
        store.remove(KEY_SELECTED)
    }

    public fun week(selected: SelectedSchedule, monday: LocalDate): Schedule? =
        read<Schedule>(weekKey(selected, monday))

    public fun saveWeek(selected: SelectedSchedule, schedule: Schedule) {
        val monday = schedule.weeks.firstOrNull()?.firstDayOfWeek ?: return
        val key = weekKey(selected, monday)
        store.putString(key, json.encodeToString(Schedule.serializer(), schedule))
        val index = (readIndex() - key) + key
        val dropped = index.size - MAX_WEEKS
        index.take(dropped.coerceAtLeast(0)).forEach(store::remove)
        store.putString(KEY_INDEX, index.drop(dropped.coerceAtLeast(0)).joinToString(INDEX_SEPARATOR))
    }

    private inline fun <reified T> read(key: String): T? {
        val text = store.getString(key) ?: return null
        return runCatching { json.decodeFromString<T>(text) }.getOrNull()
    }

    private fun readIndex(): List<String> =
        store.getString(KEY_INDEX)?.split(INDEX_SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()

    private fun weekKey(selected: SelectedSchedule, monday: LocalDate): String =
        "$KEY_WEEK_PREFIX${selected.type.pathName}:${selected.name.trim().lowercase()}:$monday"

    public companion object {
        public const val MAX_WEEKS: Int = 12

        private const val KEY_SELECTED = "mpeix_schedule_selected"
        private const val KEY_INDEX = "mpeix_schedule_weeks_index"
        private const val KEY_WEEK_PREFIX = "mpeix_schedule_week:"
        private const val INDEX_SEPARATOR = "\n"
    }
}
