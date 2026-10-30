package kekmech.ru.lib_settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class PreferencesTest {

    private val store = InMemoryKeyValueStore()

    private var name by store.string("name", "nobody")
    private var counter by store.int("counter", 7)
    private var enabled by store.boolean("enabled", true)
    private var group by store.typed(
        key = "group",
        toString = { it.number.toString() },
        fromString = { Group(it.toInt()) },
        defaultValue = Group(0),
    )

    private data class Group(val number: Int)

    @Test
    fun `defaults are returned until something is written`() {
        assertEquals("nobody", name)
        assertEquals(7, counter)
        assertEquals(true, enabled)
        assertEquals(Group(0), group)
    }

    @Test
    fun `written values are read back`() {
        name = "Анна"
        counter = 42
        enabled = false
        group = Group(15)
        assertEquals("Анна", name)
        assertEquals(42, counter)
        assertEquals(false, enabled)
        assertEquals(Group(15), group)
    }

    @Test
    fun `typed value that cannot be parsed gives the default`() {
        store.putString("group", "not a number")
        assertEquals(Group(0), group)
    }

    @Test
    fun `store returns null for an unknown key and for a key of another type`() {
        assertNull(store.getString("missing"))
        store.putInt("counter", 1)
        assertNull(store.getString("counter"))
        store.remove("counter")
        assertNull(store.getInt("counter"))
    }
}
