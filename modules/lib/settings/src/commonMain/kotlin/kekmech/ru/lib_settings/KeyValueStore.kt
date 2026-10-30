package kekmech.ru.lib_settings

/**
 * Persistent key-value storage. A getter returns `null` when the key has never been written, so the caller decides
 * what the default is.
 */
public interface KeyValueStore {

    public fun getString(key: String): String?

    public fun putString(key: String, value: String)

    public fun getInt(key: String): Int?

    public fun putInt(key: String, value: Int)

    public fun getBoolean(key: String): Boolean?

    public fun putBoolean(key: String, value: Boolean)

    public fun remove(key: String)
}
