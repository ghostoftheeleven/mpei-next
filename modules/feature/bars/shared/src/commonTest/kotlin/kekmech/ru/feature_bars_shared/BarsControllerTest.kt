package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_client.BarsApi
import kekmech.ru.lib_bars_client.BarsLoginResult
import kekmech.ru.lib_bars_client.BarsNetworkException
import kekmech.ru.lib_bars_client.BarsSessionExpiredException
import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.BarsMarksPage
import kekmech.ru.lib_bars_parser.BarsMarksPart
import kekmech.ru.lib_bars_parser.BarsRating
import kekmech.ru.lib_bars_parser.BarsSemester
import kekmech.ru.lib_bars_parser.BarsSendCodeResult
import kekmech.ru.lib_bars_parser.BarsSummaryPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorKind
import kekmech.ru.lib_bars_parser.BarsTwoFactorPage
import kekmech.ru.lib_bars_parser.BarsTwoFactorProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BarsControllerTest {

    private val api = FakeApi()

    @Test
    fun `without a saved session the sign-in is asked for`() = runTest(UnconfinedTestDispatcher()) {
        val controller = BarsController(api, this)

        controller.start()

        assertEquals(BarsUiState.SignIn(), controller.state.value)
    }

    @Test
    fun `a saved session opens the marks`() = runTest(UnconfinedTestDispatcher()) {
        api.restored = true
        val controller = BarsController(api, this)

        controller.start()

        val content = assertIs<BarsUiState.Content>(controller.state.value)
        assertEquals("Иванов Иван", content.studentName)
        assertEquals("27", content.semesterId)
        assertEquals("Весна", content.semesterName)
        assertEquals(listOf("Химия"), content.disciplines.map { it.name })
        assertEquals(BarsTab.MARKS, content.tab)
    }

    @Test
    fun `a saved session that BARS has closed leads to the sign-in`() = runTest(UnconfinedTestDispatcher()) {
        api.restored = true
        api.marksError = BarsSessionExpiredException()
        val controller = BarsController(api, this)

        controller.start()

        val state = assertIs<BarsUiState.SignIn>(controller.state.value)
        assertEquals("Сессия закончилась, войдите снова", state.error)
    }

    @Test
    fun `no connection is a failure that can be tried again`() = runTest(UnconfinedTestDispatcher()) {
        api.restored = true
        api.marksError = RuntimeException("offline")
        val controller = BarsController(api, this)
        controller.start()
        assertIs<BarsUiState.Failed>(controller.state.value)

        api.marksError = null
        controller.start()

        assertIs<BarsUiState.Content>(controller.state.value)
    }

    @Test
    fun `an answer that is not understood says so`() = runTest(UnconfinedTestDispatcher()) {
        api.restored = true
        api.marksError = BarsNetworkException("Unexpected page (HTTP 200)")
        val controller = BarsController(api, this)

        controller.start()

        assertEquals(BarsUiState.Failed("Unexpected page (HTTP 200)"), controller.state.value)
    }

    @Test
    fun `the login and the password are asked for before anything is sent`() = runTest(UnconfinedTestDispatcher()) {
        val controller = signInScreen()

        controller.signIn("  ", "pass")
        assertEquals("Введите логин и пароль", assertIs<BarsUiState.SignIn>(controller.state.value).error)
        controller.signIn("user", "")
        assertEquals("Введите логин и пароль", assertIs<BarsUiState.SignIn>(controller.state.value).error)

        assertTrue(api.calls.none { it.startsWith("login") })
    }

    @Test
    fun `a wrong password shows the answer of BARS`() = runTest(UnconfinedTestDispatcher()) {
        api.loginResult = BarsLoginResult.Rejected("Пользователь не найден", canRetryCode = false)
        val controller = signInScreen()

        controller.signIn("user", "bad")

        assertEquals(BarsUiState.SignIn(error = "Пользователь не найден"), controller.state.value)
    }

    @Test
    fun `the login is trimmed and the password is sent as typed`() = runTest(UnconfinedTestDispatcher()) {
        api.loginResult = BarsLoginResult.Rejected("x", canRetryCode = false)
        val controller = signInScreen()

        controller.signIn("  user ", " pass ")

        assertEquals(listOf("login(user)"), api.calls.filter { it.startsWith("login") })
        assertEquals(" pass ", api.lastPassword)
    }

    @Test
    fun `after the password the code is asked for with the answer to the request for it`() = runTest(UnconfinedTestDispatcher()) {
        api.loginResult = BarsLoginResult.TwoFactorRequired(twoFactorPage(), BarsSendCodeResult(true, "Сообщение отправлено в 'MAX'"))
        val controller = signInScreen()

        controller.signIn("user", "pass")

        val state = assertIs<BarsUiState.Code>(controller.state.value)
        assertEquals("Сообщение отправлено в 'MAX'", state.info)
        assertEquals(BarsTwoFactorKind.TOTP, state.provider.kind)
        assertEquals(false, state.busy)
    }

    @Test
    fun `a wrong code can be tried again and a right one opens the marks`() = runTest(UnconfinedTestDispatcher()) {
        val controller = codeScreen()

        api.codeResult = BarsLoginResult.Rejected("Некорректный код подтверждения", canRetryCode = true)
        controller.submitCode("000000")
        val retry = assertIs<BarsUiState.Code>(controller.state.value)
        assertEquals("Некорректный код подтверждения", retry.error)
        assertEquals(false, retry.busy)

        api.codeResult = BarsLoginResult.LoggedIn("id", emptyList())
        controller.submitCode("123456")
        assertIs<BarsUiState.Content>(controller.state.value)
    }

    @Test
    fun `a code that cannot be retried leads back to the sign-in`() = runTest(UnconfinedTestDispatcher()) {
        val controller = codeScreen()
        api.codeResult = BarsLoginResult.Rejected("Сессия потеряна", canRetryCode = false)

        controller.submitCode("123456")

        assertEquals(BarsUiState.SignIn(error = "Сессия потеряна"), controller.state.value)
    }

    @Test
    fun `an empty code is not sent`() = runTest(UnconfinedTestDispatcher()) {
        val controller = codeScreen()

        controller.submitCode(" ")

        assertEquals("Введите код", assertIs<BarsUiState.Code>(controller.state.value).error)
        assertTrue(api.calls.none { it.startsWith("submitCode") })
    }

    @Test
    fun `the code is sent again another way`() = runTest(UnconfinedTestDispatcher()) {
        val controller = codeScreen()
        val max = twoFactorPage().providers.single { it.kind == BarsTwoFactorKind.MAX }
        api.sendResult = BarsSendCodeResult(true, "Сообщение отправлено в 'MAX'")

        controller.resendCode(max)

        val state = assertIs<BarsUiState.Code>(controller.state.value)
        assertEquals(max, state.provider)
        assertEquals("Сообщение отправлено в 'MAX'", state.info)
        assertNull(state.error)
    }

    @Test
    fun `a refused request for a code shows why`() = runTest(UnconfinedTestDispatcher()) {
        val controller = codeScreen()
        api.sendResult = BarsSendCodeResult(false, null, error = "Код уже отправлен")

        controller.resendCode(twoFactorPage().default)

        val state = assertIs<BarsUiState.Code>(controller.state.value)
        assertEquals("Код уже отправлен", state.error)
        assertNull(state.info)
    }

    @Test
    fun `back from the code to the sign-in`() = runTest(UnconfinedTestDispatcher()) {
        val controller = codeScreen()

        controller.backToSignIn()

        assertEquals(BarsUiState.SignIn(), controller.state.value)
    }

    @Test
    fun `several students on one account are not guessed`() = runTest(UnconfinedTestDispatcher()) {
        api.loginResult = BarsLoginResult.LoggedIn(null, listOf(student("a"), student("b")))
        val controller = signInScreen()

        controller.signIn("user", "pass")

        assertIs<BarsUiState.SignIn>(controller.state.value)
        assertTrue("logout" in api.calls)
    }

    @Test
    fun `a request that is going swallows the taps on the button`() = runTest(UnconfinedTestDispatcher()) {
        val gate = CompletableDeferred<BarsLoginResult>()
        api.loginGate = gate
        val controller = signInScreen()

        controller.signIn("user", "pass")
        assertEquals(true, assertIs<BarsUiState.SignIn>(controller.state.value).busy)
        controller.signIn("user", "pass")
        gate.complete(BarsLoginResult.Rejected("no", canRetryCode = false))

        assertEquals(1, api.calls.count { it.startsWith("login") })
        assertEquals(BarsUiState.SignIn(error = "no"), controller.state.value)
    }

    @Test
    fun `another semester brings its marks and its rating`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        api.part = BarsMarksPart("Осень", listOf(discipline("Физика")))
        api.ratingFor = BarsRating("28", emptyList(), emptyList())

        controller.selectSemester("28")

        val content = assertIs<BarsUiState.Content>(controller.state.value)
        assertEquals("28", content.semesterId)
        assertEquals(listOf("Физика"), content.disciplines.map { it.name })
        assertEquals("28", content.rating?.semesterId)
        assertEquals(false, content.loading)
        assertNull(content.error)
    }

    @Test
    fun `the same semester is not loaded again`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()

        controller.selectSemester("27")

        assertTrue(api.calls.none { it.startsWith("loadSemesterMarks") })
    }

    @Test
    fun `a failed semester keeps what was shown and says why`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        api.partError = RuntimeException("offline")

        controller.selectSemester("28")

        val content = assertIs<BarsUiState.Content>(controller.state.value)
        assertEquals("27", content.semesterId)
        assertEquals(listOf("Химия"), content.disciplines.map { it.name })
        assertEquals("Не удалось связаться с БАРС. Проверьте интернет и попробуйте ещё раз", content.error)
        assertEquals(false, content.loading)
    }

    @Test
    fun `a rating that fails does not hide the marks`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        api.part = BarsMarksPart("Осень", listOf(discipline("Физика")))
        api.ratingError = RuntimeException("no rating")

        controller.selectSemester("28")

        val content = assertIs<BarsUiState.Content>(controller.state.value)
        assertEquals(listOf("Физика"), content.disciplines.map { it.name })
        assertNull(content.rating)
        assertNull(content.error)
    }

    @Test
    fun `a closed session while a semester is loaded leads to the sign-in`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        api.partError = BarsSessionExpiredException()

        controller.selectSemester("28")

        assertIs<BarsUiState.SignIn>(controller.state.value)
    }

    @Test
    fun `the summary is loaded once when its tab is opened`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()

        controller.selectTab(BarsTab.SUMMARY)
        controller.selectTab(BarsTab.MARKS)
        controller.selectTab(BarsTab.SUMMARY)

        val content = assertIs<BarsUiState.Content>(controller.state.value)
        assertEquals(BarsTab.SUMMARY, content.tab)
        assertEquals("Весна", content.summary?.semesterName)
        assertEquals(1, api.calls.count { it.startsWith("loadSummary") })
        assertEquals("loadSummary(27)", api.calls.single { it.startsWith("loadSummary") })
    }

    @Test
    fun `another semester on the summary tab loads the summary of it`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        controller.selectTab(BarsTab.SUMMARY)
        api.part = BarsMarksPart("Осень", emptyList())

        controller.selectSemester("28")

        assertEquals(listOf("loadSummary(27)", "loadSummary(28)"), api.calls.filter { it.startsWith("loadSummary") })
    }

    @Test
    fun `refresh loads the shown semester again`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        api.part = BarsMarksPart("Весна", listOf(discipline("Химия"), discipline("Физика")))

        controller.refresh()

        val content = assertIs<BarsUiState.Content>(controller.state.value)
        assertEquals(2, content.disciplines.size)
        assertEquals("loadSemesterMarks(27)", api.calls.last { it.startsWith("loadSemesterMarks") })
    }

    @Test
    fun `sign out goes to the sign-in even if the request fails`() = runTest(UnconfinedTestDispatcher()) {
        val controller = contentScreen()
        api.logoutError = RuntimeException("offline")

        controller.signOut()

        assertEquals(BarsUiState.SignIn(), controller.state.value)
        assertTrue("logout" in api.calls)
    }

    private fun kotlinx.coroutines.test.TestScope.signInScreen(): BarsController =
        BarsController(api, this).also { it.start() }

    private fun kotlinx.coroutines.test.TestScope.codeScreen(): BarsController {
        api.loginResult = BarsLoginResult.TwoFactorRequired(twoFactorPage(), null)
        return signInScreen().also { it.signIn("user", "pass") }
    }

    private fun kotlinx.coroutines.test.TestScope.contentScreen(): BarsController {
        api.restored = true
        return BarsController(api, this).also { it.start() }
    }

    private class FakeApi : BarsApi {
        val calls = mutableListOf<String>()
        var lastPassword: String? = null

        var restored = false
        var loginResult: BarsLoginResult = BarsLoginResult.Rejected("unset", canRetryCode = false)
        var loginGate: CompletableDeferred<BarsLoginResult>? = null
        var codeResult: BarsLoginResult = BarsLoginResult.Rejected("unset", canRetryCode = false)
        var sendResult = BarsSendCodeResult(true, "sent")
        var marksError: Exception? = null
        var part = BarsMarksPart("Весна", listOf(discipline("Химия")))
        var partError: Exception? = null
        var ratingFor: BarsRating? = null
        var ratingError: Exception? = null
        var logoutError: Exception? = null

        override val studentId: String = "id"

        override suspend fun restoreSession(): Boolean {
            calls += "restoreSession"
            return restored
        }

        override suspend fun login(login: String, password: String, rememberMe: Boolean, sendCode: Boolean): BarsLoginResult {
            calls += "login($login)"
            lastPassword = password
            return loginGate?.await() ?: loginResult
        }

        override suspend fun sendCode(provider: BarsTwoFactorProvider?): BarsSendCodeResult {
            calls += "sendCode"
            return sendResult
        }

        override suspend fun submitCode(code: String): BarsLoginResult {
            calls += "submitCode($code)"
            return codeResult
        }

        override suspend fun loadMarksPage(studentId: String?): BarsMarksPage {
            calls += "loadMarksPage"
            marksError?.let { throw it }
            return marksPage()
        }

        override suspend fun loadSummary(semesterId: String?, studentId: String?): BarsSummaryPage {
            calls += "loadSummary($semesterId)"
            return BarsSummaryPage("Весна", emptyList(), null, emptyList())
        }

        override suspend fun loadSemesterMarks(semesterId: String, studentId: String?): BarsMarksPart {
            calls += "loadSemesterMarks($semesterId)"
            partError?.let { throw it }
            return part
        }

        override suspend fun loadRating(semesterId: String, studentId: String?): BarsRating? {
            calls += "loadRating($semesterId)"
            ratingError?.let { throw it }
            return ratingFor
        }

        override suspend fun logout() {
            calls += "logout"
            logoutError?.let { throw it }
        }
    }

    private companion object {

        fun discipline(name: String) = BarsDiscipline(name, "Орлов О.О.", "экзамен", "2ЗЕ", null, emptyList(), emptyList())

        fun student(id: String) = kekmech.ru.lib_bars_parser.BarsStudentLink(id, "Студент $id")

        fun marksPage() = BarsMarksPage(
            studentName = "Иванов Иван",
            studentGroup = "ТЕСТ-01",
            meta = emptyList(),
            semesters = listOf(
                BarsSemester("28", "Осень", selected = false),
                BarsSemester("27", "Весна", selected = true),
            ),
            disciplines = listOf(discipline("Химия")),
            rating = BarsRating("27", emptyList(), emptyList()),
        )

        fun twoFactorPage(): BarsTwoFactorPage {
            val totp = BarsTwoFactorProvider(BarsTwoFactorKind.TOTP, "btnSend", "5", 6, "Войти по одноразовому коду")
            val max = BarsTwoFactorProvider(BarsTwoFactorKind.MAX, "btnSend3", "3", 4, "Отправить повторно через МАКС")
            return BarsTwoFactorPage(totp, listOf(totp, max), "/bars_web/Auth/JSON_SendAF2_Code", null)
        }
    }
}
