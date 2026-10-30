package kekmech.ru.lib_navigator_compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import kekmech.ru.lib_navigator.BackStackEntry
import kekmech.ru.lib_navigator.Navigator

/** The navigator of the screen that is being drawn: `LocalNavigator.current.push(...)`. */
public val LocalNavigator: ProvidableCompositionLocal<Navigator> =
    compositionLocalOf { error("No Navigator: draw the screens inside NavHost") }

/** The entry of the screen that is being drawn; its `scope` holds the objects of the screen (the ELM store). */
public val LocalBackStackEntry: ProvidableCompositionLocal<BackStackEntry> =
    compositionLocalOf { error("No BackStackEntry: draw the screens inside NavHost") }

/**
 * Draws the top entry of the back stack of [navigator]. A new screen slides in from the right, a closed one slides
 * out to the right. The state saved with `rememberSaveable` survives the screens above, and is dropped when the entry
 * leaves the stack.
 */
@Composable
public fun NavHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: @Composable (BackStackEntry) -> Unit,
) {
    val stack by navigator.backStack.collectAsState()
    val saveableStateHolder = rememberSaveableStateHolder()

    DisposableEffect(navigator, saveableStateHolder) {
        val remove = navigator.addOnRemovedListener { saveableStateHolder.removeState(it.id) }
        onDispose(remove)
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        AnimatedContent(
            targetState = stack.last(),
            modifier = modifier,
            transitionSpec = {
                transition(opening = targetState.id > initialState.id).using(SizeTransform(clip = false))
            },
            label = "NavHost",
        ) { entry ->
            saveableStateHolder.SaveableStateProvider(entry.id) {
                CompositionLocalProvider(LocalBackStackEntry provides entry) {
                    content(entry)
                }
            }
        }
    }
}

private const val SLIDE_FRACTION = 3

private fun transition(opening: Boolean): ContentTransform {
    val transform = if (opening) {
        (slideInHorizontally { it / SLIDE_FRACTION } + fadeIn()) togetherWith
            (slideOutHorizontally { -it / SLIDE_FRACTION } + fadeOut())
    } else {
        (slideInHorizontally { -it / SLIDE_FRACTION } + fadeIn()) togetherWith
            (slideOutHorizontally { it / SLIDE_FRACTION } + fadeOut())
    }
    return transform
}
