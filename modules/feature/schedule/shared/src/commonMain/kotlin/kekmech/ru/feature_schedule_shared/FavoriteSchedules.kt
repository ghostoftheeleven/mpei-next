package kekmech.ru.feature_schedule_shared

import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The groups and the teachers that the student has marked, in the order they were added. Two entries are the same
 * when the type and the name (without the case) are the same.
 */
public class FavoriteSchedules(
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    public fun list(): List<SelectedSchedule> {
        val text = store.getString(KEY_FAVORITES) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, text) }.getOrDefault(emptyList())
    }

    public fun contains(schedule: SelectedSchedule): Boolean = list().any { it.sameAs(schedule) }

    public fun add(schedule: SelectedSchedule) {
        if (contains(schedule)) return
        write(list() + schedule)
    }

    public fun remove(schedule: SelectedSchedule) {
        write(list().filterNot { it.sameAs(schedule) })
    }

    /** @return `true` when [schedule] is a favorite after the call. */
    public fun toggle(schedule: SelectedSchedule): Boolean {
        if (contains(schedule)) {
            remove(schedule)
            return false
        }
        add(schedule)
        return true
    }

    private fun write(value: List<SelectedSchedule>) {
        store.putString(KEY_FAVORITES, json.encodeToString(serializer, value))
    }

    private fun SelectedSchedule.sameAs(other: SelectedSchedule): Boolean =
        type == other.type && name.trim().equals(other.name.trim(), ignoreCase = true)

    private companion object {
        const val KEY_FAVORITES = "mpeix_schedule_favorites"
        val serializer = ListSerializer(SelectedSchedule.serializer())
    }
}
