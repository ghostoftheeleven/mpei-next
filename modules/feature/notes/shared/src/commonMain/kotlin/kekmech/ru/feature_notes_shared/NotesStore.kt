package kekmech.ru.feature_notes_shared

import kekmech.ru.lib_settings.KeyValueStore
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The notes on the device. The notes belong to a schedule (a group or a teacher), so the notes of one group do not
 * show up in the schedule of another one.
 */
public class NotesStore(
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    /** The newest first. */
    public fun list(scheduleName: String): List<Note> =
        read(scheduleName).sortedByDescending { it.dateTime }

    /** The note for the class that starts at [dateTime], or `null`. */
    public fun find(scheduleName: String, classesName: String, dateTime: LocalDateTime): Note? =
        read(scheduleName).firstOrNull { it.classesName == classesName && it.dateTime == dateTime }

    /** Adds the note, or replaces the one with the same id. @return the note with its id. */
    public fun put(scheduleName: String, note: Note): Note {
        val notes = read(scheduleName)
        val saved = note.copy(id = note.id ?: nextId())
        write(scheduleName, notes.filterNot { it.id == saved.id } + saved)
        return saved
    }

    public fun delete(scheduleName: String, noteId: Long) {
        write(scheduleName, read(scheduleName).filterNot { it.id == noteId })
    }

    private fun read(scheduleName: String): List<Note> {
        val text = store.getString(key(scheduleName)) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, text) }.getOrDefault(emptyList())
    }

    private fun write(scheduleName: String, notes: List<Note>) {
        if (notes.isEmpty()) {
            store.remove(key(scheduleName))
        } else {
            store.putString(key(scheduleName), json.encodeToString(serializer, notes))
        }
    }

    private fun nextId(): Long {
        val next = (store.getString(KEY_LAST_ID)?.toLongOrNull() ?: 0L) + 1
        store.putString(KEY_LAST_ID, next.toString())
        return next
    }

    private fun key(scheduleName: String): String = "$KEY_NOTES_PREFIX${scheduleName.trim().lowercase()}"

    private companion object {
        const val KEY_NOTES_PREFIX = "mpeix_notes:"
        const val KEY_LAST_ID = "mpeix_notes_last_id"
        val serializer = ListSerializer(Note.serializer())
    }
}
