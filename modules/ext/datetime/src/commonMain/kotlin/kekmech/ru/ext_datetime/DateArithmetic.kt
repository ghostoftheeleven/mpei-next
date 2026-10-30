package kekmech.ru.ext_datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.until

/** The Monday of the week of this date (the schedule weeks start on Monday). */
public fun LocalDate.startOfWeek(): LocalDate =
    minus(dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber, DateTimeUnit.DAY)

/** Whole days from this date to [other], negative if [other] is earlier (ChronoUnit.DAYS.between). */
public fun LocalDate.daysTo(other: LocalDate): Int = daysUntil(other)

/** Whole weeks from this date to [other], the part of a week is dropped (ChronoUnit.WEEKS.between). */
public fun LocalDate.weeksTo(other: LocalDate): Int = until(other, DateTimeUnit.WEEK)

/** Seconds from this time to [other] within one day, negative if [other] is earlier. */
public fun LocalTime.secondsTo(other: LocalTime): Int = other.toSecondOfDay() - toSecondOfDay()
