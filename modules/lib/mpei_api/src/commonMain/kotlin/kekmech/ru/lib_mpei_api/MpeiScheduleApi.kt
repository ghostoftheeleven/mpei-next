package kekmech.ru.lib_mpei_api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kekmech.ru.lib_network.BackendServiceUrl
import kekmech.ru.lib_network.ServiceUrlResolver
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * The timetable API of MPEI: the same two requests as the Retrofit service of the Android app.
 *
 * @param baseUrl is asked for every request, because the environment of the app can change at run time.
 */
public class MpeiScheduleApi(
    private val client: HttpClient,
    private val baseUrl: () -> String = { ServiceUrlResolver.resolve(BackendServiceUrl.MPEI_TIMETABLE) },
) {

    /** @param type `group` or `person`; `null` searches both. */
    public suspend fun search(term: String, type: String? = null): List<MpeiSearchResultDto> =
        client.get("${baseUrl()}search") {
            parameter("term", term)
            if (type != null) parameter("type", type)
        }.body()

    /** @param start the first day and [finish] the last day, both as `yyyy.MM.dd`. */
    public suspend fun getSchedule(
        type: String,
        id: Long,
        start: String,
        finish: String,
        lng: Int = 1,
    ): List<MpeiClassesDto> =
        client.get("${baseUrl()}schedule/$type/$id") {
            parameter("start", start)
            parameter("finish", finish)
            parameter("lng", lng)
        }.body()

    /** The classes of the week that starts on [monday] (Monday to Sunday). */
    public suspend fun getWeek(type: String, id: Long, monday: LocalDate): List<MpeiClassesDto> =
        getSchedule(
            type = type,
            id = id,
            start = formatMpeiDate(monday),
            finish = formatMpeiDate(monday.plus(DAYS_IN_WEEK_AFTER_MONDAY, DateTimeUnit.DAY)),
        )

    private companion object {
        const val DAYS_IN_WEEK_AFTER_MONDAY = 6
    }
}

/** `2026.10.30`: the date format of the API. */
public fun formatMpeiDate(date: LocalDate): String =
    "${date.year.toString().padStart(YEAR_DIGITS, '0')}." +
        "${date.monthNumber.toString().padStart(2, '0')}." +
        date.dayOfMonth.toString().padStart(2, '0')

private const val YEAR_DIGITS = 4
