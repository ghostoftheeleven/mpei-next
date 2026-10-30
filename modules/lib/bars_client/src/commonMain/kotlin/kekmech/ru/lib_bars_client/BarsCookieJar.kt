package kekmech.ru.lib_bars_client

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.URLProtocol
import io.ktor.util.date.GMTDate
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The cookies of the session for the Ktor client. It does what the standard storage does (the domain, the path, the
 * expiry, "secure") and lets the owner know about every change, so the session can be saved. It does not know how.
 *
 * @param now the clock, milliseconds since the epoch
 */
internal class BarsCookieJar(
    initial: List<BarsCookie> = emptyList(),
    private val now: () -> Long = { getTimeMillis() },
) : CookiesStorage {

    private val lock = Mutex()
    private val cookies = initial.toMutableList()

    /** Called after the cookies have changed. */
    var onChanged: (suspend () -> Unit)? = null

    /** The cookies that are alive now. */
    fun snapshot(): List<BarsCookie> {
        val time = now()
        return cookies.filter { !it.isDead(time) }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = lock.withLock {
        val time = now()
        cookies.removeAll { it.isDead(time) }
        cookies.filter { it.matches(requestUrl) }.map { it.toKtor() }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name.isBlank()) return

        val domain = (cookie.domain ?: requestUrl.host).removePrefix(".").lowercase()
        val path = cookie.path?.takeIf { it.startsWith("/") } ?: defaultPath(requestUrl.encodedPath)
        val time = now()
        // maxAge 0 means that the cookie has no Max-Age; a Max-Age that deletes a cookie comes with an expiry in the past
        val expires = cookie.expires
        val expiresAt = when {
            cookie.maxAge > 0 -> time + cookie.maxAge * MILLIS_IN_SECOND
            expires != null -> expires.timestamp
            else -> null
        }

        lock.withLock {
            cookies.removeAll { it.name == cookie.name && it.domain == domain && it.path == path }
            if (expiresAt == null || expiresAt > time) {
                cookies += BarsCookie(
                    name = cookie.name,
                    value = cookie.value,
                    domain = domain,
                    path = path,
                    expiresAtMillis = expiresAt,
                    secure = cookie.secure,
                    httpOnly = cookie.httpOnly,
                )
            }
        }
        onChanged?.invoke()
    }

    override fun close() = Unit

    private fun BarsCookie.isDead(time: Long): Boolean = expiresAtMillis != null && expiresAtMillis <= time

    private fun BarsCookie.matches(url: Url): Boolean {
        val host = url.host.lowercase()
        if (host != domain && !host.endsWith(".$domain")) return false
        if (secure && url.protocol != URLProtocol.HTTPS) return false
        val requestPath = url.encodedPath.ifEmpty { "/" }
        return requestPath == path ||
            (requestPath.startsWith(path) && (path.endsWith("/") || requestPath[path.length] == '/'))
    }

    private fun BarsCookie.toKtor() = Cookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        secure = secure,
        httpOnly = httpOnly,
        expires = expiresAtMillis?.let { GMTDate(it) },
    )

    /** RFC 6265: the path of a cookie without a Path is the directory of the request. */
    private fun defaultPath(requestPath: String): String {
        val last = requestPath.lastIndexOf('/')
        return if (!requestPath.startsWith("/") || last <= 0) "/" else requestPath.substring(0, last)
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1000L
    }
}
