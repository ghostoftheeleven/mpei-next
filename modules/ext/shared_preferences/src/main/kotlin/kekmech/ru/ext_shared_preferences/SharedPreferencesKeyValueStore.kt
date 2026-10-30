package kekmech.ru.ext_shared_preferences

import android.content.SharedPreferences
import kekmech.ru.lib_settings.KeyValueStore

/** Adapts SharedPreferences to the multiplatform store: the values stay in the same file, nothing has to migrate. */
public fun SharedPreferences.asKeyValueStore(): KeyValueStore = SharedPreferencesKeyValueStore(this)

private class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getInt(key: String): Int? =
        if (preferences.contains(key)) preferences.getInt(key, 0) else null

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean? =
        if (preferences.contains(key)) preferences.getBoolean(key, false) else null

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
