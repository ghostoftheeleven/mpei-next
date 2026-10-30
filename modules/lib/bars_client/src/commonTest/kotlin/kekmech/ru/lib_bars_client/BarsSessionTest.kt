package kekmech.ru.lib_bars_client

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarsSessionTest {

    private var clock = 1_000_000L
    private val jar = BarsCookieJar(now = { clock })
    private val bars = Url("https://bars.mpei.ru/bars_web/ST_Study/Main/Main")

    @Test
    fun `the session survives the text form`() {
        val session = BarsStoredSession(
            cookies = listOf(
                BarsCookie("ASP.NET_SessionId", "abc", "bars.mpei.ru", "/", null, secure = false, httpOnly = true),
                BarsCookie("auth", "a\tb%c\nd\re=f;g", "bars.mpei.ru", "/bars_web", 1_700_000_000_000L, secure = true, httpOnly = false),
            ),
            studentId = "11111111-2222-3333-4444-555555555555",
        )

        assertEquals(session, BarsSessionCodec.decode(BarsSessionCodec.encode(session)))
    }

    @Test
    fun `a session without a student and without cookies survives the text form`() {
        val session = BarsStoredSession(cookies = emptyList(), studentId = null)

        assertEquals(session, BarsSessionCodec.decode(BarsSessionCodec.encode(session)))
    }

    @Test
    fun `what is not a session is not read as one`() {
        assertNull(BarsSessionCodec.decode(""))
        assertNull(BarsSessionCodec.decode("hello"))
        assertNull(BarsSessionCodec.decode("BARS-SESSION v2\nstudent\t"))
        assertNull(BarsSessionCodec.decode("BARS-SESSION v1\ncookie\tname\tvalue"))
        assertNull(BarsSessionCodec.decode("BARS-SESSION v1\ncookie\ta\tb\tc\t/\tnot-a-number\t0\t0"))
        assertNull(BarsSessionCodec.decode("BARS-SESSION v1\nsomething\telse"))
    }

    @Test
    fun `a cookie goes back to the host and the path it came from`() = runTest {
        jar.addCookie(bars, Cookie("sess", "1", path = "/bars_web"))

        assertEquals(listOf("sess"), jar.get(Url("https://bars.mpei.ru/bars_web/x")).map { it.name })
        assertEquals(listOf("sess"), jar.get(Url("https://bars.mpei.ru/bars_web")).map { it.name })
        assertTrue(jar.get(Url("https://bars.mpei.ru/other")).isEmpty())
        assertTrue(jar.get(Url("https://bars.mpei.ru/bars_web_other")).isEmpty())
        assertTrue(jar.get(Url("https://evil.example.com/bars_web")).isEmpty())
    }

    @Test
    fun `a cookie without a path takes the directory of the request`() = runTest {
        jar.addCookie(bars, Cookie("sess", "1"))

        assertEquals("/bars_web/ST_Study/Main", jar.snapshot().single().path)
        assertEquals(1, jar.get(Url("https://bars.mpei.ru/bars_web/ST_Study/Main/Summary")).size)
        assertTrue(jar.get(Url("https://bars.mpei.ru/bars_web/")).isEmpty())
    }

    @Test
    fun `a cookie of the domain goes to its subdomains`() = runTest {
        jar.addCookie(bars, Cookie("sess", "1", domain = ".mpei.ru", path = "/"))

        assertEquals(1, jar.get(Url("https://bars.mpei.ru/")).size)
        assertEquals(1, jar.get(Url("https://ts.mpei.ru/")).size)
        assertTrue(jar.get(Url("https://notmpei.ru/")).isEmpty())
    }

    @Test
    fun `a secure cookie never goes over http`() = runTest {
        jar.addCookie(bars, Cookie("sess", "1", path = "/", secure = true))

        assertEquals(1, jar.get(Url("https://bars.mpei.ru/")).size)
        assertTrue(jar.get(Url("http://bars.mpei.ru/")).isEmpty())
    }

    @Test
    fun `the same cookie is replaced, another one is added`() = runTest {
        jar.addCookie(bars, Cookie("sess", "1", path = "/"))
        jar.addCookie(bars, Cookie("sess", "2", path = "/"))
        jar.addCookie(bars, Cookie("auth", "x", path = "/"))

        assertEquals(mapOf("sess" to "2", "auth" to "x"), jar.snapshot().associate { it.name to it.value })
    }

    @Test
    fun `a cookie dies when it expires`() = runTest {
        jar.addCookie(bars, Cookie("short", "1", path = "/", maxAge = 10))
        jar.addCookie(bars, Cookie("dated", "1", path = "/", expires = GMTDate(clock + 60_000)))
        jar.addCookie(bars, Cookie("forever", "1", path = "/"))
        assertEquals(setOf("short", "dated", "forever"), jar.snapshot().map { it.name }.toSet())

        clock += 11_000
        assertEquals(setOf("dated", "forever"), jar.get(bars).map { it.name }.toSet())

        clock += 60_000
        assertEquals(setOf("forever"), jar.snapshot().map { it.name }.toSet())
    }

    @Test
    fun `a cookie that comes already expired deletes the one that was kept`() = runTest {
        jar.addCookie(bars, Cookie("auth", "1", path = "/"))

        jar.addCookie(bars, Cookie("auth", "", path = "/", expires = GMTDate(clock - 1000)))

        assertTrue(jar.snapshot().isEmpty())
        assertTrue(jar.get(bars).isEmpty())
    }

    @Test
    fun `the owner is told about every change`() = runTest {
        var changes = 0
        jar.onChanged = { changes++ }

        jar.addCookie(bars, Cookie("a", "1", path = "/"))
        jar.addCookie(bars, Cookie("a", "2", path = "/"))
        jar.addCookie(bars, Cookie(" ", "ignored", path = "/"))

        assertEquals(2, changes)
    }

    @Test
    fun `the cookies of a session that was kept come back`() = runTest {
        val kept = BarsCookieJar(
            initial = listOf(BarsCookie("sess", "1", "bars.mpei.ru", "/", null, secure = false, httpOnly = true)),
            now = { clock },
        )

        val cookie = assertNotNull(kept.get(bars).singleOrNull())
        assertEquals("sess", cookie.name)
        assertEquals("1", cookie.value)
    }

    @Test
    fun `the in-memory store keeps and forgets`() = runTest {
        val store = InMemoryBarsSessionStore()
        assertNull(store.load())

        val session = BarsStoredSession(emptyList(), "id")
        store.save(session)
        assertEquals(session, store.load())

        store.clear()
        assertNull(store.load())
    }
}
