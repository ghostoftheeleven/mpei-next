package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_parser.BarsActivity
import kekmech.ru.lib_bars_parser.BarsActivityType
import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.parseBarsMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarsFormatTest {

    @Test
    fun `a whole score has no fraction and a fraction has a comma`() {
        assertEquals("5", formatScore(5f))
        assertEquals("0", formatScore(0f))
        assertEquals("2,66", formatScore(2.66f))
        assertEquals("4,5", formatScore(4.5f))
    }

    @Test
    fun `marks as the screens show them`() {
        assertEquals("—", formatMark(null))
        assertEquals("4", formatMark(parseBarsMark("4  (06.06.26)")))
        assertEquals("2,66", formatMark(parseBarsMark("2,66")))
        assertEquals("5 · зачтено", formatMark(parseBarsMark("5/зачтено")))
        assertEquals("3 (НД)", formatMark(parseBarsMark("3 (НД, 15.06.26)")))
    }

    @Test
    fun `a mark without a number is shown as it was written`() {
        assertEquals("зачтено (06.06.26)", formatMark(parseBarsMark("зачтено (06.06.26)")))
    }

    @Test
    fun `the parts of a discipline`() {
        val discipline = BarsDiscipline(
            name = "Химия",
            person = "Орлов О.О.",
            assessmentType = "экзамен",
            units = "2ЗЕ",
            deadline = null,
            notes = emptyList(),
            activities = listOf(
                activity(BarsActivityType.CONTROL_ACTIVITY, "1. КМ", "4 (01.01.26)"),
                activity(BarsActivityType.CONTROL_ACTIVITY, "2. КМ", ""),
                activity(BarsActivityType.CURRENT_SCORE, "Балл текущего контроля:", "3,5"),
                activity(BarsActivityType.CONTROL_WEEK, "Контрольная неделя №1", ""),
                activity(BarsActivityType.FINAL_MARK, "Итоговая оценка:", "4"),
            ),
        )

        assertEquals(2, discipline.controlActivities.size)
        assertEquals("3,5", formatMark(discipline.currentScore))
        assertEquals("4", formatMark(discipline.finalMark))
        assertEquals(
            listOf(BarsActivityType.CURRENT_SCORE, BarsActivityType.FINAL_MARK),
            discipline.summaryActivities.map { it.type },
        )
    }

    @Test
    fun `a discipline without a final mark has none`() {
        val discipline = BarsDiscipline("Химия", "", "", "", null, emptyList(), emptyList())

        assertNull(discipline.finalMark)
        assertNull(discipline.currentScore)
    }

    private fun activity(type: BarsActivityType, name: String, mark: String) =
        BarsActivity(type, name, weight = null, weekNum = null, mark = parseBarsMark(mark), criteria = null)
}
