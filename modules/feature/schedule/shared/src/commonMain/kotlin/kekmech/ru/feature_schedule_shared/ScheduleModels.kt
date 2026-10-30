package kekmech.ru.feature_schedule_shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

public enum class ScheduleType(public val pathName: String) {
    GROUP("group"),
    PERSON("person"),
}

public data class SearchResult(
    val id: String,
    val name: String,
    val description: String,
    val type: ScheduleType,
)

public data class Schedule(
    val name: String,
    val id: String,
    val type: ScheduleType,
    val weeks: List<Week>,
)

public data class Week(
    val weekOfYear: Int,
    val weekOfSemester: WeekOfSemester,
    val firstDayOfWeek: LocalDate,
    val days: List<Day>,
)

public sealed interface WeekOfSemester {

    /** A week of the holidays or of the session. */
    public data object NonStudying : WeekOfSemester

    public data class Studying(val num: Int) : WeekOfSemester
}

public data class Day(
    /** 1 is Monday, 7 is Sunday. */
    val dayOfWeek: Int,
    val date: LocalDate,
    val classes: List<Classes>,
)

public data class Classes(
    val name: String,
    val type: ClassesType,
    val rawType: String?,
    val place: String,
    val groups: String,
    val person: String,
    val time: Time,
    /** The number of the class in the day (1 to 7), or -1 when the time does not fit any class. */
    val number: Int,
)

public data class Time(
    val start: LocalTime,
    val end: LocalTime,
)

public enum class ClassesType {
    UNDEFINED,
    LECTURE,
    PRACTICE,
    LAB,
    COURSE,
    EXAM,
    CONSULTATION,
}
