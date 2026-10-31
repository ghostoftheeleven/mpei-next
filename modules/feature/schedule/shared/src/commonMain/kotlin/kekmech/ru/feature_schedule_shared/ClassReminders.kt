@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_shared

import kekmech.ru.ext_datetime.MoscowTimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** When to remind about a class. [code] is a part of the stable id of a reminder: do not change it. */
public enum class ReminderKind(public val hoursBefore: Int, public val code: Int) {
    DAY_BEFORE(hoursBefore = 24, code = 1),
    HOURS_BEFORE(hoursBefore = 5, code = 2),
}

public data class ClassReminder(
    /** The same for the same class and kind in every plan: it lets the system replace or remove a reminder that was made. */
    val id: Int,
    val kind: ReminderKind,
    val triggerAt: Instant,
    /** The start and the end of the class in the time of the university (Moscow). */
    val start: LocalDateTime,
    val end: LocalTime,
    val name: String,
    val type: ClassesType,
    val place: String,
) {

    /** "Лабораторная — завтра" or "Практика — через 5 часов". */
    public val title: String
        get() {
            val typeName = if (type == ClassesType.LAB) "Лабораторная" else "Практика"
            return when (kind) {
                ReminderKind.DAY_BEFORE -> "$typeName — завтра"
                ReminderKind.HOURS_BEFORE -> "$typeName — через 5 часов"
            }
        }

    /** The name of the class, the time (the time of the university) and the place. */
    public val body: String
        get() = listOf(name, "${start.time.format()}–${end.format()}", place).filter { it.isNotBlank() }.joinToString(" · ")

    private fun LocalTime.format() =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/**
 * Builds the reminders for the practices and the laboratory works (the same rules as the Android app):
 * - the lectures are not reminded about;
 * - a reminder that is in the past is skipped;
 * - a reminder that would come at night (23:00 to 07:00 by the clock of the student) is moved to 07:00, unless that
 *   leaves less than 15 minutes before the class.
 */
public object ClassReminderPlanner {

    private val RemindedTypes = setOf(ClassesType.PRACTICE, ClassesType.LAB)
    private val QuietFrom = LocalTime(23, 0)
    private val QuietTo = LocalTime(7, 0)
    private val MinLead: Duration = 15.minutes

    /**
     * @param classZone the time zone of the times in the schedule: the university's
     * @param quietZone the time zone whose clock says what is night: the student's
     */
    public fun plan(
        schedules: List<Schedule>,
        now: Instant,
        kinds: Set<ReminderKind>,
        classZone: TimeZone = MoscowTimeZone,
        quietZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<ClassReminder> {
        if (kinds.isEmpty()) return emptyList()
        val reminders = mutableListOf<ClassReminder>()
        for (schedule in schedules) {
            for (week in schedule.weeks) {
                for (day in week.days) {
                    for (classes in day.classes) {
                        if (classes.type !in RemindedTypes) continue
                        val start = LocalDateTime(day.date, classes.time.start)
                        val startInstant = start.toInstant(classZone)
                        for (kind in kinds) {
                            val trigger = triggerTime(startInstant, kind, now, quietZone) ?: continue
                            reminders += ClassReminder(
                                id = stableId(day.date, classes.time.start, classes.name, kind),
                                kind = kind,
                                triggerAt = trigger,
                                start = start,
                                end = classes.time.end,
                                name = classes.name,
                                type = classes.type,
                                place = classes.place,
                            )
                        }
                    }
                }
            }
        }
        // the same class can be in several weeks that were loaded
        return reminders.distinctBy { it.id }.sortedBy { it.triggerAt }
    }

    private fun triggerTime(start: Instant, kind: ReminderKind, now: Instant, quietZone: TimeZone): Instant? {
        val trigger = shiftOutOfQuietHours(start - kind.hoursBefore.hours, quietZone)
        return when {
            trigger <= now -> null
            trigger + MinLead > start -> null
            else -> trigger
        }
    }

    internal fun shiftOutOfQuietHours(time: Instant, zone: TimeZone): Instant {
        val local = time.toLocalDateTime(zone)
        return when {
            local.time >= QuietFrom -> LocalDateTime(local.date.plus(1, DateTimeUnit.DAY), QuietTo).toInstant(zone)
            local.time < QuietTo -> LocalDateTime(local.date, QuietTo).toInstant(zone)
            else -> time
        }
    }

    private fun stableId(date: LocalDate, start: LocalTime, name: String, kind: ReminderKind): Int =
        "$date|$start|$name|${kind.code}".hashCode() and Int.MAX_VALUE
}
