package kekmech.ru.lib_bars_client

/** A cookie of the BARS session as it is kept between the launches of the app. */
data class BarsCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    /** When the cookie dies, milliseconds since the epoch; null for a session cookie. */
    val expiresAtMillis: Long?,
    val secure: Boolean,
    val httpOnly: Boolean,
)

/**
 * What the app keeps to stay signed in: the cookies of the session and the student. There is no login and no password
 * in it, a dead session means signing in again (and entering the code again).
 */
data class BarsStoredSession(
    val cookies: List<BarsCookie>,
    val studentId: String?,
)

/**
 * The place where the session survives the end of the app. The cookies are bearer secrets: whoever has them is
 * signed in as the student, so an implementation has to be a protected storage (the Keychain on iOS).
 */
interface BarsSessionStore {
    suspend fun load(): BarsStoredSession?
    suspend fun save(session: BarsStoredSession)
    suspend fun clear()
}

/** Keeps the session in memory: it is gone with the process. For tests and for a client that must not remember. */
class InMemoryBarsSessionStore : BarsSessionStore {

    private var session: BarsStoredSession? = null

    override suspend fun load(): BarsStoredSession? = session

    override suspend fun save(session: BarsStoredSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}

/**
 * The text form of [BarsStoredSession] for the stores that keep bytes. Plain lines, no dependencies:
 *
 * ```
 * BARS-SESSION v1
 * student<TAB>id
 * cookie<TAB>name<TAB>value<TAB>domain<TAB>path<TAB>expires or -<TAB>secure 0/1<TAB>httpOnly 0/1
 * ```
 *
 * The tabs, the line breaks and the percent sign inside a field are written as %09, %0A, %0D and %25.
 */
object BarsSessionCodec {

    private const val HEADER = "BARS-SESSION v1"
    private const val NONE = "-"

    fun encode(session: BarsStoredSession): String = buildString {
        append(HEADER).append('\n')
        append("student\t").append(escape(session.studentId.orEmpty())).append('\n')
        for (cookie in session.cookies) {
            append("cookie")
            for (field in listOf(
                cookie.name,
                cookie.value,
                cookie.domain,
                cookie.path,
                cookie.expiresAtMillis?.toString() ?: NONE,
                if (cookie.secure) "1" else "0",
                if (cookie.httpOnly) "1" else "0",
            )) {
                append('\t').append(escape(field))
            }
            append('\n')
        }
    }

    /** Null if [text] is not a session of this version: a store must not crash on what it finds in the storage. */
    fun decode(text: String): BarsStoredSession? {
        val lines = text.split('\n').filter { it.isNotEmpty() }
        if (lines.firstOrNull() != HEADER) return null

        var studentId: String? = null
        val cookies = mutableListOf<BarsCookie>()
        for (line in lines.drop(1)) {
            val fields = line.split('\t')
            when (fields.first()) {
                "student" -> studentId = fields.getOrNull(1)?.let(::unescape)?.ifEmpty { null }
                "cookie" -> {
                    if (fields.size != COOKIE_FIELDS) return null
                    val expires = fields[5].let { if (it == NONE) null else it.toLongOrNull() ?: return null }
                    cookies += BarsCookie(
                        name = unescape(fields[1]),
                        value = unescape(fields[2]),
                        domain = unescape(fields[3]),
                        path = unescape(fields[4]),
                        expiresAtMillis = expires,
                        secure = fields[6] == "1",
                        httpOnly = fields[7] == "1",
                    )
                }
                else -> return null
            }
        }
        return BarsStoredSession(cookies, studentId)
    }

    private fun escape(text: String): String =
        text.replace("%", "%25").replace("\t", "%09").replace("\n", "%0A").replace("\r", "%0D")

    private fun unescape(text: String): String =
        text.replace("%09", "\t").replace("%0A", "\n").replace("%0D", "\r").replace("%25", "%")

    private const val COOKIE_FIELDS = 8
}
