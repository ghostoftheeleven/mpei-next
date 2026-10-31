package kekmech.ru.feature_notes_impl.domain.usecase

import kekmech.ru.feature_notes_api.domain.model.Note
import kekmech.ru.feature_notes_api.domain.usecase.GetNotesForSelectedScheduleUseCase
import kekmech.ru.feature_notes_impl.data.repository.NotesRepository
import kekmech.ru.feature_schedule_api.data.repository.ScheduleRepository

internal class GetNotesForSelectedScheduleUseCaseImpl(
    private val scheduleRepository: ScheduleRepository,
    private val notesRepository: NotesRepository,
) : GetNotesForSelectedScheduleUseCase {

    override suspend operator fun invoke(): List<Note> {
        val selectedSchedule = scheduleRepository.getSelectedSchedule()
        return try {
            // TODO: get rid of SQL for notes
            notesRepository.getNotesBySchedule(selectedSchedule)
        } catch (e: android.database.sqlite.SQLiteDatabaseLockedException) {
            android.util.Log.w("GetNotesUseCase", "The database of the notes is locked", e)
            emptyList()
        } catch (e: Exception) {
            throw e
        }
    }
}
