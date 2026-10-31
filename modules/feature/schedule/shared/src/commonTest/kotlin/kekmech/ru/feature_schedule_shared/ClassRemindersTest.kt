@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_shared

import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class ClassRemindersTest {

    private val moscow = TimeZone.of("Europe/Moscow")
    private val ekaterinburg = TimeZone.of("Asia/Yekaterinburg")

    private fun lesson(name: String, type: ClassesType, start: LocalTime, end: LocalTime, place: String = "") = Classes(
        name = name, type = type, rawType = null, place = place, groups = "", person = "", time = Time(start, end), number = 1,
    )

    private fun schedule(vararg days: Pair<LocalDate, List<Classes>>) = Schedule(
        name = "А-01-22",
        id = "9",
        type = ScheduleType.GROUP,
        weeks = listOf(
            Week(
                weekOfYear = 44,
                weekOfSemester = WeekOfSemester.Studying(9),
                firstDayOfWeek = LocalDate(2026, 10, 26),
                days = days.map { (date, classes) -> Day(date.dayOfWeek.ordinal + 1, date, classes) },
            ),
        ),
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0, zone: TimeZone = moscow): Instant =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone)

    private val all = ReminderKind.entries.toSet()

    @Test
    fun `only practices and laboratory works are reminded about`() {
        val monday = LocalDate(2026, 11, 2)
        val plan = ClassReminderPlanner.plan(
            schedules = listOf(
                schedule(
                    monday to listOf(
                        lesson("Лекция", ClassesType.LECTURE, LocalTime(13, 45), LocalTime(15, 20)),
                        lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20)),
                        lesson("Лаба", ClassesType.LAB, LocalTime(15, 35), LocalTime(17, 10)),
                    ),
                ),
            ),
            now = at(2026, 10, 30, 12),
            kinds = all,
            quietZone = moscow,
        )

        assertEquals(setOf("Практика", "Лаба"), plan.map { it.name }.toSet())
        assertEquals(4, plan.size)
    }

    @Test
    fun `a reminder comes 24 and 5 hours before the start`() {
        val plan = ClassReminderPlanner.plan(
            schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20), "Е-101")))),
            now = at(2026, 10, 30, 12),
            kinds = all,
            quietZone = moscow,
        )

        assertEquals(listOf(at(2026, 11, 2, 13, 45), at(2026, 11, 3, 8, 45)), plan.map { it.triggerAt })
        assertEquals(listOf(ReminderKind.DAY_BEFORE, ReminderKind.HOURS_BEFORE), plan.map { it.kind })
    }

    @Test
    fun `only the chosen kinds are planned and none means nothing`() {
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20)))))

        val five = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), setOf(ReminderKind.HOURS_BEFORE), moscow, moscow)
        val none = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), emptySet(), moscow, moscow)

        assertEquals(listOf(ReminderKind.HOURS_BEFORE), five.map { it.kind })
        assertEquals(emptyList(), none)
    }

    @Test
    fun `a reminder in the past is skipped`() {
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20)))))

        // 10:00 on the day of the class: the 24 hours reminder (13:45 the day before) is over, the 5 hours one (08:45) too
        val plan = ClassReminderPlanner.plan(schedules, at(2026, 11, 3, 10), all, moscow, moscow)

        assertEquals(emptyList(), plan)
    }

    @Test
    fun `a night reminder is moved to seven in the morning`() {
        // a class at 08:30: the 5 hours reminder would be at 03:30, so it is moved to 07:00; the 24 hours one is at 08:30 the day before
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Лаба", ClassesType.LAB, LocalTime(8, 30), LocalTime(10, 5)))))

        val plan = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), all, moscow, moscow)

        assertEquals(at(2026, 11, 3, 7), plan.single { it.kind == ReminderKind.HOURS_BEFORE }.triggerAt)
        assertEquals(at(2026, 11, 2, 8, 30), plan.single { it.kind == ReminderKind.DAY_BEFORE }.triggerAt)
    }

    @Test
    fun `a late evening reminder goes to the next morning`() {
        // a class at 04:00 does not exist, so the 24 hours reminder of a 23:30 class is at 23:30 the day before: moved to 07:00 of the class day
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Лаба", ClassesType.LAB, LocalTime(23, 30), LocalTime(23, 59)))))

        val plan = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), setOf(ReminderKind.DAY_BEFORE), moscow, moscow)

        assertEquals(at(2026, 11, 3, 7), plan.single().triggerAt)
    }

    @Test
    fun `a reminder that leaves less than fifteen minutes is dropped`() {
        // the class is at 07:10: the 5 hours reminder (02:10) moves to 07:00, 10 minutes before the class, so it is dropped
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Лаба", ClassesType.LAB, LocalTime(7, 10), LocalTime(8, 45)))))

        val plan = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), setOf(ReminderKind.HOURS_BEFORE), moscow, moscow)

        assertEquals(emptyList(), plan)
    }

    @Test
    fun `the night is by the clock of the student and the class by the clock of the university`() {
        // the class is at 13:45 in Moscow (15:45 in Yekaterinburg); the 5 hours reminder is at 08:45 in Moscow = 10:45 in Yekaterinburg
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20)))))

        val plan = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), setOf(ReminderKind.HOURS_BEFORE), moscow, ekaterinburg)

        assertEquals(at(2026, 11, 3, 8, 45), plan.single().triggerAt)
    }

    @Test
    fun `the same class in two weeks is planned once and the ids are stable`() {
        val day = LocalDate(2026, 11, 3) to listOf(lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20)))
        val first = ClassReminderPlanner.plan(listOf(schedule(day), schedule(day)), at(2026, 10, 30, 12), all, moscow, moscow)
        val second = ClassReminderPlanner.plan(listOf(schedule(day)), at(2026, 10, 30, 13), all, moscow, moscow)

        assertEquals(2, first.size)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertNotEquals(first[0].id, first[1].id)
        assertTrue(first.all { it.id >= 0 })
    }

    @Test
    fun `the text of a reminder says what and when`() {
        val schedules = listOf(schedule(LocalDate(2026, 11, 3) to listOf(lesson("Физика", ClassesType.LAB, LocalTime(9, 20), LocalTime(10, 55), "Е-101"))))

        val plan = ClassReminderPlanner.plan(schedules, at(2026, 10, 30, 12), all, moscow, moscow)

        assertEquals("Лабораторная — завтра", plan[0].title)
        assertEquals("Лабораторная — через 5 часов", plan[1].title)
        assertEquals("Физика · 09:20–10:55 · Е-101", plan[0].body)
    }

    // the controller

    private class FakeScheduler(var granted: Boolean) : ReminderScheduler {
        var planned: List<ClassReminder> = emptyList()
        var cancelled = 0
        var asked = 0

        override suspend fun requestPermission(): Boolean {
            asked++
            return granted
        }

        override suspend fun isPermissionGranted(): Boolean = granted
        override fun replaceAll(reminders: List<ClassReminder>) {
            planned = reminders
        }

        override fun cancelAll() {
            cancelled++
            planned = emptyList()
        }
    }

    private val now = Instant.parse("2026-10-30T09:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private val chosen = SelectedSchedule(ScheduleType.GROUP, "А-01-22")
    private val oneWeek = schedule(LocalDate(2026, 11, 3) to listOf(lesson("Практика", ClassesType.PRACTICE, LocalTime(13, 45), LocalTime(15, 20))))

    private fun controller(
        scheduler: FakeScheduler,
        enabledByDefault: Boolean = false,
        scope: kotlinx.coroutines.test.TestScope,
        load: suspend (SelectedSchedule, Int) -> Schedule? = { _, offset -> if (offset == 0) oneWeek else null },
    ) = RemindersController(
        settings = ClassReminderSettings(InMemoryKeyValueStore(), enabledByDefault),
        changeSettings = ScheduleChangeSettings(InMemoryKeyValueStore()),
        scheduler = scheduler,
        loadWeek = load,
        scope = scope.backgroundScope,
        clock = clock,
    )

    @Test
    fun `the reminders are off until the student switches them on`() = runTest {
        val scheduler = FakeScheduler(granted = true)
        val controller = controller(scheduler, scope = this)

        controller.sync(chosen)
        runCurrent()

        assertEquals(false, controller.state.value.enabled)
        assertEquals(emptyList(), scheduler.planned)
        assertEquals(0, scheduler.asked)
    }

    @Test
    fun `switching on asks for the permission and plans the reminders`() = runTest {
        val scheduler = FakeScheduler(granted = true)
        val controller = controller(scheduler, scope = this)
        controller.sync(chosen)

        controller.setEnabled(true)
        runCurrent()

        assertEquals(1, scheduler.asked)
        assertEquals(true, controller.state.value.enabled)
        assertEquals(2, scheduler.planned.size)
        assertEquals(2, controller.state.value.planned)
    }

    @Test
    fun `without the permission the switch goes back and the hint is shown`() = runTest {
        val scheduler = FakeScheduler(granted = false)
        val controller = controller(scheduler, scope = this)
        controller.sync(chosen)

        controller.setEnabled(true)
        runCurrent()

        assertEquals(false, controller.state.value.enabled)
        assertEquals(true, controller.state.value.permissionDenied)
        assertEquals(emptyList(), scheduler.planned)
    }

    @Test
    fun `switching a kind off plans only the other one and switching all off cancels`() = runTest {
        val scheduler = FakeScheduler(granted = true)
        val controller = controller(scheduler, scope = this)
        controller.sync(chosen)
        controller.setEnabled(true)
        runCurrent()

        controller.setKindEnabled(ReminderKind.DAY_BEFORE, false)
        runCurrent()
        assertEquals(listOf(ReminderKind.HOURS_BEFORE), scheduler.planned.map { it.kind })

        controller.setEnabled(false)
        runCurrent()
        assertEquals(emptyList(), scheduler.planned)
        assertEquals(false, controller.state.value.enabled)
    }

    @Test
    fun `a change of the schedule plans again and no schedule cancels`() = runTest {
        val scheduler = FakeScheduler(granted = true)
        val controller = controller(scheduler, enabledByDefault = true, scope = this)

        controller.sync(chosen)
        runCurrent()
        assertEquals(2, scheduler.planned.size)

        controller.sync(null)
        runCurrent()
        assertEquals(emptyList(), scheduler.planned)
    }

    @Test
    fun `when no week could be loaded the planned reminders stay`() = runTest {
        val scheduler = FakeScheduler(granted = true)
        var online = true
        val controller = controller(scheduler, enabledByDefault = true, scope = this) { _, offset ->
            if (online && offset == 0) oneWeek else null
        }
        controller.sync(chosen)
        runCurrent()
        assertEquals(2, scheduler.planned.size)

        online = false
        controller.sync(chosen)
        runCurrent()

        assertEquals(2, scheduler.planned.size)
    }

    @Test
    fun `the permission that was taken away later is noticed`() = runTest {
        val scheduler = FakeScheduler(granted = true)
        val controller = controller(scheduler, enabledByDefault = true, scope = this)
        controller.sync(chosen)
        runCurrent()

        scheduler.granted = false
        controller.sync(chosen)
        runCurrent()

        assertEquals(true, controller.state.value.permissionDenied)
        assertEquals(emptyList(), scheduler.planned)
    }
}
