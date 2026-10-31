@file:OptIn(ExperimentalTime::class)

package kekmech.ru.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kekmech.ru.feature_bars_shared.BarsController
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_notes_shared.NotesStore
import kekmech.ru.feature_schedule_shared.FavoriteSchedules
import kekmech.ru.feature_schedule_shared.ScheduleCache
import kekmech.ru.feature_schedule_shared.ScheduleController
import kekmech.ru.feature_schedule_shared.ScheduleService
import kekmech.ru.feature_schedule_shared.ScheduleUiState
import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.shared.bars.BarsScreen
import kekmech.ru.shared.schedule.ScheduleScreen
import kekmech.ru.ui_theme.theme.MpeixTheme
import kotlin.time.ExperimentalTime

private const val TAB_SCHEDULE = 0
private const val TAB_BARS = 1

/**
 * The root of the shared UI. Two sections were moved from the Android app so far: the schedule (milestone 3 of the
 * iOS port, see issue #5) and BARS (milestone 6); the other sections move here module by module.
 *
 * @param bars the client of the BARS portal; the platform decides where its session is kept
 * @param scheduleService the timetable of MPEI
 * @param scheduleCache the offline copy of the schedule; the platform decides where it is kept
 * @param favorites the groups and the teachers that the student has marked
 * @param notesStore the notes of the student for the classes
 */
@Composable
public fun App(
    bars: BarsApi,
    scheduleService: ScheduleService,
    scheduleCache: ScheduleCache,
    favorites: FavoriteSchedules,
    notesStore: NotesStore,
) {
    MpeixTheme {
        Surface(Modifier.fillMaxSize()) {
            val scope = rememberCoroutineScope()
            val schedule = remember(scheduleService, scheduleCache, favorites) {
                ScheduleController(scheduleService, scheduleCache, favorites, scope)
            }
            val barsController = remember(bars) { BarsController(bars, scope) }
            val notes = remember(notesStore) { NotesController(notesStore) }
            val scheduleState by schedule.state.collectAsState()
            val notesSchedule = (scheduleState as? ScheduleUiState.Week)?.selected?.name
            LaunchedEffect(notes, notesSchedule) { notes.setSchedule(notesSchedule) }
            LaunchedEffect(schedule) { schedule.start() }
            LaunchedEffect(barsController) { barsController.start() }

            var tab by remember { mutableIntStateOf(TAB_SCHEDULE) }
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == TAB_SCHEDULE,
                            onClick = { tab = TAB_SCHEDULE },
                            icon = {},
                            label = { Text("Расписание") },
                        )
                        NavigationBarItem(
                            selected = tab == TAB_BARS,
                            onClick = { tab = TAB_BARS },
                            icon = {},
                            label = { Text("БАРС") },
                        )
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (tab) {
                        TAB_SCHEDULE -> ScheduleScreen(schedule, notes)
                        else -> BarsScreen(barsController)
                    }
                }
            }
        }
    }
}
