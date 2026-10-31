package kekmech.ru.shared.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_schedule_shared.ReminderKind
import kekmech.ru.feature_schedule_shared.RemindersController

/** The settings: for now the reminders about the practices and the laboratory works. */
@Composable
public fun SettingsScreen(reminders: RemindersController, hasSchedule: Boolean, onBack: () -> Unit) {
    val state by reminders.state.collectAsState()
    Column(
        Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Назад") }
        }
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Напоминания о занятиях", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Практические и лабораторные занятия. Ночью (с 23:00 до 07:00) уведомления не приходят: " +
                        "они переносятся на 07:00.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.enabled, onCheckedChange = reminders::setEnabled)
        }
        if (state.permissionDenied) {
            Text(
                "Уведомления для МЭИ.Хаб отключены, напоминания не будут показываться. " +
                    "Включите их в Настройках iPhone: Уведомления → МЭИ.Хаб.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.enabled) {
            KindRow("За 24 часа до начала", state.dayBefore) { reminders.setKindEnabled(ReminderKind.DAY_BEFORE, it) }
            KindRow("За 5 часов до начала", state.hoursBefore) { reminders.setKindEnabled(ReminderKind.HOURS_BEFORE, it) }
            Text(
                when {
                    !hasSchedule -> "Выберите группу или преподавателя на вкладке «Расписание»."
                    state.planned == 0 -> "Подходящих занятий в ближайшие три недели нет."
                    else -> "Запланировано напоминаний: ${state.planned}. Они обновляются при каждом открытии приложения."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun KindRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}
