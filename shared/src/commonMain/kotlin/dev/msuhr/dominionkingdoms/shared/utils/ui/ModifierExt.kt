package dev.msuhr.dominionkingdoms.shared.utils.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.msuhr.dominionkingdoms.shared.ui.components.SwipeLock

// Port of app/utils/ui/ModifierExt.kt

/** Fading edges for scrolling chips. */
internal fun Modifier.horizontalFadingEdges(fadeWidth: Dp = 16.dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val widthPx = fadeWidth.toPx()

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = 0f,
                endX = widthPx
            ),
            blendMode = BlendMode.DstIn
        )

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = size.width - widthPx,
                endX = size.width
            ),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * A synchronous touch gatekeeper that ensures only a single composable node
 * across the entire hierarchy can process pointer drag events at any given time.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.swipeLockGatekeeper(
    cardId: Int,
    swipeLock: SwipeLock,
    dismissState: SwipeToDismissBoxState
): Modifier = this.pointerInput(cardId) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val currentLock = swipeLock.activeCardId

            if (currentLock != null && currentLock != cardId) {
                event.changes.forEach { it.consume() }
            } else {
                val hasActiveTouches = event.changes.any { it.pressed }

                if (hasActiveTouches && currentLock == null) {
                    swipeLock.activeCardId = cardId
                }

                val allFingersUp = event.changes.all { !it.pressed }
                if (allFingersUp && swipeLock.activeCardId == cardId && dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
                    swipeLock.activeCardId = null
                }
            }
        }
    }
}
