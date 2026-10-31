package kekmech.ru.shared.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.shared.schedule.dayTitle
import kekmech.ru.shared.schedule.formatTime

/** All the notes of the chosen schedule, the newest first; a tap opens a note for editing. */
@Composable
public fun NotesScreen(controller: NotesController, scheduleName: String?) {
    val notes by controller.notes.collectAsState()
    var editing by remember { mutableStateOf<Note?>(null) }
    editing?.let { note ->
        EditNoteDialog(
            note = note,
            onSave = { text ->
                controller.save(note.classesName, note.dateTime, text)
                editing = null
            },
            onDelete = {
                controller.delete(note)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    Column(
        Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Заметки", style = MaterialTheme.typography.headlineSmall)
        when {
            scheduleName == null -> Text("Выберите группу или преподавателя на вкладке «Расписание».")
            notes.isEmpty() -> Text(
                "Заметок для $scheduleName пока нет. Чтобы добавить, нажмите на пару в расписании.",
            )
            else -> LazyColumn {
                items(notes) { note ->
                    Column(Modifier.fillMaxWidth().clickable { editing = note }.padding(vertical = 10.dp)) {
                        Text(note.classesName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${dayTitle(note.dateTime.date)}, ${formatTime(note.dateTime.time)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(NotesController.preview(note), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EditNoteDialog(note: Note, onSave: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var text by remember(note) { mutableStateOf(note.content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.classesName) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Заметка") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Сохранить") } },
        dismissButton = {
            Column {
                TextButton(onClick = onDelete) { Text("Удалить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}
