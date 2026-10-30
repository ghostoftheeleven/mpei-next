package kekmech.ru.lib_settings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** `var name by store.string("key", "default")`: a property that is read from and written to the store. */
public fun KeyValueStore.string(key: String, defaultValue: String = ""): ReadWriteProperty<Any, String> =
    object : ReadWriteProperty<Any, String> {
        override fun getValue(thisRef: Any, property: KProperty<*>): String = getString(key) ?: defaultValue

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) = putString(key, value)
    }

public fun KeyValueStore.int(key: String, defaultValue: Int = 0): ReadWriteProperty<Any, Int> =
    object : ReadWriteProperty<Any, Int> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Int = getInt(key) ?: defaultValue

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) = putInt(key, value)
    }

public fun KeyValueStore.boolean(key: String, defaultValue: Boolean = false): ReadWriteProperty<Any, Boolean> =
    object : ReadWriteProperty<Any, Boolean> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean = getBoolean(key) ?: defaultValue

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) = putBoolean(key, value)
    }

/**
 * A value that is kept as a string. A value that cannot be read (never written, or the format has changed)
 * gives [defaultValue].
 */
public fun <T> KeyValueStore.typed(
    key: String,
    toString: (T) -> String,
    fromString: (String) -> T,
    defaultValue: T,
): ReadWriteProperty<Any, T> =
    object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T =
            runCatching { fromString(getString(key).orEmpty()) }.getOrElse { defaultValue }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            runCatching { putString(key, toString(value)) }
        }
    }
