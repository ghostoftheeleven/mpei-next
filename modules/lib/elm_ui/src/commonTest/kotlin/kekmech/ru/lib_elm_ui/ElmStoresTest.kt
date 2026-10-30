package kekmech.ru.lib_elm_ui

import kekmech.ru.lib_navigator.Navigator
import kekmech.ru.lib_navigator.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import money.vivid.elmslie.core.store.Store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

internal class ElmStoresTest {

    private data class Page(val name: String) : Screen

    private class FakeStore(val name: String) : Store<String, String, Int> {
        var started = 0
        var stopped = 0
        val accepted = mutableListOf<String>()

        override val startEvent: String? = null
        override val scope: CoroutineScope = CoroutineScope(Job())
        override val states: StateFlow<Int> = MutableStateFlow(0)
        override val effects: Flow<String> = emptyFlow()

        override fun start(): Store<String, String, Int> = also { started++ }

        override fun stop() {
            stopped++
        }

        override fun accept(event: String) {
            accepted += event
        }
    }

    private val navigator = Navigator(Page("home"))
    private val created = mutableListOf<FakeStore>()

    private fun factory(name: String) = { FakeStore(name).also { created += it } }

    @Test
    fun `the store is created and started once for an entry`() {
        val entry = navigator.current
        val first = entry.elmStore(storeFactory = factory("home"))
        val second = entry.elmStore(storeFactory = factory("home"))

        assertSame(first, second)
        assertEquals(1, created.size)
        assertEquals(1, created.single().started)
    }

    @Test
    fun `the store survives the screens above`() {
        val home = navigator.current
        val store = home.elmStore(storeFactory = factory("home")) as FakeStore
        navigator.push(Page("a"))
        navigator.current.elmStore(storeFactory = factory("a"))
        navigator.pop()

        assertSame(store, home.elmStore(storeFactory = factory("home")))
        assertEquals(0, store.stopped)
    }

    @Test
    fun `the store is stopped when the entry leaves the stack`() {
        navigator.push(Page("a"))
        val store = navigator.current.elmStore(storeFactory = factory("a")) as FakeStore

        navigator.pop()

        assertEquals(1, store.stopped)
    }

    @Test
    fun `the same screen opened again gets a new store`() {
        navigator.push(Page("a"))
        val first = navigator.current.elmStore(storeFactory = factory("a"))
        navigator.pop()
        navigator.push(Page("a"))
        val second = navigator.current.elmStore(storeFactory = factory("a"))

        assertNotSame(first, second)
    }

    @Test
    fun `an entry can have several stores under different keys`() {
        val entry = navigator.current
        val one = entry.elmStore(key = "one", storeFactory = factory("one"))
        val two = entry.elmStore(key = "two", storeFactory = factory("two"))

        assertNotSame(one, two)
        navigator.newRoot(Page("login"))
        assertEquals(listOf(1, 1), created.map { it.stopped })
    }
}
