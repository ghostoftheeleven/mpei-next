package kekmech.ru.lib_settings

/** A store that lives as long as the object does: for tests and for the places where nothing must be persisted. */
public class InMemoryKeyValueStore : KeyValueStore {

    private val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getInt(key: String): Int? = values[key] as? Int

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
