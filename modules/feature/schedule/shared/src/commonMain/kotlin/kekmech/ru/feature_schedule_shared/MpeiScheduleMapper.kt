package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.startOfWeek
import kekmech.ru.ext_datetime.weeksTo
import kekmech.ru.lib_mpei_api.MpeiClassesDto
import kekmech.ru.lib_mpei_api.MpeiSearchResultDto
import kekmech.ru.lib_mpei_api.parseMpeiDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The answers of the MPEI API to the models of the app. The rules are those of the Android app, moved as they are. */
internal object MpeiScheduleMapper {

    fun mapSearchResult(result: MpeiSearchResultDto): SearchResult =
        SearchResult(
            id = result.id.toString(),
            name = result.label,
            description = result.description.orEmpty(),
            type = if (result.type.equals("person", ignoreCase = true)) ScheduleType.PERSON else ScheduleType.GROUP,
        )

    /** The week that starts on [monday] (Monday to Sunday); the days without classes are there, empty. */
    fun mapWeek(
        name: String,
        id: Long,
        type: ScheduleType,
        monday: LocalDate,
        classesList: List<MpeiClassesDto>,
    ): Schedule {
        val classesByDate = mutableMapOf<LocalDate, MutableList<Classes>>()
        for (item in classesList) {
            val date = parseMpeiDate(item.date) ?: continue
            val time = parseTime(item.beginLesson, item.endLesson)
            val isVacancy = item.lecturer?.contains("вакансия", ignoreCase = true) == true
            val classes = Classes(
                name = item.discipline,
                type = mapClassesType(item.kindOfWork.orEmpty()),
                rawType = item.kindOfWork,
                place = item.auditorium.orEmpty(),
                groups = item.stream ?: item.group ?: item.subGroup.orEmpty(),
                person = if (isVacancy) "" else item.lecturer?.trim().orEmpty(),
                time = time,
                number = item.lessonNumberStart ?: lessonNumber(time.start),
            )
            classesByDate.getOrPut(date) { mutableListOf() }.add(classes)
        }

        val days = (0 until DAYS_IN_WEEK).map { offset ->
            val date = monday.plus(offset, DateTimeUnit.DAY)
            Day(
                dayOfWeek = date.dayOfWeek.isoDayNumber,
                date = date,
                classes = classesByDate[date].orEmpty().sortedBy { it.time.start },
            )
        }
        val weekOfSemester = weekOfSemester(monday)
        return Schedule(
            name = name,
            id = id.toString(),
            type = type,
            weeks = listOf(
                Week(
                    weekOfYear = weekOfYear(monday),
                    weekOfSemester = if (weekOfSemester < 0) WeekOfSemester.NonStudying else WeekOfSemester.Studying(weekOfSemester),
                    firstDayOfWeek = monday,
                    days = days,
                ),
            ),
        )
    }

    internal fun mapClassesType(rawType: String): ClassesType {
        val lower = rawType.lowercase()
        return when {
            lower.contains("лек") -> ClassesType.LECTURE
            lower.contains("лаб") -> ClassesType.LAB
            lower.contains("прак") -> ClassesType.PRACTICE
            lower.contains("курс") || lower.contains("кп") -> ClassesType.COURSE
            lower.contains("экз") -> ClassesType.EXAM
            lower.contains("конс") -> ClassesType.CONSULTATION
            else -> ClassesType.UNDEFINED
        }
    }

    private fun parseTime(begin: String, end: String): Time =
        Time(
            start = runCatching { LocalTime.parse(begin) }.getOrElse { LocalTime(DEFAULT_START_HOUR, DEFAULT_START_MINUTE) },
            end = runCatching { LocalTime.parse(end) }.getOrElse { LocalTime(DEFAULT_END_HOUR, DEFAULT_END_MINUTE) },
        )

    /** The number of the class by its start; the borders are the breaks between the classes of MPEI. */
    internal fun lessonNumber(start: LocalTime): Int {
        val minutes = start.hour * MINUTES_IN_HOUR + start.minute
        return LESSON_STARTS.indexOfFirst { minutes in it }.let { if (it < 0) -1 else it + 1 }
    }

    /**
     * The number of the week of the semester (1 to 18), or -1 for the weeks outside of it. The autumn semester
     * starts on the week of 1 September (the next Monday if it is Sunday), the spring one on the week of 1 February.
     */
    internal fun weekOfSemester(monday: LocalDate): Int {
        val month = monday.monthNumber
        val firstMonday = if (month in AUTUMN_FIRST_MONTH..DECEMBER || month == JANUARY) {
            val year = if (month == JANUARY) monday.year - 1 else monday.year
            val september1 = LocalDate(year, SEPTEMBER, 1)
            if (september1.dayOfWeek == DayOfWeek.SUNDAY) september1.plus(1, DateTimeUnit.DAY) else september1.startOfWeek()
        } else {
            LocalDate(monday.year, FEBRUARY, 1).startOfWeek()
        }
        val number = 1 + firstMonday.weeksTo(monday)
        return if (number in 1..LAST_STUDY_WEEK) number else -1
    }

    /**
     * The week of the year as `WeekFields.of(Locale("ru"))` gives it: weeks start on Monday, the first week is the one
     * with at least four days in the year, the days before it are in week 0.
     */
    internal fun weekOfYear(date: LocalDate): Int {
        val january4 = LocalDate(date.year, JANUARY, MIN_DAYS_IN_FIRST_WEEK)
        val firstWeekStart = january4.startOfWeek()
        if (date < firstWeekStart) return 0
        return firstWeekStart.weeksTo(date.startOfWeek()) + 1
    }

    private const val DAYS_IN_WEEK = 7
    private const val MINUTES_IN_HOUR = 60
    private const val DEFAULT_START_HOUR = 9
    private const val DEFAULT_START_MINUTE = 20
    private const val DEFAULT_END_HOUR = 10
    private const val DEFAULT_END_MINUTE = 55
    private const val JANUARY = 1
    private const val FEBRUARY = 2
    private const val SEPTEMBER = 9
    private const val DECEMBER = 12
    private const val AUTUMN_FIRST_MONTH = 9
    private const val LAST_STUDY_WEEK = 18
    private const val MIN_DAYS_IN_FIRST_WEEK = 4

    // 8:30-10:15, 10:16-12:45, 12:46-14:40, 14:41-16:30, 16:31-18:15, 18:16-19:45, 19:46-21:30
    private val LESSON_STARTS = listOf(
        510..615,
        616..765,
        766..880,
        881..990,
        991..1095,
        1096..1185,
        1186..1290,
    )
}
