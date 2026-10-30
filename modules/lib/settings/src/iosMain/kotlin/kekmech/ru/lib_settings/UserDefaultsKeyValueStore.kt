package kekmech.ru.lib_settings

import platform.Foundation.NSUserDefaults

/** The store of the iOS app: NSUserDefaults. */
public class UserDefaultsKeyValueStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueStore {

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getInt(key: String): Int? = if (has(key)) defaults.integerForKey(key).toInt() else null

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getBoolean(key: String): Boolean? = if (has(key)) defaults.boolForKey(key) else null

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    private fun has(key: String): Boolean = defaults.objectForKey(key) != null
}
