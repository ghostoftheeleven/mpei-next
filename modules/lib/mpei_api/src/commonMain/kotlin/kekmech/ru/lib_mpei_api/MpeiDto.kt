package kekmech.ru.lib_mpei_api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class MpeiSearchResultDto(
    @SerialName("id")
    val id: Long,
    @SerialName("label")
    val label: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("type")
    val type: String,
)

@Serializable
public data class MpeiClassesDto(
    @SerialName("auditorium")
    val auditorium: String? = null,
    @SerialName("beginLesson")
    val beginLesson: String,
    @SerialName("endLesson")
    val endLesson: String,
    @SerialName("date")
    val date: String,
    @SerialName("discipline")
    val discipline: String,
    @SerialName("kindOfWork")
    val kindOfWork: String? = null,
    @SerialName("lecturer")
    val lecturer: String? = null,
    @SerialName("group")
    val group: String? = null,
    @SerialName("subGroup")
    val subGroup: String? = null,
    @SerialName("stream")
    val stream: String? = null,
    @SerialName("dayOfWeek")
    val dayOfWeek: Int? = null,
    @SerialName("lessonNumberStart")
    val lessonNumberStart: Int? = null,
)
