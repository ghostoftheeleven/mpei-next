package kekmech.ru.lib_elm_ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kekmech.ru.lib_navigator.BackStackEntry
import kekmech.ru.lib_navigator_compose.LocalBackStackEntry
import kotlinx.coroutines.flow.Flow
import money.vivid.elmslie.core.store.Store

private const val DEFAULT_STORE_KEY = "store"

/**
 * The store of [this] screen: [storeFactory] is called once, the store is started right away and stopped when the
 * entry leaves the back stack. A screen with several stores gives them different [key]s.
 */
public fun <Event : Any, Effect : Any, State : Any> BackStackEntry.elmStore(
    key: String = DEFAULT_STORE_KEY,
    storeFactory: () -> Store<Event, Effect, State>,
): Store<Event, Effect, State> =
    scope.getOrPut(key, onCleared = { store: Store<Event, Effect, State> -> store.stop() }) {
        storeFactory().also { it.start() }
    }

/** The store of the screen that is being drawn (see [elmStore]). */
@Composable
public fun <Event : Any, Effect : Any, State : Any> rememberElmStore(
    key: String = DEFAULT_STORE_KEY,
    storeFactory: () -> Store<Event, Effect, State>,
): Store<Event, Effect, State> {
    val entry = LocalBackStackEntry.current
    return remember(entry.id, key) { entry.elmStore(key, storeFactory) }
}

/**
 * The content of a screen driven by a store: the current state, a function that sends an event, and the flow of
 * effects (handle it with [EffectHandler]).
 */
@Composable
public fun <Event : Any, Effect : Any, State : Any> ElmContent(
    key: String = DEFAULT_STORE_KEY,
    storeFactory: () -> Store<Event, Effect, State>,
    content: @Composable (onAccept: (Event) -> Unit, state: State, effects: Flow<Effect>) -> Unit,
) {
    val store = rememberElmStore(key, storeFactory)
    val state by store.states.collectAsState()
    val onAccept = remember(store) { { event: Event -> store.accept(event) } }
    content(onAccept, state, store.effects)
}

/** Calls [effectHandler] for every effect of the store while the screen is on. */
@Composable
public fun <Effect : Any> EffectHandler(
    effects: Flow<Effect>,
    effectHandler: (Effect) -> Unit,
) {
    val currentEffectHandler by rememberUpdatedState(effectHandler)
    LaunchedEffect(effects) {
        effects.collect { currentEffectHandler(it) }
    }
}
