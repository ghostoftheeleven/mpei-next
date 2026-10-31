package kekmech.ru.ui_shimmer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalWindowInfo

/** The window of the app: the shimmer of all the views moves across it as one. */
@Composable
internal fun rememberShimmerBounds(): Rect {
    val container = LocalWindowInfo.current.containerSize
    return remember(container) {
        Rect(
            left = 0f,
            top = 0f,
            right = container.width.toFloat(),
            bottom = container.height.toFloat(),
        )
    }
}
