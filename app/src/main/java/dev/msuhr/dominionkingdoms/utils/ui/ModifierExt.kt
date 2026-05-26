package dev.msuhr.dominionkingdoms.utils.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


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
