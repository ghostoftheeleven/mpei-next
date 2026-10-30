package kekmech.ru.ext_datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExtDatetimeTest {

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    @Test
    fun `moscow time is UTC plus three hours`() {
        val clock = FixedClock(Instant.parse("2026-10-30T21:30:00Z"))
        assertEquals(LocalDateTime(2026, 10, 31, 0, 30), moscowDateTime(clock))
        assertEquals(LocalDate(2026, 10, 31), moscowDate(clock))
        assertEquals(LocalTime(0, 30), moscowTime(clock))
    }

    @Test
    fun `startOfWeek gives the Monday for every day of the week`() {
        val monday = LocalDate(2026, 10, 26)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        for (offset in 0..6) {
            assertEquals(monday, monday.plus(offset, DateTimeUnit.DAY).startOfWeek())
        }
    }

    @Test
    fun `startOfWeek crosses the end of the month and the year`() {
        assertEquals(LocalDate(2026, 10, 26), LocalDate(2026, 11, 1).startOfWeek())
        assertEquals(LocalDate(2025, 12, 29), LocalDate(2026, 1, 1).startOfWeek())
    }

    @Test
    fun `daysTo and weeksTo count whole units in both directions`() {
        val a = LocalDate(2026, 10, 1)
        assertEquals(30, a.daysTo(LocalDate(2026, 10, 31)))
        assertEquals(-3, LocalDate(2026, 10, 4).daysTo(a))
        assertEquals(4, a.weeksTo(LocalDate(2026, 10, 31)))
        assertEquals(-1, LocalDate(2026, 10, 10).weeksTo(a))
        assertEquals(0, a.weeksTo(LocalDate(2026, 10, 7)))
    }

    @Test
    fun `secondsTo is the difference within a day`() {
        assertEquals(5400, LocalTime(9, 20).secondsTo(LocalTime(10, 50)))
        assertEquals(-60, LocalTime(10, 1).secondsTo(LocalTime(10, 0)))
    }

    @Test
    fun `formatHm pads hours and minutes`() {
        assertEquals("09:05", LocalTime(9, 5).formatHm())
        assertEquals("00:00", LocalTime(0, 0).formatHm())
        assertEquals("23:59", LocalTime(23, 59, 30).formatHm())
    }
}
