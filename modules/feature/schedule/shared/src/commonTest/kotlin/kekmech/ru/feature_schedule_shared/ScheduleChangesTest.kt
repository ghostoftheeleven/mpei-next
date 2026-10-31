package kekmech.ru.feature_schedule_shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ScheduleChangesTest {

    private val date = LocalDate(2026, 11, 3)

    private fun lesson(
        number: Int = 1,
        name: String = "Математика",
        place: String = "Е-101",
        person: String = "Иванов И.И.",
        groups: String = "А-01-22",
        start: LocalTime = LocalTime(9, 20),
        type: ClassesType = ClassesType.LECTURE,
    ) = Classes(
        name = name, type = type, rawType = null, place = place, groups = groups, person = person,
        time = Time(start, LocalTime.fromSecondOfDay(start.toSecondOfDay() + 95 * 60)),
        number = number,
    )

    private fun schedule(vararg classes: Classes) = Schedule(
        name = "А-01-22",
        id = "9",
        type = ScheduleType.GROUP,
        weeks = listOf(
            Week(
                weekOfYear = 45,
                weekOfSemester = WeekOfSemester.Studying(10),
                firstDayOfWeek = LocalDate(2026, 11, 2),
                days = listOf(Day(dayOfWeek = 2, date = date, classes = classes.toList())),
            ),
        ),
    )

    private fun changes(old: Schedule?, new: Schedule) = ScheduleChangeDetector.detectChanges(old, new)

    @Test
    fun `without an old version nothing is reported`() {
        assertEquals(emptyList(), changes(null, schedule(lesson())))
    }

    @Test
    fun `the same classes are not a change`() {
        assertEquals(emptyList(), changes(schedule(lesson()), schedule(lesson())))
    }

    @Test
    fun `a new room is a modification of the class`() {
        val result = changes(schedule(lesson(place = "Е-101")), schedule(lesson(place = "Е-205")))

        assertEquals(1, result.size)
        assertEquals(ScheduleChange.ChangeType.MODIFIED, result.single().type)
        assertEquals("Изменена пара Математика: аудитория (Е-101 → Е-205)", result.single().description)
    }

    @Test
    fun `another teacher and another time are both named`() {
        val result = changes(
            schedule(lesson(person = "Иванов И.И.", start = LocalTime(9, 20))),
            schedule(lesson(person = "Петров П.П.", start = LocalTime(11, 10))),
        )

        assertEquals("Изменена пара Математика: время (09:20 → 11:10), преподаватель", result.single().description)
    }

    @Test
    fun `another discipline for the same group is a replacement`() {
        val result = changes(schedule(lesson(name = "Физика")), schedule(lesson(name = "Математика")))

        assertEquals(ScheduleChange.ChangeType.REPLACED, result.single().type)
        assertEquals("Заменена пара: Физика → Математика", result.single().description)
    }

    @Test
    fun `a class that is gone is removed and a new one is added`() {
        val removed = changes(schedule(lesson()), schedule())
        val added = changes(schedule(), schedule(lesson()))

        assertEquals(ScheduleChange.ChangeType.REMOVED, removed.single().type)
        assertEquals("Отменена пара: Математика", removed.single().description)
        assertEquals(ScheduleChange.ChangeType.ADDED, added.single().type)
        assertEquals("Добавлена пара: Математика", added.single().description)
    }

    @Test
    fun `the classes of other slots are not mixed up`() {
        val old = schedule(lesson(number = 1, name = "Физика"), lesson(number = 2, name = "Химия", start = LocalTime(11, 10)))
        val new = schedule(lesson(number = 1, name = "Физика"), lesson(number = 2, name = "Химия", start = LocalTime(11, 10), place = "А-1"))

        val result = changes(old, new)

        assertEquals(1, result.size)
        assertEquals("Химия", result.single().newClass?.name)
    }

    @Test
    fun `two subgroups at the same time are matched by their groups`() {
        val old = schedule(
            lesson(name = "Лаба", groups = "А-01-22\\1п.", place = "М-1"),
            lesson(name = "Лаба", groups = "А-01-22\\2п.", place = "М-2"),
        )
        val new = schedule(
            lesson(name = "Лаба", groups = "А-01-22\\1п.", place = "М-1"),
            lesson(name = "Лаба", groups = "А-01-22\\2п.", place = "М-3"),
        )

        val result = changes(old, new)

        assertEquals(1, result.size)
        assertEquals("А-01-22\\2п.", result.single().newClass?.groups)
    }

    @Test
    fun `days and weeks that are not in the old version are not compared`() {
        val old = schedule(lesson())
        val new = Schedule(
            name = "А-01-22", id = "9", type = ScheduleType.GROUP,
            weeks = listOf(
                Week(46, WeekOfSemester.Studying(11), LocalDate(2026, 11, 9), listOf(Day(2, LocalDate(2026, 11, 10), listOf(lesson())))),
            ),
        )

        assertEquals(emptyList(), changes(old, new))
    }

    // the words

    @Test
    fun `the title depends on how many changes there are`() {
        assertEquals("Изменение в расписании", ScheduleChangeText.title(1))
        assertEquals("Изменения в расписании", ScheduleChangeText.title(4))
        assertEquals("Много изменений в расписании", ScheduleChangeText.title(5))
    }

    @Test
    fun `the days are today tomorrow or the date`() {
        val today = LocalDate(2026, 11, 3)

        assertEquals("Сегодня", ScheduleChangeText.formatDate(today, today))
        assertEquals("Завтра", ScheduleChangeText.formatDate(LocalDate(2026, 11, 4), today))
        assertEquals("5 ноября (чт)", ScheduleChangeText.formatDate(LocalDate(2026, 11, 5), today))
    }

    @Test
    fun `the preview says the only change in full and the count of many`() {
        val today = LocalDate(2026, 11, 3)
        val one = changes(schedule(lesson(place = "Е-1")), schedule(lesson(place = "Е-2")))
        val two = changes(schedule(lesson(name = "А"), lesson(number = 2, name = "Б")), schedule(lesson(name = "В"), lesson(number = 2, name = "Г")))

        assertEquals("Сегодня: Изменена пара Математика: аудитория (Е-1 → Е-2)", ScheduleChangeText.preview(one, today))
        assertEquals("Изменений в расписании: 2", ScheduleChangeText.preview(two, today))
    }

    @Test
    fun `the details are by days and end with the rest`() {
        val today = LocalDate(2026, 11, 3)
        val old = schedule(*(1..12).map { lesson(number = it, name = "Пара $it") }.toTypedArray())
        val new = schedule()

        val text = ScheduleChangeText.details(changes(old, new), today)

        assertTrue(text.startsWith("Сегодня:\n• Отменена пара: Пара 1"))
        assertTrue(text.endsWith("… и ещё 2"))
        assertEquals(10, text.lines().count { it.startsWith("• ") })
    }
}
