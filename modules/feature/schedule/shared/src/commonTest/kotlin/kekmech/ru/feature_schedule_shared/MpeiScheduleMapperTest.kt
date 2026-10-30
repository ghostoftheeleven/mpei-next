package kekmech.ru.feature_schedule_shared

import kekmech.ru.lib_mpei_api.MpeiClassesDto
import kekmech.ru.lib_mpei_api.MpeiSearchResultDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MpeiScheduleMapperTest {

    private val monday = LocalDate(2026, 10, 26)

    private fun dto(
        date: String = "2026.10.27",
        begin: String = "09:20",
        end: String = "10:55",
        kind: String? = "Лекция",
        lecturer: String? = "Иванов И.И.",
        stream: String? = null,
        group: String? = "А-01-22",
        lessonNumber: Int? = null,
    ) = MpeiClassesDto(
        auditorium = "Е-101",
        beginLesson = begin,
        endLesson = end,
        date = date,
        discipline = "Физика",
        kindOfWork = kind,
        lecturer = lecturer,
        group = group,
        stream = stream,
        lessonNumberStart = lessonNumber,
    )

    private fun map(vararg items: MpeiClassesDto) =
        MpeiScheduleMapper.mapWeek("А-01-22", 9, ScheduleType.GROUP, monday, items.toList())

    @Test
    fun `search result of a teacher and of a group`() {
        val person = MpeiScheduleMapper.mapSearchResult(MpeiSearchResultDto(5, "Иванов И.И.", null, "person"))
        assertEquals(SearchResult("5", "Иванов И.И.", "", ScheduleType.PERSON), person)
        val group = MpeiScheduleMapper.mapSearchResult(MpeiSearchResultDto(9, "А-01-22", "Группа", "group"))
        assertEquals(ScheduleType.GROUP, group.type)
        assertEquals("Группа", group.description)
    }

    @Test
    fun `a week has seven days from Monday and the days without classes are empty`() {
        val week = map(dto()).weeks.single()
        assertEquals(7, week.days.size)
        assertEquals(monday, week.firstDayOfWeek)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), week.days.map { it.dayOfWeek })
        assertEquals(1, week.days[1].classes.size)
        assertTrue(week.days.filterIndexed { index, _ -> index != 1 }.all { it.classes.isEmpty() })
    }

    @Test
    fun `classes of a day are sorted by the start`() {
        val day = map(dto(begin = "12:50", end = "14:25"), dto(begin = "09:20", end = "10:55")).weeks.single().days[1]
        assertEquals(listOf(LocalTime(9, 20), LocalTime(12, 50)), day.classes.map { it.time.start })
    }

    @Test
    fun `a class with a date that is not a date is skipped`() {
        assertTrue(map(dto(date = "завтра")).weeks.single().days.all { it.classes.isEmpty() })
    }

    @Test
    fun `fields of a class`() {
        val classes = map(dto(stream = "Поток 3")).weeks.single().days[1].classes.single()
        assertEquals("Физика", classes.name)
        assertEquals(ClassesType.LECTURE, classes.type)
        assertEquals("Лекция", classes.rawType)
        assertEquals("Е-101", classes.place)
        assertEquals("Поток 3", classes.groups)
        assertEquals("Иванов И.И.", classes.person)
        assertEquals(1, classes.number)
    }

    @Test
    fun `groups come from the group when there is no stream`() {
        assertEquals("А-01-22", map(dto()).weeks.single().days[1].classes.single().groups)
    }

    @Test
    fun `a vacancy is not a teacher`() {
        assertEquals("", map(dto(lecturer = "Вакансия")).weeks.single().days[1].classes.single().person)
    }

    @Test
    fun `the number of the class comes from the API or from the time`() {
        assertEquals(5, map(dto(lessonNumber = 5)).weeks.single().days[1].classes.single().number)
        assertEquals(3, map(dto(begin = "13:45", end = "15:20")).weeks.single().days[1].classes.single().number)
    }

    @Test
    fun `a time that cannot be read becomes the first class`() {
        val time = map(dto(begin = "?", end = "?")).weeks.single().days[1].classes.single().time
        assertEquals(Time(LocalTime(9, 20), LocalTime(10, 55)), time)
    }

    @Test
    fun `type of classes by the words`() {
        assertEquals(ClassesType.LECTURE, MpeiScheduleMapper.mapClassesType("Лекция"))
        assertEquals(ClassesType.LAB, MpeiScheduleMapper.mapClassesType("Лабораторная работа"))
        assertEquals(ClassesType.PRACTICE, MpeiScheduleMapper.mapClassesType("Практика"))
        assertEquals(ClassesType.COURSE, MpeiScheduleMapper.mapClassesType("Курсовой проект"))
        assertEquals(ClassesType.EXAM, MpeiScheduleMapper.mapClassesType("Экзамен"))
        assertEquals(ClassesType.CONSULTATION, MpeiScheduleMapper.mapClassesType("Консультация"))
        assertEquals(ClassesType.UNDEFINED, MpeiScheduleMapper.mapClassesType(""))
    }

    @Test
    fun `lesson numbers at the borders of the breaks`() {
        assertEquals(-1, MpeiScheduleMapper.lessonNumber(LocalTime(8, 29)))
        assertEquals(1, MpeiScheduleMapper.lessonNumber(LocalTime(8, 30)))
        assertEquals(1, MpeiScheduleMapper.lessonNumber(LocalTime(10, 15)))
        assertEquals(2, MpeiScheduleMapper.lessonNumber(LocalTime(10, 16)))
        assertEquals(7, MpeiScheduleMapper.lessonNumber(LocalTime(21, 30)))
        assertEquals(-1, MpeiScheduleMapper.lessonNumber(LocalTime(21, 31)))
    }

    @Test
    fun `week of the semester in autumn`() {
        // 1 September 2026 is a Tuesday: the semester starts on the week of Monday 31 August. The rules of the Android
        // app (moved as they are) take a Monday in August for the spring branch, so that first week is not counted
        assertEquals(-1, MpeiScheduleMapper.weekOfSemester(LocalDate(2026, 8, 31)))
        assertEquals(2, MpeiScheduleMapper.weekOfSemester(LocalDate(2026, 9, 7)))
        assertEquals(9, MpeiScheduleMapper.weekOfSemester(LocalDate(2026, 10, 26)))
        assertEquals(18, MpeiScheduleMapper.weekOfSemester(LocalDate(2026, 12, 28)))
        // the weeks after the 18th are the session
        assertEquals(-1, MpeiScheduleMapper.weekOfSemester(LocalDate(2027, 1, 4)))
    }

    @Test
    fun `week of the semester when 1 September is a Sunday`() {
        // 1 September 2024 is a Sunday: the semester starts on Monday 2 September
        assertEquals(1, MpeiScheduleMapper.weekOfSemester(LocalDate(2024, 9, 2)))
    }

    @Test
    fun `week of the semester in spring`() {
        // 1 February 2026 is a Sunday: the week of Monday 26 January is the first one, but it is in January and the rules
        // of the Android app count January as the autumn semester, so the first week that is counted is 2 February
        assertEquals(2, MpeiScheduleMapper.weekOfSemester(LocalDate(2026, 2, 2)))
        assertEquals(6, MpeiScheduleMapper.weekOfSemester(LocalDate(2026, 3, 2)))
    }

    @Test
    fun `week outside of the semester is not studying`() {
        val week = MpeiScheduleMapper.mapWeek("x", 1, ScheduleType.GROUP, LocalDate(2026, 7, 6), emptyList()).weeks.single()
        assertEquals(WeekOfSemester.NonStudying, week.weekOfSemester)
        val studying = map().weeks.single()
        assertEquals(WeekOfSemester.Studying(9), studying.weekOfSemester)
    }

    @Test
    fun `week of the year starts on Monday and the first week has four days of the year`() {
        assertEquals(44, MpeiScheduleMapper.weekOfYear(LocalDate(2026, 10, 26)))
        // 1 January 2026 is a Thursday: the days of 29 December to 4 January are the first week of 2026
        assertEquals(1, MpeiScheduleMapper.weekOfYear(LocalDate(2026, 1, 1)))
        // 1 January 2027 is a Friday: three days of the year, they belong to week 0
        assertEquals(0, MpeiScheduleMapper.weekOfYear(LocalDate(2027, 1, 1)))
        assertEquals(1, MpeiScheduleMapper.weekOfYear(LocalDate(2027, 1, 4)))
    }
}
