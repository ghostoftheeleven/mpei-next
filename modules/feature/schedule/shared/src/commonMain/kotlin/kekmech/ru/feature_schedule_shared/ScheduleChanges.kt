package kekmech.ru.feature_schedule_shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** A change in the schedule between the version the app knew and the version that came now. */
public data class ScheduleChange(
    val type: ChangeType,
    val date: LocalDate,
    val oldClass: Classes?,
    val newClass: Classes?,
) {

    public enum class ChangeType {
        /** A new class. */
        ADDED,

        /** A class was cancelled. */
        REMOVED,

        /** The class is the same, its details changed: the time, the room, the teacher, the type. */
        MODIFIED,

        /** The class was replaced with another one. */
        REPLACED,
    }

    /** The change in words: "Заменена пара: Физика → Математика". */
    public val description: String
        get() = when (type) {
            ChangeType.ADDED -> "Добавлена пара: ${newClass?.name.orEmpty()}"
            ChangeType.REMOVED -> "Отменена пара: ${oldClass?.name.orEmpty()}"
            ChangeType.MODIFIED -> "Изменена пара ${newClass?.name.orEmpty()}: ${modifiedFields()}"
            ChangeType.REPLACED -> "Заменена пара: ${oldClass?.name.orEmpty()} → ${newClass?.name.orEmpty()}"
        }

    private fun modifiedFields(): String {
        val fields = mutableListOf<String>()
        if (oldClass?.time != newClass?.time) {
            fields += "время (${oldClass?.time?.start} → ${newClass?.time?.start})"
        }
        if (oldClass?.place != newClass?.place) {
            fields += "аудитория (${oldClass?.place.orEmpty()} → ${newClass?.place.orEmpty()})"
        }
        if (oldClass?.person != newClass?.person) fields += "преподаватель"
        if (oldClass?.type != newClass?.type) fields += "тип"
        return fields.joinToString(", ")
    }
}

/**
 * Compares the version of a week that the app knew with the new one and says what changed (the same algorithm as the
 * Android app). The classes of a day are compared slot by slot (the first class, the second...):
 * 1. the exact matches are not changes;
 * 2. the same name and the same groups, the details differ: MODIFIED;
 * 3. the same groups, the other discipline: REPLACED;
 * 4. the same name, the other groups: MODIFIED;
 * 5. what is left in the slot is paired one to one: REPLACED or MODIFIED;
 * 6. what is left only in the old one: REMOVED; only in the new one: ADDED.
 */
public object ScheduleChangeDetector {

    /** @return no changes if there is no [oldSchedule] to compare with */
    public fun detectChanges(oldSchedule: Schedule?, newSchedule: Schedule): List<ScheduleChange> {
        if (oldSchedule == null) return emptyList()
        val changes = mutableListOf<ScheduleChange>()
        for (newWeek in newSchedule.weeks) {
            val oldWeek = oldSchedule.weeks.firstOrNull { it.weekOfYear == newWeek.weekOfYear } ?: continue
            for (newDay in newWeek.days) {
                val oldDay = oldWeek.days.firstOrNull { it.date == newDay.date } ?: continue
                changes += detectDayChanges(newDay.date, oldDay.classes, newDay.classes)
            }
        }
        return changes
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun detectDayChanges(date: LocalDate, oldClasses: List<Classes>, newClasses: List<Classes>): List<ScheduleChange> {
        val changes = mutableListOf<ScheduleChange>()
        val oldByNumber = oldClasses.groupBy { it.number }
        val newByNumber = newClasses.groupBy { it.number }

        for (number in (oldByNumber.keys + newByNumber.keys).sorted()) {
            val oldSlot = oldByNumber[number].orEmpty().toMutableList()
            val newSlot = newByNumber[number].orEmpty().toMutableList()

            fun paired(old: Classes, new: Classes, type: ScheduleChange.ChangeType) {
                changes += ScheduleChange(type, date, old, new)
            }

            // 1. the exact matches
            oldSlot.removeAll { old ->
                val index = newSlot.indexOfFirst { areEqual(old, it) }
                if (index != -1) newSlot.removeAt(index)
                index != -1
            }
            // 2. the same name and groups
            oldSlot.removeAll { old ->
                val index = newSlot.indexOfFirst { it.name == old.name && it.groups == old.groups }
                if (index != -1) paired(old, newSlot.removeAt(index), ScheduleChange.ChangeType.MODIFIED)
                index != -1
            }
            // 3. the same groups
            oldSlot.removeAll { old ->
                val index = if (old.groups.isBlank()) {
                    -1
                } else {
                    newSlot.indexOfFirst { it.groups.isNotBlank() && it.groups == old.groups }
                }
                if (index != -1) {
                    val new = newSlot.removeAt(index)
                    paired(old, new, if (old.name != new.name) ScheduleChange.ChangeType.REPLACED else ScheduleChange.ChangeType.MODIFIED)
                }
                index != -1
            }
            // 4. the same name
            oldSlot.removeAll { old ->
                val index = newSlot.indexOfFirst { it.name == old.name }
                if (index != -1) paired(old, newSlot.removeAt(index), ScheduleChange.ChangeType.MODIFIED)
                index != -1
            }
            // 5. what is left in the slot, one to one
            while (oldSlot.isNotEmpty() && newSlot.isNotEmpty()) {
                val old = oldSlot.removeAt(0)
                val new = newSlot.removeAt(0)
                paired(old, new, if (old.name != new.name) ScheduleChange.ChangeType.REPLACED else ScheduleChange.ChangeType.MODIFIED)
            }
            // 6. the rest
            oldSlot.forEach { changes += ScheduleChange(ScheduleChange.ChangeType.REMOVED, date, it, null) }
            newSlot.forEach { changes += ScheduleChange(ScheduleChange.ChangeType.ADDED, date, null, it) }
        }
        return changes
    }

    private fun areEqual(a: Classes, b: Classes): Boolean =
        a.name == b.name && a.type == b.type && a.place == b.place && a.person == b.person &&
            a.time == b.time && a.groups == b.groups
}

/** The words of the notification about the changes (the same as in the Android app). */
public object ScheduleChangeText {

    private const val MAX_CHANGES = 10
    private const val FEW_CHANGES_LIMIT = 5
    private val DAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    private val MONTHS = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )

    public fun title(count: Int): String = when {
        count == 1 -> "Изменение в расписании"
        count < FEW_CHANGES_LIMIT -> "Изменения в расписании"
        else -> "Много изменений в расписании"
    }

    /** One line: the only change in full, or how many there are. */
    public fun preview(changes: List<ScheduleChange>, today: LocalDate): String {
        val first = changes.first()
        return if (changes.size == 1) "${formatDate(first.date, today)}: ${first.description}" else "Изменений в расписании: ${changes.size}"
    }

    /** All the changes by days, up to ten of them. */
    public fun details(changes: List<ScheduleChange>, today: LocalDate): String {
        val text = changes
            .sortedBy { it.date }
            .take(MAX_CHANGES)
            .groupBy { it.date }
            .entries
            .joinToString("\n\n") { (date, dayChanges) ->
                "${formatDate(date, today)}:\n" + dayChanges.joinToString("\n") { "• ${it.description}" }
            }
        val hidden = changes.size - MAX_CHANGES
        return if (hidden > 0) "$text\n\n… и ещё $hidden" else text
    }

    /** "Сегодня", "Завтра" or "5 ноября (чт)". */
    public fun formatDate(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Сегодня"
        today.plus(1, DateTimeUnit.DAY) -> "Завтра"
        else -> "${date.dayOfMonth} ${MONTHS[date.monthNumber - 1]} (${DAYS[date.dayOfWeek.ordinal]})"
    }
}
