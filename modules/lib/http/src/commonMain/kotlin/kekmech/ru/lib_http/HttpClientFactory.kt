package kekmech.ru.lib_http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** What the backend learns about the app: the same headers the Android client has always sent. */
public class ClientIdentity(
    public val userAgent: String,
    public val deviceId: String,
    public val deviceOs: String,
    public val deviceLocale: String,
    public val appVersion: String,
)

public val DefaultJson: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

private const val REQUEST_TIMEOUT_MILLIS = 15_000L

/**
 * The HTTP client of the app. Any status other than 2xx is an exception (ClientRequestException,
 * ServerResponseException), and a response is decoded as JSON with [DefaultJson].
 *
 * @param engine the engine; if it is `null`, the engine of the platform is used (OkHttp, Darwin).
 * @param logger gets the log of the requests; the log is off when it is `null`.
 */
public fun createHttpClient(
    identity: ClientIdentity,
    engine: HttpClientEngine? = null,
    logger: ((String) -> Unit)? = null,
    logLevel: LogLevel = LogLevel.INFO,
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = true
        install(ContentNegotiation) { json(DefaultJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, identity.userAgent)
            header("X-Device-Id", identity.deviceId)
            header("X-Device-Os", identity.deviceOs)
            header("X-Device-Locale", identity.deviceLocale)
            header("X-App-Version", identity.appVersion)
        }
        if (logger != null) {
            install(Logging) {
                this.logger = object : Logger {
                    override fun log(message: String) = logger(message)
                }
                level = logLevel
            }
        }
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}
