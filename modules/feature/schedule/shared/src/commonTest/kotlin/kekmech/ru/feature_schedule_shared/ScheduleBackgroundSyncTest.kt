@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_shared

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_mpei_api.MpeiScheduleApi
import kekmech.ru.lib_settings.InMemoryKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class ScheduleBackgroundSyncTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    // Friday 30 October 2026, 12:00 in Moscow: the current week starts on Monday 26 October
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-30T09:00:00Z")
    }
    private val selected = SelectedSchedule(ScheduleType.GROUP, "А-01-22")

    private val search = """[{"id":9,"label":"А-01-22","description":"Группа","type":"group"}]"""
    private var room = "Е-101"
    private var online = true
    private var withFutureLesson = false

    private fun classesAnswer(): String {
        fun lesson(date: String, name: String, room: String) =
            """{"beginLesson":"13:45","endLesson":"15:20","date":"$date","discipline":"$name","kindOfWork":"Практика","auditorium":"$room"}"""
        return "[" + listOf(
            lesson("2026.10.30", "Физика", room),
            lesson("2026.10.28", "Прошлое", room),
        ).plus(if (withFutureLesson) listOf(lesson("2026.11.03", "Практика", room)) else emptyList()).joinToString(",") + "]"
    }

    private class FakeNotifier : ScheduleChangeNotifier {
        val shown = mutableListOf<List<ScheduleChange>>()
        override fun notify(changes: List<ScheduleChange>, today: LocalDate) {
            shown += changes
        }
    }

    private class FakeScheduler(var granted: Boolean = true) : ReminderScheduler {
        var planned: List<ClassReminder>? = null
        override suspend fun requestPermission() = granted
        override suspend fun isPermissionGranted() = granted
        override fun replaceAll(reminders: List<ClassReminder>) {
            planned = reminders
        }

        override fun cancelAll() {
            planned = emptyList()
        }
    }

    private val store = InMemoryKeyValueStore()
    private val notifier = FakeNotifier()
    private val scheduler = FakeScheduler()
    private val cache = ScheduleCache(store)
    private val changeSettings = ScheduleChangeSettings(store)
    private val reminderSettings = ClassReminderSettings(store)

    private fun sync(): ScheduleBackgroundSync {
        val engine = MockEngine { request ->
            when {
                !online -> respond("", HttpStatusCode.ServiceUnavailable)
                request.url.encodedPath.endsWith("/search") -> respond(search, HttpStatusCode.OK, jsonHeaders)
                else -> respond(classesAnswer(), HttpStatusCode.OK, jsonHeaders)
            }
        }
        val api = MpeiScheduleApi(
            client = createHttpClient(ClientIdentity("test/1.0", "device", "iOS", "ru", "0.5.2"), engine),
            baseUrl = { "https://ts.mpei.ru/api/" },
        )
        val checker = ScheduleChangeChecker(ScheduleService(api, clock), ScheduleSnapshots(store), cache, clock)
        return ScheduleBackgroundSync(cache, checker, changeSettings, notifier, reminderSettings, scheduler, clock)
    }

    @Test
    fun `nothing is done when no schedule is chosen or both switches are off`() = runTest {
        assertTrue(sync().run())

        cache.saveSelected(selected)
        assertTrue(sync().run())

        assertEquals(emptyList(), notifier.shown)
        assertEquals(null, scheduler.planned)
    }

    @Test
    fun `the first look has nothing to compare with and the second one finds the new room`() = runTest {
        cache.saveSelected(selected)
        changeSettings.enabled = true

        assertTrue(sync().run())
        assertEquals(emptyList(), notifier.shown)

        room = "Е-205"
        assertTrue(sync().run())

        val changes = notifier.shown.single()
        assertEquals(1, changes.size)
        assertEquals("Изменена пара Физика: аудитория (Е-101 → Е-205)", changes.single().description)
    }

    @Test
    fun `a change in a day that has passed is not told`() = runTest {
        cache.saveSelected(selected)
        changeSettings.enabled = true
        sync().run()

        room = "Е-205"
        sync().run()

        // the class of the 28th was changed too, but it is over: only the class of the 30th is told about
        assertEquals(listOf(LocalDate(2026, 10, 30)), notifier.shown.single().map { it.date }.distinct())
    }

    @Test
    fun `the same change is not told twice`() = runTest {
        cache.saveSelected(selected)
        changeSettings.enabled = true
        sync().run()
        room = "Е-205"
        sync().run()

        sync().run()

        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `without the network it asks to try again and tells nothing`() = runTest {
        cache.saveSelected(selected)
        changeSettings.enabled = true
        online = false

        assertFalse(sync().run())
        assertEquals(emptyList(), notifier.shown)
    }

    @Test
    fun `the reminders are planned by the fresh schedule`() = runTest {
        cache.saveSelected(selected)
        reminderSettings.enabled = true
        withFutureLesson = true

        assertTrue(sync().run())

        // the practice of the 3rd of November is in the next week: both the reminders are in the future
        assertEquals(listOf(ReminderKind.DAY_BEFORE, ReminderKind.HOURS_BEFORE), scheduler.planned?.map { it.kind })
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun `without the permission of the system nothing is shown or planned`() = runTest {
        cache.saveSelected(selected)
        changeSettings.enabled = true
        reminderSettings.enabled = true
        scheduler.granted = false
        sync().run()
        room = "Е-205"

        sync().run()

        assertEquals(emptyList(), notifier.shown)
        assertEquals(null, scheduler.planned)
    }

    @Test
    fun `the weeks that were loaded are also in the cache of the screens`() = runTest {
        cache.saveSelected(selected)
        changeSettings.enabled = true

        sync().run()

        assertTrue(cache.week(selected, LocalDate(2026, 10, 26)) != null)
        assertTrue(cache.week(selected, LocalDate(2026, 11, 9)) != null)
    }
}
