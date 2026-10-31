@file:OptIn(ExperimentalTime::class)

package kekmech.ru.feature_schedule_impl.data.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kekmech.ru.feature_schedule_api.domain.model.ScheduleType
import kekmech.ru.feature_schedule_impl.data.model.ClassesDto
import kekmech.ru.feature_schedule_impl.data.model.ClassesTypeDto
import kekmech.ru.feature_schedule_impl.data.model.DayDto
import kekmech.ru.feature_schedule_impl.data.model.GetSearchResultsResponse
import kekmech.ru.feature_schedule_impl.data.model.ScheduleDto
import kekmech.ru.feature_schedule_impl.data.model.ScheduleTypeDto
import kekmech.ru.feature_schedule_impl.data.model.SearchResultDto
import kekmech.ru.feature_schedule_impl.data.model.TimeDto
import kekmech.ru.feature_schedule_impl.data.model.WeekDto
import kekmech.ru.feature_schedule_shared.Classes
import kekmech.ru.feature_schedule_shared.ClassesType
import kekmech.ru.feature_schedule_shared.Day
import kekmech.ru.feature_schedule_shared.Schedule
import kekmech.ru.feature_schedule_shared.SearchResult
import kekmech.ru.feature_schedule_shared.Week
import kekmech.ru.feature_schedule_shared.WeekOfSemester
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.net.SocketTimeoutException
import kotlin.time.ExperimentalTime
import kekmech.ru.feature_schedule_shared.ScheduleService as SharedScheduleService
import kekmech.ru.feature_schedule_shared.ScheduleType as SharedScheduleType

/**
 * The schedule from the code that is shared with iOS ([SharedScheduleService]: Ktor, the mapper and the id cache),
 * in the form the Android screens, the cache and the widget have always had ([ScheduleDto]). The errors of Ktor are
 * turned into the ones that the screens know: [HttpException] for a bad status, [SocketTimeoutException] for a timeout.
 * It goes away when the Android models are replaced by the shared ones.
 */
internal class KtorScheduleService(
    private val shared: SharedScheduleService,
) : ScheduleService {

    override suspend fun getSchedule(type: String, name: String, weekOffset: Int): ScheduleDto =
        translateErrors("schedule $name") {
            shared.getWeek(type.toSharedType(), name, weekOffset).toDto()
        }

    override suspend fun getSearchResults(query: String, type: ScheduleType?): GetSearchResultsResponse =
        translateErrors("search $query") {
            val sharedType = when (type) {
                ScheduleType.GROUP -> SharedScheduleType.GROUP
                ScheduleType.PERSON -> SharedScheduleType.PERSON
                null -> null
            }
            GetSearchResultsResponse(items = shared.search(query, sharedType).map { it.toDto() })
        }

    private suspend fun <T> translateErrors(what: String, block: suspend () -> T): T =
        try {
            block()
        } catch (e: ResponseException) {
            Timber.tag(TAG).w(e, "MPEI request failed: $what")
            val body = runCatching { e.response.bodyAsText() }.getOrDefault("")
            throw HttpException(Response.error<Unit>(e.response.status.value, body.toResponseBody(JSON_MEDIA_TYPE)))
        } catch (e: HttpRequestTimeoutException) {
            Timber.tag(TAG).w(e, "MPEI request timed out: $what")
            throw SocketTimeoutException(e.message)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.tag(TAG).w(e, "MPEI request failed: $what")
            throw e
        }

    private fun String.toSharedType(): SharedScheduleType =
        if (equals("person", ignoreCase = true)) SharedScheduleType.PERSON else SharedScheduleType.GROUP

    private companion object {
        const val TAG = "KtorScheduleService"
        val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()
    }
}

internal fun Schedule.toDto(): ScheduleDto = ScheduleDto(
    name = name,
    id = id,
    type = type.toDto(),
    weeks = weeks.map { it.toDto() },
)

private fun Week.toDto() = WeekDto(
    weekOfYear = weekOfYear,
    // -1 is a week of the holidays or of the session, the same as the cache of the app has always kept
    weekOfSemester = when (val semester = weekOfSemester) {
        WeekOfSemester.NonStudying -> NON_STUDYING_WEEK
        is WeekOfSemester.Studying -> semester.num
    },
    firstDayOfWeek = firstDayOfWeek.toJavaLocalDate(),
    days = days.map { it.toDto() },
)

private fun Day.toDto() = DayDto(
    dayOfWeek = dayOfWeek,
    date = date.toJavaLocalDate(),
    classes = classes.map { it.toDto() },
)

private fun Classes.toDto() = ClassesDto(
    name = name,
    type = type.toDto(),
    rawType = rawType,
    place = place,
    groups = groups,
    person = person,
    time = TimeDto(start = time.start.toJavaLocalTime(), end = time.end.toJavaLocalTime()),
    number = number,
)

internal fun SearchResult.toDto() = SearchResultDto(
    id = id,
    name = name,
    description = description,
    type = type.toDto(),
)

private fun SharedScheduleType.toDto() = when (this) {
    SharedScheduleType.GROUP -> ScheduleTypeDto.GROUP
    SharedScheduleType.PERSON -> ScheduleTypeDto.PERSON
}

private fun ClassesType.toDto() = when (this) {
    ClassesType.UNDEFINED -> ClassesTypeDto.UNDEFINED
    ClassesType.LECTURE -> ClassesTypeDto.LECTURE
    ClassesType.PRACTICE -> ClassesTypeDto.PRACTICE
    ClassesType.LAB -> ClassesTypeDto.LAB
    ClassesType.COURSE -> ClassesTypeDto.COURSE
    ClassesType.EXAM -> ClassesTypeDto.EXAM
    ClassesType.CONSULTATION -> ClassesTypeDto.CONSULTATION
}

private const val NON_STUDYING_WEEK = -1
