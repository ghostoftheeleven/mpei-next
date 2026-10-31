package kekmech.ru.shared.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_schedule_shared.DashboardController
import kekmech.ru.feature_schedule_shared.DashboardUiState
import kekmech.ru.feature_schedule_shared.UPCOMING_DAYS
import kekmech.ru.feature_schedule_shared.UpcomingClass
import kekmech.ru.feature_schedule_shared.WeekOfSemester
import kekmech.ru.shared.ClassCard
import kekmech.ru.shared.schedule.dayTitle
import kekmech.ru.shared.schedule.formatTime
import kotlinx.datetime.LocalDateTime

/** The home screen: the week of the semester and the classes that are going now and come next. */
@Composable
public fun DashboardScreen(
    controller: DashboardController,
    notes: NotesController,
    onChooseSchedule: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by controller.state.collectAsState()
    val noteList by notes.notes.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Главная", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onOpenSettings) { Text("Настройки") }
        }
        when (val current = state) {
            DashboardUiState.NoSchedule -> {
                Text("Выберите группу или преподавателя, чтобы видеть ближайшие пары.")
                Button(onClick = onChooseSchedule) { Text("Выбрать расписание") }
            }
            is DashboardUiState.Loading ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is DashboardUiState.Failed -> {
                Text(current.selected.name, style = MaterialTheme.typography.titleLarge)
                Text(current.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = controller::refresh) { Text("Повторить") }
            }
            is DashboardUiState.Content -> ContentView(current, noteList)
        }
    }
}

@Composable
private fun ContentView(state: DashboardUiState.Content, notes: List<Note>) {
    Text(state.selected.name, style = MaterialTheme.typography.titleLarge)
    Text(
        when (val week = state.weekOfSemester) {
            is WeekOfSemester.Studying -> "${dayTitle(state.today)} · идёт ${week.num}-я учебная неделя"
            WeekOfSemester.NonStudying -> "${dayTitle(state.today)} · каникулы или сессия"
            null -> dayTitle(state.today)
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (state.offline) {
        Text(
            "Нет сети, показана сохранённая копия",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Text("Ближайшие пары", style = MaterialTheme.typography.titleMedium)
    if (state.upcoming.isEmpty()) {
        Text("На ближайшие $UPCOMING_DAYS дней пар нет", style = MaterialTheme.typography.bodyMedium)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.upcoming) { upcoming ->
            UpcomingView(upcoming, today = upcoming.date == state.today, note = noteOf(upcoming, notes))
        }
    }
}

@Composable
private fun UpcomingView(upcoming: UpcomingClass, today: Boolean, note: Note?) {
    val dayLabel = if (today) "Сегодня" else dayTitle(upcoming.date)
    ClassCard(
        classes = upcoming.classes,
        note = note,
        caption = if (upcoming.inProgress) "$dayLabel · идёт сейчас" else dayLabel,
        highlighted = upcoming.inProgress,
    )
}

private fun noteOf(upcoming: UpcomingClass, notes: List<Note>): Note? {
    val dateTime = LocalDateTime(upcoming.date, upcoming.classes.time.start)
    return notes.firstOrNull { it.classesName == upcoming.classes.name && it.dateTime == dateTime }
}
