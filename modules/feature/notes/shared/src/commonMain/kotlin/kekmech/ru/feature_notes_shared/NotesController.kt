package kekmech.ru.feature_notes_shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime

/**
 * What the notes of the shown schedule do: it keeps the [notes] that the screen draws. The work with the store is quick
 * and local, so it runs at once on the thread of the screen.
 */
public class NotesController(private val store: NotesStore) {

    private var scheduleName: String? = null
    private val mutableNotes = MutableStateFlow<List<Note>>(emptyList())

    /** The notes of the schedule that was given to [setSchedule], the newest first. */
    public val notes: StateFlow<List<Note>> = mutableNotes.asStateFlow()

    /** Call it when the shown schedule changes; `null` when no schedule is chosen. */
    public fun setSchedule(name: String?) {
        scheduleName = name
        mutableNotes.value = name?.let(store::list).orEmpty()
    }

    /** The note for the class [classesName] that starts at [dateTime], or `null`. */
    public fun noteFor(classesName: String, dateTime: LocalDateTime): Note? =
        mutableNotes.value.firstOrNull { it.classesName == classesName && it.dateTime == dateTime }

    /** Saves the text of the note of the class; a blank text removes the note. */
    public fun save(classesName: String, dateTime: LocalDateTime, text: String) {
        val name = scheduleName ?: return
        val existing = noteFor(classesName, dateTime)
        when {
            text.isBlank() -> existing?.id?.let { store.delete(name, it) }
            existing != null -> store.put(name, existing.copy(content = text.trim()))
            else -> store.put(name, Note(content = text.trim(), dateTime = dateTime, classesName = classesName))
        }
        mutableNotes.value = store.list(name)
    }

    public fun delete(note: Note) {
        val name = scheduleName ?: return
        note.id?.let { store.delete(name, it) }
        mutableNotes.value = store.list(name)
    }

    public companion object {

        /** The text of a note on one line, for the card of a class. */
        public fun preview(note: Note): String = note.content.replace('\n', ' ')
    }
}
