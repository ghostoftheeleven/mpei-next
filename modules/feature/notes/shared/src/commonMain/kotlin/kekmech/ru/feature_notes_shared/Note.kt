package kekmech.ru.feature_notes_shared

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * A note of the student for one class. The class is told by its name and by the date and the start time of it
 * ([dateTime]), the same as in the Android app.
 */
@Serializable
public data class Note(
    val content: String,
    val dateTime: LocalDateTime,
    val classesName: String,
    /** Which of the classes with the same name and time it is. */
    val target: Int = 0,
    /** `null` until the note is saved. */
    val id: Long? = null,
)
