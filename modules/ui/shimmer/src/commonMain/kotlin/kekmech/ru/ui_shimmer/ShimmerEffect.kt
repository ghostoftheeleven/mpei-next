package kekmech.ru.ui_shimmer

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSaveLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class ShimmerEffect(
    private val shimmerWidthPx: Float,
) {

    private val animatedState = Animatable(0f)

    private val emptyPaint = Paint()

    private val rotationRadians = ShimmerTokens.Rotation / HALF_TURN_DEGREES * PI.toFloat()

    internal suspend fun startAnimation() {
        animatedState.animateTo(
            targetValue = 1f,
            animationSpec = ShimmerTokens.AnimationSpec,
        )
    }

    fun ContentDrawScope.draw(shimmerArea: ShimmerArea) = with(shimmerArea) {
        if (shimmerBounds.isEmpty || viewBounds.isEmpty) return

        val progress = animatedState.value
        val traversal = -translationDistance / 2 + translationDistance * progress + pivotPoint.x

        // The band is a horizontal gradient that is moved by [traversal] and then turned around the pivot, so it
        // is drawn as a gradient between the two turned end points: outside of the band the alpha stays at 0.5
        val brush = Brush.linearGradient(
            0.0f to Color.Black.copy(alpha = EDGE_ALPHA),
            0.5f to Color.Black,
            1.0f to Color.Black.copy(alpha = EDGE_ALPHA),
            start = transformed(Offset(-shimmerWidthPx / 2, 0f), traversal, pivotPoint),
            end = transformed(Offset(shimmerWidthPx / 2, 0f), traversal, pivotPoint),
            tileMode = TileMode.Clamp,
        )

        val drawArea = size.toRect()
        drawIntoCanvas { canvas ->
            canvas.withSaveLayer(
                bounds = drawArea,
                paint = emptyPaint,
            ) {
                drawContent()
                drawRect(
                    brush = brush,
                    topLeft = drawArea.topLeft,
                    size = drawArea.size,
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }

    private fun transformed(point: Offset, traversal: Float, pivot: Offset): Offset {
        val dx = point.x + traversal - pivot.x
        val dy = point.y - pivot.y
        val cos = cos(rotationRadians)
        val sin = sin(rotationRadians)
        return Offset(
            x = pivot.x + dx * cos - dy * sin,
            y = pivot.y + dx * sin + dy * cos,
        )
    }

    private companion object {
        const val EDGE_ALPHA = 0.5f
        const val HALF_TURN_DEGREES = 180f
    }
}
