package kekmech.ru.lib_navigator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class EntryScopeTest {

    private data class Page(val name: String) : Screen

    private class Resource(val name: String)

    private val navigator = Navigator(Page("home"))
    private val cleared = mutableListOf<String>()

    private fun BackStackEntry.resource(key: String) =
        scope.getOrPut(key, onCleared = { r: Resource -> cleared += r.name }) { Resource("${(screen as Page).name}/$key") }

    @Test
    fun `an object is created once for a key`() {
        val entry = navigator.current
        assertSame(entry.resource("store"), entry.resource("store"))
    }

    @Test
    fun `different keys and different entries have different objects`() {
        navigator.push(Page("a"))
        val a = navigator.current
        val home = navigator.backStack.value.first()
        assertEquals(false, a.resource("one") === a.resource("two"))
        assertEquals(false, a.resource("one") === home.resource("one"))
    }

    @Test
    fun `the object lives while the entry is in the stack`() {
        val home = navigator.current
        val first = home.resource("store")
        navigator.push(Page("a"))
        navigator.pop()
        assertSame(first, home.resource("store"))
        assertEquals(emptyList(), cleared)
    }

    @Test
    fun `pop clears the objects of the closed entry only`() {
        navigator.current.resource("store")
        navigator.push(Page("a"))
        navigator.current.resource("store")

        navigator.pop()

        assertEquals(listOf("a/store"), cleared)
    }

    @Test
    fun `objects are cleared in the reverse order of creation`() {
        navigator.push(Page("a"))
        navigator.current.resource("first")
        navigator.current.resource("second")

        navigator.pop()

        assertEquals(listOf("a/second", "a/first"), cleared)
    }

    @Test
    fun `newRoot clears every entry`() {
        navigator.current.resource("store")
        navigator.push(Page("a"))
        navigator.current.resource("store")

        navigator.newRoot(Page("login"))

        assertEquals(listOf("a/store", "home/store"), cleared)
    }
}
