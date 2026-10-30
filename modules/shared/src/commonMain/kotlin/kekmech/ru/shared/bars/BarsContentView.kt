package kekmech.ru.shared.bars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_bars_shared.BarsTab
import kekmech.ru.feature_bars_shared.BarsUiState
import kekmech.ru.feature_bars_shared.controlActivities
import kekmech.ru.feature_bars_shared.currentScore
import kekmech.ru.feature_bars_shared.finalMark
import kekmech.ru.feature_bars_shared.formatMark
import kekmech.ru.feature_bars_shared.summaryActivities
import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.BarsRating
import kekmech.ru.lib_bars_parser.BarsSummaryRow

@Composable
internal fun ContentView(
    state: BarsUiState.Content,
    onSemester: (String) -> Unit,
    onTab: (BarsTab) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeContentPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.studentName ?: "БАРС", style = MaterialTheme.typography.titleLarge)
                state.group?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            TextButton(onClick = onSignOut) { Text("Выйти") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SemesterPicker(state, onSemester, Modifier.weight(1f))
            TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Обновить") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TabButton("Оценки", state.tab == BarsTab.MARKS, Modifier.weight(1f)) { onTab(BarsTab.MARKS) }
            TabButton("Сводка", state.tab == BarsTab.SUMMARY, Modifier.weight(1f)) { onTab(BarsTab.SUMMARY) }
        }

        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Box(Modifier.padding(horizontal = 16.dp)) { ErrorText(it) } }

        when (state.tab) {
            BarsTab.MARKS -> MarksList(state)
            BarsTab.SUMMARY -> SummaryList(state)
        }
    }
}

@Composable
private fun TabButton(title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(title) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(title) }
    }
}

@Composable
private fun SemesterPicker(state: BarsUiState.Content, onSemester: (String) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text(state.semesterName ?: "Семестр")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.semesters.forEach { semester ->
                DropdownMenuItem(
                    text = { Text(semester.name) },
                    onClick = {
                        open = false
                        onSemester(semester.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun MarksList(state: BarsUiState.Content) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.rating?.let { rating -> item { RatingCard(rating) } }
        if (state.disciplines.isEmpty() && !state.loading) {
            item { Text("В этом семестре пока нет дисциплин", style = MaterialTheme.typography.bodyLarge) }
        }
        items(state.disciplines) { DisciplineCard(it) }
    }
}

@Composable
private fun RatingCard(rating: BarsRating) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Рейтинг", style = MaterialTheme.typography.titleMedium)
            rating.items.forEach { Text("${it.label}: ${it.raw}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun DisciplineCard(discipline: BarsDiscipline) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(discipline.name, style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(discipline.person, discipline.assessmentType, discipline.units)
                    .filter { it.isNotEmpty() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
            discipline.deadline?.let { Text("Сдать до $it", style = MaterialTheme.typography.bodySmall) }
            if (discipline.notes.isNotEmpty()) {
                Text(discipline.notes.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Балл: ${formatMark(discipline.currentScore)}", style = MaterialTheme.typography.bodyMedium)
                Text("Итог: ${formatMark(discipline.finalMark)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (expanded) {
                discipline.controlActivities.forEach { activity ->
                    val mark = activity.mark
                    val retaken = if (mark != null && mark.isRetaken) " (пересдача)" else ""
                    Text(activity.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(
                            activity.weight?.let { "вес $it%" },
                            activity.weekNum?.let { "неделя $it" },
                            "оценка ${formatMark(mark)}$retaken",
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                discipline.summaryActivities.forEach { activity ->
                    Text("${activity.name} ${formatMark(activity.mark)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryList(state: BarsUiState.Content) {
    val summary = state.summary
    if (summary == null) {
        if (!state.loading) Box(Modifier.padding(16.dp)) { Text("Сводка не загружена") }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (summary.rows.isEmpty()) {
            item { Text("В этом семестре пока нет оценок", style = MaterialTheme.typography.bodyLarge) }
        }
        items(summary.rows) { SummaryCard(it, summary.currentWeek) }
    }
}

@Composable
private fun SummaryCard(row: BarsSummaryRow, currentWeek: Int?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.discipline, style = MaterialTheme.typography.titleMedium)
            row.assessmentType?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            row.weeks.forEach { week ->
                val marks = week.marks.joinToString(" ") { formatMark(it.mark) }
                val now = if (week.week == currentWeek) " · сейчас" else ""
                Text("Неделя ${week.week}$now: $marks", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "ТБ ${formatMark(row.currentScore)} · ПА ${formatMark(row.intermediate)} · И ${formatMark(row.finalMark)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
