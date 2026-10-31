package kekmech.ru.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_schedule_shared.Classes
import kekmech.ru.feature_schedule_shared.ClassesType
import kekmech.ru.shared.schedule.formatTime

/**
 * A class on the schedule and on the home screen: the same card as in the Android app. The time is on the left, the kind of
 * the class has its color, then the name, the room, the teacher and the note.
 *
 * @param caption a line above the name (the day, "going now")
 * @param highlighted the class that goes now
 */
@Composable
internal fun ClassCard(
    classes: Classes,
    note: Note?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlighted) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(48.dp), horizontalAlignment = Alignment.End) {
                Text(formatTime(classes.time.start), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatTime(classes.time.end),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (caption != null) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                KindLine(classes)
                Text(classes.name, style = MaterialTheme.typography.titleMedium)
                val place = classes.place.takeIf { it.isNotBlank() }
                val person = classes.person.takeIf { it.isNotBlank() }
                if (place != null || person != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (place != null) {
                            Text(place, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (person != null) {
                            Text(person, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (classes.groups.isNotBlank()) {
                    Text(classes.groups, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (note != null) {
                    Text(
                        "✎ ${NotesController.preview(note)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/** The colored dot and the name of the kind ("Лекция"), and the number of the class. */
@Composable
private fun KindLine(classes: Classes) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val title = typeTitle(classes.type, classes.rawType)
        if (title.isNotBlank()) {
            Surface(Modifier.size(8.dp), shape = CircleShape, color = classes.type.color()) {}
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (classes.number > 0) {
            Text("${classes.number} пара", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun typeTitle(type: ClassesType, raw: String?): String = when (type) {
    ClassesType.LECTURE -> "Лекция"
    ClassesType.PRACTICE -> "Практика"
    ClassesType.LAB -> "Лабораторная"
    ClassesType.COURSE -> "Курсовая"
    ClassesType.EXAM -> "Экзамен"
    ClassesType.CONSULTATION -> "Консультация"
    ClassesType.UNDEFINED -> raw.orEmpty()
}

/** The colors of the kinds are the ones of the Android app. */
private fun ClassesType.color(): Color = when (this) {
    ClassesType.LECTURE -> Color(0xFF578B0F)
    ClassesType.PRACTICE -> Color(0xFFF9A825)
    ClassesType.LAB -> Color(0xFFDC3F32)
    ClassesType.COURSE -> Color(0xFF673AB7)
    ClassesType.EXAM -> Color(0xFFFF505C)
    ClassesType.CONSULTATION -> Color(0xFF009688)
    ClassesType.UNDEFINED -> Color(0xFF9E9E9E)
}
