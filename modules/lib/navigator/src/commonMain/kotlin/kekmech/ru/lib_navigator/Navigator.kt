package kekmech.ru.lib_navigator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A destination. Implement it with a data class (or an object) that holds the arguments of the screen. */
public interface Screen

/**
 * A screen in the back stack. The same [Screen] can be in the stack twice, so the entry has its own [id]: the state
 * of the screen (its ELM store, the saved state of the UI) belongs to the entry, not to the [Screen] value.
 */
public class BackStackEntry internal constructor(
    public val id: Long,
    public val screen: Screen,
) {

    /** The objects of this screen that live while the entry is in the stack. */
    public val scope: EntryScope = EntryScope()
}

/**
 * The back stack. The stack is never empty: the root stays until [newRoot] replaces it.
 *
 * Not thread-safe: call it from the main thread, like the Router of the Android app.
 */
public class Navigator(root: Screen) {

    private var lastId = 0L

    private val stack = MutableStateFlow(listOf(newEntry(root)))
    private val removedListeners = mutableListOf<(BackStackEntry) -> Unit>()

    public val backStack: StateFlow<List<BackStackEntry>> = stack.asStateFlow()

    public val current: BackStackEntry get() = stack.value.last()

    public val canPop: Boolean get() = stack.value.size > 1

    /** AddScreenForward: opens the screen above the current one. */
    public fun push(screen: Screen) {
        change { it + newEntry(screen) }
    }

    /** ReplaceScreen: the screen takes the place of the current one. */
    public fun replace(screen: Screen) {
        change { it.dropLast(1) + newEntry(screen) }
    }

    /** NewRoot: clears the stack, [screen] becomes its only entry. */
    public fun newRoot(screen: Screen) {
        change { listOf(newEntry(screen)) }
    }

    /** PopBackStack. Returns `false` when there is nothing to pop (only the root is left). */
    public fun pop(): Boolean = change { if (it.size > 1) it.dropLast(1) else it }

    /**
     * PopUntil: closes the screens above the topmost screen that satisfies [predicate]; with [inclusive] that screen
     * is closed too (the root is never closed). Returns `false`, and changes nothing, if there is no such screen.
     */
    public fun popUntil(inclusive: Boolean = false, predicate: (Screen) -> Boolean): Boolean {
        val index = stack.value.indexOfLast { predicate(it.screen) }
        if (index < 0) return false
        change { entries ->
            val keep = if (inclusive) index else index + 1
            entries.take(keep.coerceAtLeast(1))
        }
        return true
    }

    /** Calls [listener] for every entry that has left the stack; the returned function removes the listener. */
    public fun addOnRemovedListener(listener: (BackStackEntry) -> Unit): () -> Unit {
        removedListeners += listener
        return { removedListeners -= listener }
    }

    private fun newEntry(screen: Screen) = BackStackEntry(id = ++lastId, screen = screen)

    private fun change(transform: (List<BackStackEntry>) -> List<BackStackEntry>): Boolean {
        val old = stack.value
        val new = transform(old)
        if (new == old) return false
        stack.value = new
        old.asReversed().filter { it !in new }.forEach { removed ->
            removed.scope.clear()
            removedListeners.toList().forEach { it(removed) }
        }
        return true
    }
}
