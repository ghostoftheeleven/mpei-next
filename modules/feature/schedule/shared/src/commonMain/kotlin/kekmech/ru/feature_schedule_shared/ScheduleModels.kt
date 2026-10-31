package kekmech.ru.feature_schedule_shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
public enum class ScheduleType(public val pathName: String) {
    GROUP("group"),
    PERSON("person"),
}

@Serializable
public data class SearchResult(
    val id: String,
    val name: String,
    val description: String,
    val type: ScheduleType,
)

@Serializable
public data class Schedule(
    val name: String,
    val id: String,
    val type: ScheduleType,
    val weeks: List<Week>,
)

@Serializable
public data class Week(
    val weekOfYear: Int,
    val weekOfSemester: WeekOfSemester,
    val firstDayOfWeek: LocalDate,
    val days: List<Day>,
)

@Serializable
public sealed interface WeekOfSemester {

    /** A week of the holidays or of the session. */
    @Serializable
    public data object NonStudying : WeekOfSemester

    @Serializable
    public data class Studying(val num: Int) : WeekOfSemester
}

@Serializable
public data class Day(
    /** 1 is Monday, 7 is Sunday. */
    val dayOfWeek: Int,
    val date: LocalDate,
    val classes: List<Classes>,
)

@Serializable
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

@Serializable
public data class Time(
    val start: LocalTime,
    val end: LocalTime,
)

@Serializable
public enum class ClassesType {
    UNDEFINED,
    LECTURE,
    PRACTICE,
    LAB,
    COURSE,
    EXAM,
    CONSULTATION,
}
