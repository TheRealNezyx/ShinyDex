package com.espinosa.shinydex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.Ink

/**
 * The app mark: the original Poke Ball silhouette, drawn in the ShinyDex palette
 * (gold upper half, white lower half, black band and button).
 */
@Composable
fun PokeballLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(centre.x - radius, centre.y - radius)
        val box = Size(radius * 2f, radius * 2f)

        drawArc(
            color = Gold,
            startAngle = HALF_TURN,
            sweepAngle = HALF_TURN,
            useCenter = true,
            topLeft = topLeft,
            size = box,
        )
        drawArc(
            color = Bone,
            startAngle = 0f,
            sweepAngle = HALF_TURN,
            useCenter = true,
            topLeft = topLeft,
            size = box,
        )

        drawRect(
            color = Ink,
            topLeft = Offset(centre.x - radius, centre.y - radius * BAND_HALF_HEIGHT),
            size = Size(radius * 2f, radius * BAND_HALF_HEIGHT * 2f),
        )

        val rim = radius * RIM_WIDTH
        drawCircle(
            color = Ink,
            radius = radius - rim / 2f,
            center = centre,
            style = Stroke(width = rim),
        )

        drawCircle(color = Ink, radius = radius * BUTTON_OUTER, center = centre)
        drawCircle(color = Bone, radius = radius * BUTTON_INNER, center = centre)
        drawCircle(color = Gold, radius = radius * BUTTON_CORE, center = centre)
    }
}

private const val HALF_TURN = 180f
private const val BAND_HALF_HEIGHT = 0.09f
private const val RIM_WIDTH = 0.11f
private const val BUTTON_OUTER = 0.30f
private const val BUTTON_INNER = 0.21f
private const val BUTTON_CORE = 0.10f
