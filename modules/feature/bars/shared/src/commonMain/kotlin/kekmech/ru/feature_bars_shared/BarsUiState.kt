package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.BarsMetaItem
import kekmech.ru.lib_bars_parser.BarsRating
import kekmech.ru.lib_bars_parser.BarsSemester
import kekmech.ru.lib_bars_parser.BarsSummaryPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorProvider

/** What the BARS screen shows. */
sealed interface BarsUiState {

    /** The saved session is being looked for. */
    data object Starting : BarsUiState

    /** The login and the password are asked for. */
    data class SignIn(
        val busy: Boolean = false,
        val error: String? = null,
    ) : BarsUiState

    /** The code of the second factor is asked for. */
    data class Code(
        val page: BarsTwoFactorPage,
        /** The way the code is expected from: it sets the length of the code. */
        val provider: BarsTwoFactorProvider = page.default,
        val busy: Boolean = false,
        val error: String? = null,
        /** What BARS answered to the request for a code: "Сообщение отправлено в 'MAX'". */
        val info: String? = null,
    ) : BarsUiState

    /** The state cannot be opened: no connection, an answer that is not understood. [start][BarsController.start] tries again. */
    data class Failed(val message: String) : BarsUiState

    /** The student is signed in. */
    data class Content(
        val studentName: String?,
        val group: String?,
        val meta: List<BarsMetaItem>,
        val semesters: List<BarsSemester>,
        /** The semester that the marks, the rating and the summary belong to. */
        val semesterId: String?,
        val disciplines: List<BarsDiscipline>,
        val rating: BarsRating?,
        val tab: BarsTab = BarsTab.MARKS,
        /** The summary of [semesterId], null until the tab is opened. */
        val summary: BarsSummaryPage? = null,
        /** A request is going. */
        val loading: Boolean = false,
        /** Why the last request failed; what is shown is the data of the request before it. */
        val error: String? = null,
    ) : BarsUiState {

        val semesterName: String? get() = semesters.firstOrNull { it.id == semesterId }?.name
    }
}

enum class BarsTab { MARKS, SUMMARY }
