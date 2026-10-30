package kekmech.ru.lib_bars_parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarsTextTest {

    @Test
    fun `a mark with a date`() {
        val mark = assertNotNull(parseBarsMark("4  (06.06.26)"))

        assertEquals(4f, mark.value)
        assertEquals("06.06.26", mark.date)
        assertNull(mark.note)
        assertFalse(mark.isRetaken)
    }

    @Test
    fun `a decimal mark with a comma`() {
        assertEquals(2.5f, parseBarsMark("2,5")?.value)
        assertEquals(0f, parseBarsMark("0,00")?.value)
    }

    @Test
    fun `a mark with a retake`() {
        val mark = assertNotNull(parseBarsMark("4  (06.06.26)", "Пересдана: 0 (11.04.26)"))

        assertTrue(mark.isRetaken)
        assertEquals("0", mark.retakenFrom)
        assertEquals("11.04.26", mark.retakenDate)
        assertEquals("4 (06.06.26) Пересдана: 0 (11.04.26)", mark.raw)
    }

    @Test
    fun `a mark with a note`() {
        val mark = assertNotNull(parseBarsMark("2 ППА1 - 3 (не уч.)"))

        assertEquals(2f, mark.value)
        assertNull(mark.date)
        assertEquals("ППА1 - 3 (не уч.)", mark.note)
    }

    @Test
    fun `an attestation with the tags and the repeated attempts`() {
        val mark = assertNotNull(parseBarsMark("3 (НД, 15.06.26) ППА1 - 2 (НД, 20.06.26) ППА2 - 3 (22.06.26)"))

        assertEquals(3f, mark.value)
        assertEquals("НД", mark.tag)
        assertEquals("15.06.26", mark.date)
        assertEquals("ППА1 - 2 (НД, 20.06.26) ППА2 - 3 (22.06.26)", mark.note)

        assertEquals(2, mark.attempts.size)
        val (first, second) = mark.attempts
        assertEquals("ППА1", first.label)
        assertEquals(2f, first.value)
        assertEquals("НД", first.tag)
        assertEquals("20.06.26", first.date)
        assertEquals("ППА2", second.label)
        assertEquals(3f, second.value)
        assertNull(second.tag)
        assertEquals("22.06.26", second.date)
    }

    @Test
    fun `the date of an attempt is not the date of the mark`() {
        val mark = assertNotNull(parseBarsMark("2 ППА1 - 3 (22.06.26)"))

        assertNull(mark.date)
        assertEquals("22.06.26", mark.attempts.single().date)
    }

    @Test
    fun `a final mark with verdicts`() {
        val mark = assertNotNull(parseBarsMark("2/не зачтено ППА1 - 3/зачтено (не уч.)"))

        assertEquals(2f, mark.value)
        assertEquals("не зачтено", mark.verdict)
        val attempt = mark.attempts.single()
        assertEquals("зачтено", attempt.verdict)
        assertEquals(3f, attempt.value)
        assertEquals("не уч.", attempt.tag)
        assertNull(attempt.date)

        val plain = assertNotNull(parseBarsMark("5/зачтено"))
        assertEquals(5f, plain.value)
        assertEquals("зачтено", plain.verdict)
        assertNull(plain.note)
    }

    @Test
    fun `a mark that is not counted any more`() {
        val mark = assertNotNull(parseBarsMark("4 (06.06.26) Не учитывается: 5 (01.06.26)", "Пересдана: 0 (11.04.26)"))

        assertEquals(4f, mark.value)
        assertEquals("06.06.26", mark.date)
        assertEquals("5", mark.ignoredFrom)
        assertEquals("01.06.26", mark.ignoredDate)
        assertNull(mark.note)
        assertEquals("0", mark.retakenFrom)
    }

    @Test
    fun `a score with a comma`() {
        assertEquals(2.66f, parseBarsMark("2,66")?.value)
    }

    @Test
    fun `text without a number keeps its raw value`() {
        val mark = assertNotNull(parseBarsMark("зачтено (06.06.26)"))

        assertNull(mark.value)
        assertEquals("06.06.26", mark.date)
        assertEquals("зачтено", mark.note)
    }

    @Test
    fun `an empty cell is no mark`() {
        assertNull(parseBarsMark(""))
        assertNull(parseBarsMark("    "))
    }

    @Test
    fun `number with a comma`() {
        assertEquals(12f, parseBarsNumber("12,0"))
        assertEquals(6f, parseBarsNumber(" 6 "))
        assertNull(parseBarsNumber("н/д"))
        assertNull(parseBarsNumber(""))
    }

    @Test
    fun `last brackets of a title`() {
        assertEquals("Электротехника" to "экзамен", splitTrailingBrackets("Электротехника (экзамен)"))
        assertEquals("Физика (часть 1)" to "зачёт с оценкой", splitTrailingBrackets("Физика (часть 1) (зачёт с оценкой)"))
        assertEquals("Химия" to null, splitTrailingBrackets("Химия"))
        assertEquals("(экзамен)" to null, splitTrailingBrackets("(экзамен)"))
    }

    @Test
    fun `week number`() {
        assertEquals(15, parseBarsWeekNumber("15 (07.12.26-13.12.26)"))
        assertEquals(4, parseBarsWeekNumber("4"))
        assertNull(parseBarsWeekNumber(""))
        assertNull(parseBarsWeekNumber(null))
    }

    @Test
    fun `header of a discipline`() {
        val header = parseDisciplineHeader("Сети и системы передачи информации, Баронов О.Р., экзамен, 1ЗЕ")

        assertEquals("Сети и системы передачи информации", header.name)
        assertEquals("Баронов О.Р.", header.person)
        assertEquals("экзамен", header.assessmentType)
        assertEquals("1ЗЕ", header.units)
        assertNull(header.note)
    }

    @Test
    fun `header with a comma in the name and a supervisor in brackets`() {
        val header = parseDisciplineHeader(
            "Практика, учебная, Власенко М.Н. (руководитель - Власенко М.Н.), зачёт с оценкой, 2 ЗЕ частичная переаттестация"
        )

        assertEquals("Практика, учебная", header.name)
        assertEquals("Власенко М.Н. (руководитель - Власенко М.Н.)", header.person)
        assertEquals("зачёт с оценкой", header.assessmentType)
        assertEquals("2ЗЕ", header.units)
        assertEquals("частичная переаттестация", header.note)
    }

    @Test
    fun `header without units`() {
        val header = parseDisciplineHeader("Химия, Орлов О.О., экзамен")

        assertEquals("Химия", header.name)
        assertEquals("Орлов О.О.", header.person)
        assertEquals("экзамен", header.assessmentType)
        assertEquals("", header.units)
    }

    @Test
    fun `short headers do not fail`() {
        assertEquals("Химия", parseDisciplineHeader("Химия").name)
        assertEquals("экзамен", parseDisciplineHeader("Химия, экзамен").assessmentType)
        assertEquals("", parseDisciplineHeader("").name)
    }
}
