package kekmech.ru.shared.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_schedule_shared.Classes
import kekmech.ru.shared.ClassCard
import kekmech.ru.feature_schedule_shared.ClassesType
import kekmech.ru.feature_schedule_shared.Day
import kekmech.ru.feature_schedule_shared.ScheduleController
import kekmech.ru.feature_schedule_shared.ScheduleType
import kekmech.ru.feature_schedule_shared.SelectedSchedule
import kekmech.ru.feature_schedule_shared.SearchResult
import kekmech.ru.feature_schedule_shared.ScheduleUiState
import kekmech.ru.feature_schedule_shared.Week
import kekmech.ru.feature_schedule_shared.WeekOfSemester
import kekmech.ru.ui_shimmer.shimmer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** The schedule section: draws what [controller] says and [notes] keeps, and reports the taps to them. */
@Composable
public fun ScheduleScreen(controller: ScheduleController, notes: NotesController) {
    val state by controller.state.collectAsState()
    when (val current = state) {
        is ScheduleUiState.Search -> SearchView(
            state = current,
            onQuery = controller::onQueryChange,
            onSelect = controller::select,
            onFavorite = controller::openFavorite,
            onRemoveFavorite = controller::removeFavorite,
        )
        is ScheduleUiState.Week -> WeekView(
            state = current,
            onPrevious = { controller.shiftWeek(-1) },
            onNext = { controller.shiftWeek(1) },
            onRefresh = controller::refresh,
            onChange = controller::changeSchedule,
            onToggleFavorite = controller::toggleFavorite,
            notes = notes,
        )
    }
}

@Composable
private fun SearchView(
    state: ScheduleUiState.Search,
    onQuery: (String) -> Unit,
    onSelect: (SearchResult) -> Unit,
    onFavorite: (SelectedSchedule) -> Unit,
    onRemoveFavorite: (SelectedSchedule) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Расписание", style = MaterialTheme.typography.headlineSmall)
        if (state.favorites.isEmpty() && state.query.isEmpty()) {
            Text(
                "Введите номер группы (например, А-01-22) или фамилию преподавателя, чтобы открыть расписание.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            label = { Text("Группа или преподаватель") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            state.busy -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            state.query.trim().length >= 2 && state.results.isEmpty() -> Text("Ничего не найдено")
        }
        LazyColumn {
            if (state.query.trim().length < 2 && state.favorites.isNotEmpty()) {
                item { Text("Избранное", style = MaterialTheme.typography.titleSmall) }
                items(state.favorites) { favorite ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onFavorite(favorite) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(favorite.name, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onRemoveFavorite(favorite) }) { Text("Убрать") }
                    }
                    HorizontalDivider()
                }
            }
            items(state.results) { result ->
                Column(Modifier.fillMaxWidth().clickable { onSelect(result) }.padding(vertical = 10.dp)) {
                    Text(result.name, style = MaterialTheme.typography.titleMedium)
                    val kind = if (result.type == ScheduleType.GROUP) "Группа" else "Преподаватель"
                    Text(
                        listOf(kind, result.description).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WeekView(
    state: ScheduleUiState.Week,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    onChange: () -> Unit,
    onToggleFavorite: () -> Unit,
    notes: NotesController,
) {
    val noteList by notes.notes.collectAsState()
    var editing by remember { mutableStateOf<NoteTarget?>(null) }
    editing?.let { target ->
        NoteDialog(
            target = target,
            initial = noteList.firstOrNull { it.classesName == target.classes.name && it.dateTime == target.dateTime },
            onSave = { text ->
                notes.save(target.classes.name, target.dateTime, text)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(state.selected.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggleFavorite) { Text(if (state.favorite) "★ В избранном" else "☆ В избранное") }
                TextButton(onClick = onChange) { Text("Сменить") }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onPrevious) { Text("‹ Назад") }
            Text(weekTitle(state), style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onNext) { Text("Вперёд ›") }
        }
        if (state.offline) {
            Text(
                "Нет сети, показана сохранённая копия",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val schedule = state.schedule
        when {
            state.loading && schedule == null ->
                WeekSkeleton()
            schedule == null -> Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.error ?: "Нет данных")
                Button(onClick = onRefresh) { Text("Повторить") }
            }
            else -> DaysList(schedule.weeks.firstOrNull(), noteList) { target -> editing = target }
        }
    }
}

/** The class that a note is written for: the class and the moment it starts. */
private class NoteTarget(val classes: Classes, val dateTime: LocalDateTime)

@Composable
private fun DaysList(week: Week?, notes: List<Note>, onEditNote: (NoteTarget) -> Unit) {
    if (week == null) return
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(week.days) { day -> DayView(day, notes, onEditNote) }
    }
}

@Composable
private fun DayView(day: Day, notes: List<Note>, onEditNote: (NoteTarget) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(dayTitle(day.date), style = MaterialTheme.typography.titleMedium)
        if (day.classes.isEmpty()) {
            Text("Занятий нет", style = MaterialTheme.typography.bodySmall)
        }
        day.classes.forEach { classes ->
            val dateTime = LocalDateTime(day.date, classes.time.start)
            val note = notes.firstOrNull { it.classesName == classes.name && it.dateTime == dateTime }
            ClassCard(classes, note, onClick = { onEditNote(NoteTarget(classes, dateTime)) })
        }
    }
}

@Composable
private fun NoteDialog(target: NoteTarget, initial: Note?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(target) { mutableStateOf(initial?.content.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.classes.name) },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

private fun weekTitle(state: ScheduleUiState.Week): String {
    val week = state.schedule?.weeks?.firstOrNull() ?: return when (state.weekOffset) {
        0 -> "Эта неделя"
        else -> "Неделя ${if (state.weekOffset > 0) "+" else ""}${state.weekOffset}"
    }
    return when (val number = week.weekOfSemester) {
        is WeekOfSemester.Studying -> "${number.num} неделя"
        WeekOfSemester.NonStudying -> "Каникулы / сессия"
    }
}

private val DAY_NAMES = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
private val MONTHS = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

internal fun dayTitle(date: LocalDate): String =
    "${DAY_NAMES[date.dayOfWeek.ordinal]}, ${date.dayOfMonth} ${MONTHS[date.monthNumber - 1]}"

internal fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

private const val SKELETON_DAYS = 3
private const val SKELETON_CLASSES_PER_DAY = 2

/** The grey blocks that stand where the days and the classes will be while the week loads. */
@Composable
private fun WeekSkeleton() {
    val color = MaterialTheme.colorScheme.surfaceVariant
    Column(
        Modifier.fillMaxSize().padding(16.dp).shimmer(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(SKELETON_DAYS) {
            Box(Modifier.fillMaxWidth(SKELETON_TITLE_WIDTH).height(20.dp).clip(RoundedCornerShape(6.dp)).background(color))
            repeat(SKELETON_CLASSES_PER_DAY) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(10.dp)).background(color))
            }
        }
    }
}

private const val SKELETON_TITLE_WIDTH = 0.5f
