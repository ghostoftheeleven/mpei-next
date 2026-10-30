package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_parser.BarsActivity
import kekmech.ru.lib_bars_parser.BarsActivityType
import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.BarsMark

/** 5.0 -> "5", 2.66 -> "2,66": how BARS writes a mark. */
fun formatScore(value: Float): String {
    val whole = value.toInt()
    return if (value == whole.toFloat()) whole.toString() else value.toString().replace('.', ',')
}

/**
 * A mark cell for a line of a screen: "4", "2,66", "5 · зачтено", "3 (НД)", "—" for a cell without a mark.
 * What cannot be told as a number is shown as BARS wrote it.
 */
fun formatMark(mark: BarsMark?): String {
    if (mark == null) return "—"
    val value = mark.value ?: return mark.raw
    return buildString {
        append(formatScore(value))
        mark.verdict?.let { append(" · ").append(it) }
        mark.tag?.let { append(" (").append(it).append(')') }
    }
}

/** The activities of a discipline that carry a mark of the semester as a whole: the score, the attestation, the final mark. */
val BarsDiscipline.summaryActivities: List<BarsActivity>
    get() = activities.filter { it.type != BarsActivityType.CONTROL_ACTIVITY && it.mark != null }

/** The final mark of a discipline, null while there is none. */
val BarsDiscipline.finalMark: BarsMark?
    get() = activities.firstOrNull { it.type == BarsActivityType.FINAL_MARK }?.mark

/** The current score of a discipline ("Балл текущего контроля"), null while there is none. */
val BarsDiscipline.currentScore: BarsMark?
    get() = activities.firstOrNull { it.type == BarsActivityType.CURRENT_SCORE }?.mark

/** The control events of a discipline: what the student gets the marks for. */
val BarsDiscipline.controlActivities: List<BarsActivity>
    get() = activities.filter { it.type == BarsActivityType.CONTROL_ACTIVITY }
