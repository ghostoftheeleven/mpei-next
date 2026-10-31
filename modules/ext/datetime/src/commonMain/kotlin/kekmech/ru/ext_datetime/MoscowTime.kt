@file:OptIn(ExperimentalTime::class)

package kekmech.ru.ext_datetime

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/** The schedule of MPEI is always read in Moscow time, wherever the phone is. */
public val MoscowTimeZone: TimeZone = TimeZone.of("Europe/Moscow")

public fun moscowDateTime(clock: Clock = Clock.System): LocalDateTime =
    clock.now().toLocalDateTime(MoscowTimeZone)

public fun moscowDate(clock: Clock = Clock.System): LocalDate = moscowDateTime(clock).date

public fun moscowTime(clock: Clock = Clock.System): LocalTime = moscowDateTime(clock).time
