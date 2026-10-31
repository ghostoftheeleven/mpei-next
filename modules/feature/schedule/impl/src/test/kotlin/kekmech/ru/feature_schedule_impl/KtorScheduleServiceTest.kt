@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_impl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kekmech.ru.feature_schedule_api.domain.model.ScheduleType
import kekmech.ru.feature_schedule_impl.data.model.ClassesTypeDto
import kekmech.ru.feature_schedule_impl.data.model.ScheduleTypeDto
import kekmech.ru.feature_schedule_impl.data.network.KtorScheduleService
import kekmech.ru.lib_http.ClientIdentity
import kekmech.ru.lib_http.createHttpClient
import kekmech.ru.lib_mpei_api.MpeiScheduleApi
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kekmech.ru.feature_schedule_shared.ScheduleService as SharedScheduleService

class KtorScheduleServiceTest : BehaviorSpec({

    val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    // Friday 30 October 2026: the current week starts on Monday 26 October
    val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-30T09:00:00Z")
    }
    val search = """[{"id":9,"label":"А-01-22","description":"Группа","type":"group"}]"""
    val classes =
        """[{"beginLesson":"09:20","endLesson":"10:55","date":"2026.10.27","discipline":"Физика","kindOfWork":"Лекция"}]"""

    fun service(status: HttpStatusCode = HttpStatusCode.OK): KtorScheduleService {
        val engine = MockEngine { request ->
            when {
                status != HttpStatusCode.OK -> respond("{}", status, jsonHeaders)
                request.url.encodedPath.endsWith("/search") -> respond(search, HttpStatusCode.OK, jsonHeaders)
                else -> respond(classes, HttpStatusCode.OK, jsonHeaders)
            }
        }
        val api = MpeiScheduleApi(
            client = createHttpClient(ClientIdentity("test", "id", "Android", "ru", "1"), engine),
            baseUrl = { "https://ts.mpei.ru/api/" },
        )
        return KtorScheduleService(SharedScheduleService(api, clock))
    }

    given("a week from the shared service") {
        `when`("it is mapped to the DTO of the Android app") {
            val dto = kotlinx.coroutines.runBlocking { service().getSchedule("group", "А-01-22", 0) }

            then("the schedule has the dates as java.time and the type of the group") {
                dto.name shouldBe "А-01-22"
                dto.id shouldBe "9"
                dto.type shouldBe ScheduleTypeDto.GROUP
                val week = dto.weeks.single()
                week.firstDayOfWeek shouldBe LocalDate.of(2026, 10, 26)
                week.days shouldHaveSize 7
                val lecture = week.days[1].classes.single()
                lecture.name shouldBe "Физика"
                lecture.type shouldBe ClassesTypeDto.LECTURE
                lecture.time.start shouldBe LocalTime.of(9, 20)
                lecture.number shouldBe 1
            }
        }
    }

    given("a search") {
        `when`("the answer has a group") {
            val response = kotlinx.coroutines.runBlocking { service().getSearchResults("А-01", ScheduleType.GROUP) }

            then("the item is in the DTO of the Android app") {
                response.items.single().name shouldBe "А-01-22"
                response.items.single().type shouldBe ScheduleTypeDto.GROUP
            }
        }
    }

    given("the portal answers with a bad request") {
        then("the screens get the HttpException with the same code") {
            val error = shouldThrow<HttpException> {
                service(HttpStatusCode.BadRequest).getSearchResults("%%", null)
            }
            error.code() shouldBe 400
        }
    }
})
