package kekmech.ru.lib_navigator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class NavigatorTest {

    private data class Page(val name: String) : Screen

    private val home = Page("home")
    private val navigator = Navigator(home)

    private fun Navigator.names() = backStack.value.map { (it.screen as Page).name }

    @Test
    fun `stack starts with the root`() {
        assertEquals(listOf("home"), navigator.names())
        assertFalse(navigator.canPop)
    }

    @Test
    fun `push opens a screen above`() {
        navigator.push(Page("a"))
        navigator.push(Page("b"))
        assertEquals(listOf("home", "a", "b"), navigator.names())
        assertEquals(Page("b"), navigator.current.screen)
        assertTrue(navigator.canPop)
    }

    @Test
    fun `pop closes the top screen and keeps the root`() {
        navigator.push(Page("a"))
        assertTrue(navigator.pop())
        assertEquals(listOf("home"), navigator.names())
        assertFalse(navigator.pop())
        assertEquals(listOf("home"), navigator.names())
    }

    @Test
    fun `replace takes the place of the top screen`() {
        navigator.push(Page("a"))
        navigator.replace(Page("b"))
        assertEquals(listOf("home", "b"), navigator.names())
    }

    @Test
    fun `replace on the root replaces the root`() {
        navigator.replace(Page("a"))
        assertEquals(listOf("a"), navigator.names())
    }

    @Test
    fun `newRoot clears the stack`() {
        navigator.push(Page("a"))
        navigator.push(Page("b"))
        navigator.newRoot(Page("login"))
        assertEquals(listOf("login"), navigator.names())
        assertFalse(navigator.canPop)
    }

    @Test
    fun `popUntil stops at the screen`() {
        navigator.push(Page("a"))
        navigator.push(Page("b"))
        navigator.push(Page("c"))
        assertTrue(navigator.popUntil { (it as Page).name == "a" })
        assertEquals(listOf("home", "a"), navigator.names())
    }

    @Test
    fun `popUntil inclusive closes that screen too`() {
        navigator.push(Page("a"))
        navigator.push(Page("b"))
        assertTrue(navigator.popUntil(inclusive = true) { (it as Page).name == "a" })
        assertEquals(listOf("home"), navigator.names())
    }

    @Test
    fun `popUntil inclusive never closes the root`() {
        navigator.push(Page("a"))
        assertTrue(navigator.popUntil(inclusive = true) { (it as Page).name == "home" })
        assertEquals(listOf("home"), navigator.names())
    }

    @Test
    fun `popUntil without a match changes nothing`() {
        navigator.push(Page("a"))
        assertFalse(navigator.popUntil { (it as Page).name == "zzz" })
        assertEquals(listOf("home", "a"), navigator.names())
    }

    @Test
    fun `the same screen twice are two entries with different ids`() {
        navigator.push(Page("a"))
        navigator.push(Page("a"))
        val (_, first, second) = navigator.backStack.value
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `listener gets every removed entry, the top one first`() {
        navigator.push(Page("a"))
        navigator.push(Page("b"))
        val removed = mutableListOf<String>()
        navigator.addOnRemovedListener { removed += (it.screen as Page).name }

        navigator.newRoot(Page("login"))

        assertEquals(listOf("b", "a", "home"), removed)
    }

    @Test
    fun `listener is not called when nothing leaves the stack and can be removed`() {
        val removed = mutableListOf<String>()
        val remove = navigator.addOnRemovedListener { removed += (it.screen as Page).name }

        navigator.push(Page("a"))
        navigator.pop()
        remove()
        navigator.push(Page("b"))
        navigator.pop()

        assertEquals(listOf("a"), removed)
    }
}
