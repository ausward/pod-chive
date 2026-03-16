package com.pod_chive.android.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

@Composable
fun SadChive(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50), // Keeping the same vibrant green, but we could make it slightly desaturated
    isError: Boolean // The droop animation only plays if this is true (e.g., when an error state occurs)
) {
    val transition = rememberInfiniteTransition(label = "SadChiveAnimation")

    // Define the slow, slight droop animation parameters
    val droopAngle = transition.animateFloat(
        initialValue = -3f, // Start slightly upright
        targetValue = 3f,   // End hanging down slightly
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing), // Much slower, smoother ease
            repeatMode = RepeatMode.Reverse
        ),
        label = "Droop"
    )

    Canvas(modifier = modifier) {
        // Apply global bounce translation (optional, a slight subtle vertical movement)
        translate(top = 1.dp.toPx() * (if (isError) droopAngle.value / 3f else 0f)) {
            val stemWidth = 8.dp.toPx()
            val stemHeight = 60.dp.toPx()
            val stemTopOffset = 10.dp.toPx()

            // Apply rotation around the stem base for the droop
            withTransform({
                rotate(degrees = (if (isError) droopAngle.value else 0f) as Float, pivot = center.copy(y = stemHeight))
            }) {
                // Draw the main stem (same as before)
                drawRoundRect(
                    color = color,
                    topLeft = center.copy(x = center.x - stemWidth / 2, y = stemTopOffset),
                    size = Size(stemWidth, stemHeight - stemTopOffset),
                    cornerRadius = CornerRadius(stemWidth / 2)
                )

                // Draw the leaves with less flex for a more "wilted" look
                drawLeaf(
                    color = color,
                    base = center.copy(y = stemTopOffset + 15.dp.toPx()),
                    isLeft = true,
                    flex = 0.1f // Reduced flex for a downcast appearance
                )
                drawLeaf(
                    color = color,
                    base = center.copy(y = stemTopOffset + 10.dp.toPx()),
                    isLeft = false,
                    flex = -0.1f // Reduced flex in opposite direction
                )

                // **Draw the "Sad" Face Expressions (NEW)**

                // Position eyes slightly lower on the stem for a dejected look
                val eyeY = stemTopOffset + 12.dp.toPx()

                // Draw teary/downcast eyes
                // Left Eye (closer and slightly angled down)
                drawPath(
                    path = Path().apply {
                        moveTo(center.x - 6.dp.toPx(), eyeY)
                        quadraticTo(
                            center.x - 5.dp.toPx(), eyeY + 1.dp.toPx(),
                            center.x - 4.dp.toPx(), eyeY
                        )
                    },
                    color = Color.Black
                )

                // Right Eye (similar to left, slightly angled down)
                drawPath(
                    path = Path().apply {
                        moveTo(center.x + 6.dp.toPx(), eyeY)
                        quadraticTo(
                            center.x + 5.dp.toPx(), eyeY + 1.dp.toPx(),
                            center.x + 4.dp.toPx(), eyeY
                        )
                    },
                    color = Color.Black
                )

                // Draw a simple downturned "frown" mouth
                val frownY = stemTopOffset + 22.dp.toPx()
                drawPath(
                    path = Path().apply {
                        moveTo(center.x - 6.dp.toPx(), frownY + 2.dp.toPx())
                        quadraticTo(
                            center.x, frownY,
                            center.x + 6.dp.toPx(), frownY + 2.dp.toPx()
                        )
                    },
                    color = Color.Black,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}