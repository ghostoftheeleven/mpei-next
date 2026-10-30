package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.lib_bars_client.BarsLoginResult
import kekmech.ru.lib_bars_client.BarsNetworkException
import kekmech.ru.lib_bars_client.BarsSessionExpiredException
import kekmech.ru.lib_bars_parser.BarsMarksPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the BARS screen does: it keeps the [state] that the screen draws and changes it when the screen reports a tap.
 *
 * The login and the password go to the portal and nowhere else: they are not kept in the state. A tap while a request
 * is going is ignored, so the screen does not have to guard against it.
 *
 * @param scope where the requests run; the controller does not outlive it
 */
class BarsController(
    private val api: BarsApi,
    private val scope: CoroutineScope,
) {

    private val mutableState = MutableStateFlow<BarsUiState>(BarsUiState.Starting)
    val state: StateFlow<BarsUiState> = mutableState.asStateFlow()

    /** Brings back the saved session, or asks for the sign-in. Also the "try again" of [BarsUiState.Failed]. */
    fun start() {
        val current = mutableState.value
        if (current !is BarsUiState.Starting && current !is BarsUiState.Failed) return
        mutableState.value = BarsUiState.Starting
        scope.launch {
            mutableState.value = guardState(onError = { BarsUiState.Failed(describe(it)) }) {
                if (api.restoreSession()) openContent() else BarsUiState.SignIn()
            }
        }
    }

    fun signIn(login: String, password: String) {
        val current = mutableState.value as? BarsUiState.SignIn ?: return
        if (current.busy) return
        if (login.isBlank() || password.isEmpty()) {
            mutableState.value = current.copy(error = "Введите логин и пароль")
            return
        }
        mutableState.value = current.copy(busy = true, error = null)
        scope.launch {
            mutableState.value = guardState(onError = { BarsUiState.SignIn(error = describe(it)) }) {
                afterAuth(api.login(login.trim(), password))
            }
        }
    }

    fun submitCode(code: String) {
        val current = mutableState.value as? BarsUiState.Code ?: return
        if (current.busy) return
        if (code.isBlank()) {
            mutableState.value = current.copy(error = "Введите код")
            return
        }
        mutableState.value = current.copy(busy = true, error = null)
        scope.launch {
            mutableState.value = guardState(onError = { current.copy(busy = false, error = describe(it)) }) {
                when (val result = api.submitCode(code)) {
                    is BarsLoginResult.Rejected ->
                        if (result.canRetryCode) {
                            current.copy(busy = false, error = result.message, info = null)
                        } else {
                            BarsUiState.SignIn(error = result.message)
                        }
                    else -> afterAuth(result)
                }
            }
        }
    }

    /** Asks BARS to send the code another way, or again: the "send again" buttons of the page. */
    fun resendCode(provider: BarsTwoFactorProvider) {
        val current = mutableState.value as? BarsUiState.Code ?: return
        if (current.busy) return
        mutableState.value = current.copy(busy = true, error = null)
        scope.launch {
            mutableState.value = guardState(onError = { current.copy(busy = false, error = describe(it)) }) {
                val result = api.sendCode(provider)
                val failed = result.success == false || result.error != null
                current.copy(
                    busy = false,
                    provider = provider,
                    info = if (failed) null else result.message,
                    error = if (failed) result.error ?: "Не удалось отправить код" else null,
                )
            }
        }
    }

    /** Leaves the entry of the code. */
    fun backToSignIn() {
        if (mutableState.value is BarsUiState.Code) mutableState.value = BarsUiState.SignIn()
    }

    fun selectSemester(semesterId: String) {
        val current = mutableState.value as? BarsUiState.Content ?: return
        if (current.loading || semesterId == current.semesterId) return
        reload(current, semesterId, current.tab)
    }

    fun selectTab(tab: BarsTab) {
        val current = mutableState.value as? BarsUiState.Content ?: return
        if (current.loading || tab == current.tab) return
        val semesterId = current.semesterId
        if (tab == BarsTab.SUMMARY && current.summary == null && semesterId != null) {
            mutableState.value = current.copy(tab = tab, loading = true, error = null)
            scope.launch {
                mutableState.value = contentGuard(current.copy(tab = tab)) {
                    it.copy(summary = api.loadSummary(semesterId), loading = false)
                }
            }
        } else {
            mutableState.value = current.copy(tab = tab, error = null)
        }
    }

    /** Loads the shown semester again. */
    fun refresh() {
        val current = mutableState.value as? BarsUiState.Content ?: return
        if (current.loading) return
        reload(current, current.semesterId ?: return, current.tab)
    }

    fun signOut() {
        scope.launch {
            attempt(onError = { Unit }) { api.logout() }
            mutableState.value = BarsUiState.SignIn()
        }
    }

    private fun reload(current: BarsUiState.Content, semesterId: String, tab: BarsTab) {
        mutableState.value = current.copy(loading = true, error = null)
        scope.launch {
            mutableState.value = contentGuard(current) {
                val part = api.loadSemesterMarks(semesterId)
                // The rating is a nice-to-have: its failure must not hide the marks
                val rating = attempt(onError = { null }) { api.loadRating(semesterId) }
                val summary = if (tab == BarsTab.SUMMARY) api.loadSummary(semesterId) else null
                it.copy(
                    semesterId = semesterId,
                    disciplines = part.disciplines,
                    rating = rating,
                    summary = summary,
                    loading = false,
                )
            }
        }
    }

    private suspend fun afterAuth(result: BarsLoginResult): BarsUiState = when (result) {
        is BarsLoginResult.LoggedIn ->
            if (result.studentId == null && result.students.isNotEmpty()) {
                // The pages of a student are asked for by his id, and which of several is the user's is not a guess
                attempt(onError = { Unit }) { api.logout() }
                BarsUiState.SignIn(error = "На этом аккаунте несколько студентов, такой вход пока не поддерживается")
            } else {
                openContent()
            }
        is BarsLoginResult.TwoFactorRequired -> BarsUiState.Code(
            page = result.page,
            info = result.codeRequest?.let { it.message ?: it.error },
        )
        is BarsLoginResult.Rejected -> BarsUiState.SignIn(error = result.message)
    }

    private suspend fun openContent(): BarsUiState =
        guardState(onError = { BarsUiState.Failed(describe(it)) }) { content(api.loadMarksPage()) }

    private fun content(page: BarsMarksPage) = BarsUiState.Content(
        studentName = page.studentName,
        group = page.studentGroup,
        meta = page.meta,
        semesters = page.semesters,
        semesterId = (page.selectedSemester ?: page.semesters.firstOrNull())?.id,
        disciplines = page.disciplines,
        rating = page.rating,
    )

    /** A request of the signed-in screen: on a failure the data that was shown stays, with the reason. */
    private suspend fun contentGuard(
        before: BarsUiState.Content,
        block: suspend (BarsUiState.Content) -> BarsUiState,
    ): BarsUiState = guardState(onError = { before.copy(loading = false, error = describe(it)) }) { block(before) }

    /**
     * Runs [block]. The sign-in page where a page was asked for (the session has ended) leads to the sign-in screen,
     * any other failure to what [onError] makes of it. A cancellation is not a failure.
     */
    private suspend fun guardState(
        onError: (Exception) -> BarsUiState,
        block: suspend () -> BarsUiState,
    ): BarsUiState = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (expired: BarsSessionExpiredException) {
        BarsUiState.SignIn(error = "Сессия закончилась, войдите снова")
    } catch (failure: Exception) {
        onError(failure)
    }

    /** Runs [block]; whatever fails (a cancellation excepted) gives what [onError] makes of it. */
    private suspend fun <T> attempt(onError: (Exception) -> T, block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onError(failure)
    }

    private fun describe(failure: Exception): String = when (failure) {
        is BarsNetworkException -> failure.message ?: "БАРС ответил не так, как ожидалось"
        else -> "Не удалось связаться с БАРС. Проверьте интернет и попробуйте ещё раз"
    }
}
