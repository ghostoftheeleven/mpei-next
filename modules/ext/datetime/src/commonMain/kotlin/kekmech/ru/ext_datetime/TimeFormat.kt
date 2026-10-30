package kekmech.ru.ext_datetime

import kotlinx.datetime.LocalTime

/** "09:05", the same as DateTimeFormatter.ofPattern("HH:mm") does. */
public fun LocalTime.formatHm(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
