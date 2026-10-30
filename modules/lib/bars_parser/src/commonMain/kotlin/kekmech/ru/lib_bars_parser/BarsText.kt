package kekmech.ru.lib_bars_parser

private val WHITESPACE = Regex("\\s+")
private val LEADING_NUMBER = Regex("^(\\d+(?:[.,]\\d+)?)")
private val DATE_ONLY = Regex("\\d{2}\\.\\d{2}\\.\\d{2,4}")
// "(06.06.26)" or "(НД, 06.06.26)": a date, maybe with a tag before it
private val DATE_IN_BRACKETS = Regex("\\((?:([^),]*),\\s*)?(\\d{2}\\.\\d{2}\\.\\d{2,4})\\)")
private val RETAKE = Regex("Пересдана:\\s*(\\S+)\\s*\\((\\d{2}\\.\\d{2}\\.\\d{2,4})\\)")
private val IGNORED = Regex("Не учитывается:\\s*(\\S+)\\s*\\((\\d{2}\\.\\d{2}\\.\\d{2,4})\\)")
// The brackets of the mark itself: right behind the number
private val MAIN_BRACKET = Regex("^\\s*" + DATE_IN_BRACKETS.pattern)
private val VERDICT = Regex("^/(не зачтено|зачтено)")
// "ППА1 - 3 (не уч.)", "ППА2 - 2/не зачтено (НД, 20.06.26)"
private val ATTEMPT = Regex("(ППА\\d+)\\s*-\\s*([^\\s(/]+)(?:/(не зачтено|зачтено))?(?:\\s*\\(([^)]*)\\))?")
private val UNITS = Regex("^(\\d+(?:[.,]\\d+)?)\\s*ЗЕ(.*)$")

/** Collapses every kind of whitespace, non-breaking spaces included, into single spaces. */
fun normalizeBarsText(text: String): String =
    text.replace(' ', ' ').replace(WHITESPACE, " ").trim()

/**
 * Parses a mark cell. [mainText] is the text of the cell itself ("4  (06.06.26)"), [retakeText] is the line about
 * the retake ("Пересдана: 0 (11.04.26)") if the cell has one. Returns null for an empty cell.
 */
fun parseBarsMark(mainText: String, retakeText: String? = null): BarsMark? {
    val main = normalizeBarsText(mainText)
    val retakeLine = retakeText?.let(::normalizeBarsText).orEmpty()
    if (main.isEmpty() && retakeLine.isEmpty()) return null

    val numberMatch = LEADING_NUMBER.find(main)
    val value = numberMatch?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull()
    var rest = if (numberMatch != null) main.removePrefix(numberMatch.value) else main

    // "5/зачтено": the verdict sits right behind the number
    val verdict = VERDICT.find(rest)?.also { rest = rest.removePrefix(it.value) }?.groupValues?.get(1)

    // The date of the mark itself is the brackets that follow the number; the dates of the attempts come later.
    // Without a number ("зачтено (06.06.26)") the first brackets with a date are taken.
    val bracket = if (value != null) {
        MAIN_BRACKET.find(rest)
    } else {
        DATE_IN_BRACKETS.find(rest)
    }
    if (bracket != null) rest = rest.removeRange(bracket.range)

    val ignored = IGNORED.find(rest)
    if (ignored != null) rest = rest.removeRange(ignored.range)

    val attempts = ATTEMPT.findAll(rest).map { attempt ->
        val (tag, date) = splitTagAndDate(attempt.groupValues[4])
        BarsAttempt(
            label = attempt.groupValues[1],
            rawValue = attempt.groupValues[2],
            value = attempt.groupValues[2].replace(',', '.').toFloatOrNull(),
            verdict = attempt.groupValues[3].ifEmpty { null },
            tag = tag,
            date = date,
        )
    }.toList()

    val retake = RETAKE.find(retakeLine)
    return BarsMark(
        raw = listOf(main, retakeLine).filter { it.isNotEmpty() }.joinToString(" "),
        value = value,
        date = bracket?.groupValues?.get(2),
        retakenFrom = retake?.groupValues?.get(1),
        retakenDate = retake?.groupValues?.get(2),
        note = normalizeBarsText(rest).ifEmpty { null },
        tag = bracket?.groupValues?.get(1)?.trim()?.ifEmpty { null },
        verdict = verdict,
        ignoredFrom = ignored?.groupValues?.get(1),
        ignoredDate = ignored?.groupValues?.get(2),
        attempts = attempts,
    )
}

/** "НД, 20.06.26" -> "НД" to "20.06.26"; "не уч." -> "не уч." to null; "22.06.26" -> null to "22.06.26". */
private fun splitTagAndDate(inner: String): Pair<String?, String?> {
    val parts = inner.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val date = parts.lastOrNull()?.takeIf { DATE_ONLY.matches(it) }
    val tag = (if (date != null) parts.dropLast(1) else parts).joinToString(", ").ifEmpty { null }
    return tag to date
}

/** The first integer of a "15 (07.12.26-13.12.26)" cell, null if there is none. */
fun parseBarsWeekNumber(weekNum: String?): Int? {
    val number = weekNum?.let { LEADING_NUMBER.find(normalizeBarsText(it)) }?.groupValues?.get(1) ?: return null
    return number.substringBefore('.').substringBefore(',').toIntOrNull()
}

/** "12,0" -> 12.0, null if the whole text is not a number. */
fun parseBarsNumber(text: String): Float? =
    normalizeBarsText(text).replace(',', '.').toFloatOrNull()

/** "Электротехника (экзамен)" -> "Электротехника" to "экзамен"; the last brackets only, the rest may have its own. */
internal fun splitTrailingBrackets(text: String): Pair<String, String?> {
    val trimmed = normalizeBarsText(text)
    if (!trimmed.endsWith(")")) return trimmed to null
    var depth = 0
    for (index in trimmed.indices.reversed()) {
        when (trimmed[index]) {
            ')' -> depth++
            '(' -> {
                depth--
                if (depth == 0) {
                    val inner = trimmed.substring(index + 1, trimmed.length - 1).trim()
                    val name = trimmed.substring(0, index).trim()
                    return if (name.isEmpty()) trimmed to null else name to inner.ifEmpty { null }
                }
            }
        }
    }
    return trimmed to null
}

internal data class DisciplineHeader(
    val name: String,
    val person: String,
    val assessmentType: String,
    val units: String,
    val note: String?,
)

/**
 * "Дисциплина, Преподаватель, экзамен, 4ЗЕ [примечание]" -> parts. Only the discipline name may contain commas,
 * therefore the parts are taken from the end; commas inside brackets do not split.
 */
internal fun parseDisciplineHeader(text: String): DisciplineHeader {
    val parts = splitOutsideBrackets(normalizeBarsText(text)).toMutableList()

    var units = ""
    var note: String? = null
    val unitsMatch = parts.lastOrNull()?.let { UNITS.find(it) }
    if (unitsMatch != null) {
        units = unitsMatch.groupValues[1] + "ЗЕ"
        note = unitsMatch.groupValues[2].trim().ifEmpty { null }
        parts.removeAt(parts.lastIndex)
    }

    return when {
        parts.size >= 3 -> DisciplineHeader(
            name = parts.dropLast(2).joinToString(", "),
            person = parts[parts.size - 2],
            assessmentType = parts.last(),
            units = units,
            note = note,
        )
        parts.size == 2 -> DisciplineHeader(parts[0], person = "", assessmentType = parts[1], units = units, note = note)
        else -> DisciplineHeader(parts.firstOrNull().orEmpty(), person = "", assessmentType = "", units = units, note = note)
    }
}

private fun splitOutsideBrackets(text: String): List<String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    for (ch in text) {
        when {
            ch == '(' -> { depth++; current.append(ch) }
            ch == ')' -> { if (depth > 0) depth--; current.append(ch) }
            ch == ',' && depth == 0 -> { parts += current.toString().trim(); current.clear() }
            else -> current.append(ch)
        }
    }
    parts += current.toString().trim()
    return parts.filter { it.isNotEmpty() }
}
