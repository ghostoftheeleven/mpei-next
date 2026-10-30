package kekmech.ru.lib_navigator

/**
 * The objects that live as long as a [BackStackEntry] is in the stack, like a ViewModel does: the ELM store of the
 * screen is kept here. When the entry leaves the stack, every object gets its `onCleared` call, in the reverse order
 * of creation.
 *
 * Not thread-safe: call it from the main thread.
 */
public class EntryScope internal constructor() {

    private class Holder(val value: Any, val onCleared: (Any) -> Unit)

    private val holders = LinkedHashMap<String, Holder>()

    /** The object under [key]; [create] is called the first time only. */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> getOrPut(key: String, onCleared: (T) -> Unit = {}, create: () -> T): T {
        val holder = holders.getOrPut(key) { Holder(create(), onCleared as (Any) -> Unit) }
        return holder.value as T
    }

    internal fun clear() {
        val toClear = holders.values.toList().asReversed()
        holders.clear()
        toClear.forEach { it.onCleared(it.value) }
    }
}
