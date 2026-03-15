package com.pod_chive.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(modifierBOX: Modifier = Modifier.fillMaxSize(), modifierCPI: Modifier = Modifier) {
    Box(
        modifier = modifierBOX,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = modifierCPI,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 69.dp
        )
    }

//    AnimatedChive(modifier = modifierBOX.align(Alignment.Center), isLoading =true)
}


@Composable
fun AnimatedChive(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = Color(0xFF4CAF50), // A vibrant green
    isLoading: Boolean // The animation only plays if this is true
) {
    val transition = rememberInfiniteTransition(label = "ChiveAnimation")

    // Define the animation parameters
    val sway by transition.animateFloat(
        initialValue = -10f, // Start leaning left
        targetValue = 10f,   // End leaning right
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Sway"
    )

    val bounce by transition.animateFloat(
        initialValue = 0f,   // No bounce
        targetValue = 5f,    // Slight upward bounce
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bounce"
    )


    Canvas(modifier = modifier) {
        // Draw only if loading
        if (!isLoading) return@Canvas

        // Apply global bounce translation
        translate(top = -bounce) {
            val stemWidth = 8.dp.toPx()
            val stemHeight = 60.dp.toPx()
            val stemTopOffset = 10.dp.toPx() // Height above the bounce

            // Apply sway rotation around the stem base
            withTransform({
                rotate(degrees = sway, pivot = center.copy(y = stemHeight))
            }) {
                // Draw the main stem
                drawRoundRect(
                    color = color,
                    topLeft = center.copy(x = center.x - stemWidth / 2, y = stemTopOffset),
                    size = Size(stemWidth, stemHeight - stemTopOffset),
                    cornerRadius = CornerRadius(stemWidth / 2)
                )

                // Draw the leaves with additional bounce-driven flex
                val leafFlex = 0.5f * (1f - bounce / 5f) // Flex more as it bounces higher
                drawLeaf(
                    color = color,
                    base = center.copy(y = stemTopOffset + 15.dp.toPx()),
                    isLeft = true,
                    flex = leafFlex
                )
                drawLeaf(
                    color = color,
                    base = center.copy(y = stemTopOffset + 10.dp.toPx()),
                    isLeft = false,
                    flex = -leafFlex // Flex in opposite direction
                )
            }
        }
    }
}

// Helper to draw a curved leaf shape
private fun DrawScope.drawLeaf(
    color: Color,
    base: Offset,
    isLeft: Boolean,
    flex: Float
) {
    val leafWidth = 20.dp.toPx()
    val leafHeight = 35.dp.toPx()
    val dir = if (isLeft) -1f else 1f

    val bend = flex * 10f
    val tipX = base.x + (leafWidth + bend) * dir
    val tipY = base.y - leafHeight

    val path = Path().apply {
        moveTo(base.x, base.y)

        // top curve to tip
        cubicTo(
            base.x + (leafWidth * 0.3f + bend) * dir,
            base.y - leafHeight * 0.2f,

            base.x + (leafWidth * 0.7f + bend) * dir,
            base.y - leafHeight * 0.7f,

            tipX,
            tipY
        )

        // underside curve back
        cubicTo(
            base.x + (leafWidth * 0.5f - bend * 0.3f) * dir,
            base.y - leafHeight * 0.8f,

            base.x + (leafWidth * 0.1f - bend * 0.5f) * dir,
            base.y - leafHeight * 0.4f,

            base.x,
            base.y
        )
    }

    drawPath(
        path = path,
        color = color
    )

    // Optional: draw a center vein
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = base,
        end = Offset(
            base.x + leafWidth * 0.8f * dir,
            base.y - leafHeight * 0.9f
        ),
        strokeWidth = 1.5.dp.toPx()
    )
}