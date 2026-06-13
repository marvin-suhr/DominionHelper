package dev.msuhr.dominionkingdoms.utils.ui

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
import dev.msuhr.dominionkingdoms.ui.components.SwipeLock


// Fading edges for scrolling chips
internal fun Modifier.horizontalFadingEdges(fadeWidth: Dp = 16.dp): Modifier = this
    // 1. Enforce a Layer Strategy to isolate transparency blending
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent() // Render the underlying chips first

        val widthPx = fadeWidth.toPx()

        // 2. Fade at the left edge (Start)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = 0f,
                endX = widthPx
            ),
            blendMode = BlendMode.DstIn
        )

        // 3. Fade at the right edge (End)
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
                // Another element holds the exclusive lock; discard all pointers
                event.changes.forEach { it.consume() }
            } else {
                // Lock is free or held by this element
                val hasActiveTouches = event.changes.any { it.pressed }

                if (hasActiveTouches && currentLock == null) {
                    swipeLock.activeCardId = cardId // Seize lock instantly
                }

                // Clear the lock safely if the user lifts their finger without completing the swipe
                val allFingersUp = event.changes.all { !it.pressed }
                if (allFingersUp && swipeLock.activeCardId == cardId && dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
                    swipeLock.activeCardId = null
                }
            }
        }
    }
}
