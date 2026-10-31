package kekmech.ru.mpeiapp

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kekmech.ru.feature_app_update_api.ForceUpdateChecker
import kekmech.ru.lib_feature_toggles.RemoteConfigWrapper
import kekmech.ru.lib_network.BackendServiceUrl
import kekmech.ru.lib_network.ServiceUrlResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * The settings that can be changed without a new version of the app (the force update, the toggles): the file
 * `statics/app/config.json` of the repository. The last answer is kept on the device and is used when the network fails; what is
 * in neither of them is the default of the app.
 */
class RepoRemoteConfig(
    context: Context,
    private val client: HttpClient,
    defaults: Map<String, String>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigWrapper {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var values: Map<String, String> = defaults + readSaved()

    override fun getUntyped(name: String): String = values[name].orEmpty()

    override fun getAll(): Map<String, String> = values

    /** Asks the repository for the file; the new values are used from the next read. */
    fun refresh() {
        scope.launch {
            try {
                val url = ServiceUrlResolver.resolve(BackendServiceUrl.APP_CONFIG) + "config.json"
                // GitHub serves the file as text/plain, so it is read as text
                val text = client.get(url).bodyAsText()
                val fresh = json.decodeFromString(serializer, text)
                values = values + fresh
                preferences.edit().putString(KEY_SAVED, text).apply()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.w(e, "The settings of the app were not loaded, the last ones are used")
            }
        }
    }

    private fun readSaved(): Map<String, String> {
        val text = preferences.getString(KEY_SAVED, null) ?: return emptyMap()
        return runCatching { json.decodeFromString(serializer, text) }.getOrDefault(emptyMap())
    }

    companion object {

        private const val PREFERENCES = "repo_remote_config"
        private const val KEY_SAVED = "config_json"

        /** What the app does when it has not got the file: no force update, no toggles. */
        fun defaults(versionName: String) = mapOf(
            ForceUpdateChecker.KEY_UPDATE_REQUIRED to "false",
            ForceUpdateChecker.KEY_CURRENT_VERSION to versionName,
            ForceUpdateChecker.KEY_UPDATE_URL to "https://play.google.com/store/apps/details?id=ru.mpei.hub",
            ForceUpdateChecker.KEY_UPDATE_DESCRIPTION to "",
        )
    }
}
