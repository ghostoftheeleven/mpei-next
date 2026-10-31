package kekmech.ru.feature_notes_shared

import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class NotesControllerTest {

    private val keyValues = InMemoryKeyValueStore()
    private val store = NotesStore(keyValues)
    private val controller = NotesController(store).also { it.setSchedule("А-01-22") }

    private val physics = LocalDateTime(2026, 10, 27, 9, 20)
    private val math = LocalDateTime(2026, 10, 28, 11, 10)

    @Test
    fun `a saved note is found for its class and gets an id`() {
        controller.save("Физика", physics, "  Принести тетрадь  ")

        val note = assertNotNull(controller.noteFor("Физика", physics))
        assertEquals("Принести тетрадь", note.content)
        assertNotNull(note.id)
        assertEquals(listOf(note), controller.notes.value)
    }

    @Test
    fun `saving again changes the same note`() {
        controller.save("Физика", physics, "первая")
        val first = controller.noteFor("Физика", physics)

        controller.save("Физика", physics, "вторая")

        assertEquals(1, controller.notes.value.size)
        assertEquals(first?.id, controller.notes.value.single().id)
        assertEquals("вторая", controller.notes.value.single().content)
    }

    @Test
    fun `a blank text removes the note`() {
        controller.save("Физика", physics, "текст")

        controller.save("Физика", physics, "   ")

        assertNull(controller.noteFor("Физика", physics))
        assertEquals(emptyList(), controller.notes.value)
    }

    @Test
    fun `the notes are listed with the newest first`() {
        controller.save("Физика", physics, "а")
        controller.save("Математика", math, "б")

        assertEquals(listOf("Математика", "Физика"), controller.notes.value.map { it.classesName })
    }

    @Test
    fun `the notes of one schedule are not in another`() {
        controller.save("Физика", physics, "текст")

        controller.setSchedule("А-02-22")
        assertEquals(emptyList(), controller.notes.value)

        controller.setSchedule("а-01-22 ")
        assertEquals(1, controller.notes.value.size)
    }

    @Test
    fun `the notes stay after the controller is made again`() {
        controller.save("Физика", physics, "текст")

        val again = NotesController(NotesStore(keyValues)).also { it.setSchedule("А-01-22") }

        assertEquals("текст", again.noteFor("Физика", physics)?.content)
    }

    @Test
    fun `ids are not reused after a note is deleted`() {
        controller.save("Физика", physics, "а")
        val firstId = controller.notes.value.single().id
        controller.delete(controller.notes.value.single())

        controller.save("Математика", math, "б")

        assertEquals(1, controller.notes.value.size)
        assertEquals(true, controller.notes.value.single().id != firstId)
    }

    @Test
    fun `no schedule means no notes and nothing is saved`() {
        controller.setSchedule(null)

        controller.save("Физика", physics, "текст")

        assertEquals(emptyList(), controller.notes.value)
    }

    @Test
    fun `the preview is on one line`() {
        val note = Note(content = "раз\nдва", dateTime = physics, classesName = "Физика")

        assertEquals("раз два", NotesController.preview(note))
    }
}
