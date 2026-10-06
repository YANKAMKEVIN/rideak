package com.rideak.shared.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.rideak.shared.designsystem.RideakTheme

/**
 * Placeholder for the Home hero illustration (cyclist overlooking the city in the mockup).
 * Drawn in code — city, river, hills — so the layout can be validated before the real artwork,
 * which will replace this composable as an image. Purely decorative: no semantics.
 */
@Composable
fun HeroLandscape(modifier: Modifier = Modifier) {
    val night = RideakTheme.colors.night
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1F6FD1), Color(0xFF7DBCF0), Color(0xFFCFE6F7)), endY = h * 0.6f))

        // Clouds stay right of the greeting and below the status bar.
        listOf(0.66f to 0.3f, 0.9f to 0.22f).forEach { (x, y) -> cloud(Offset(w * x, h * y), w * 0.06f) }

        // Skyline on the horizon.
        val skyline = listOf(0.04f to 0.16f, 0.12f to 0.24f, 0.2f to 0.12f, 0.46f to 0.2f, 0.53f to 0.3f,
            0.6f to 0.17f, 0.68f to 0.26f, 0.76f to 0.14f, 0.84f to 0.28f, 0.92f to 0.19f)
        skyline.forEach { (x, height) ->
            drawRect(Color(0xFF6E8FB8), Offset(w * x, h * (0.56f - height)), Size(w * 0.05f, h * height))
        }
        drawRect(Color(0xFFB7C9DD), Offset(0f, h * 0.53f), Size(w, h * 0.05f))

        hill(Color(0xFF3FA866), h * 0.58f, h * 0.52f, h * 0.6f)
        // River towards the city.
        drawPath(Path().apply {
            moveTo(w * 0.35f, h * 0.6f); cubicTo(w * 0.6f, h * 0.63f, w * 0.95f, h * 0.66f, w, h * 0.7f)
            lineTo(w, h * 0.8f); cubicTo(w * 0.85f, h * 0.72f, w * 0.55f, h * 0.66f, w * 0.35f, h * 0.6f); close()
        }, Color(0xFF5DB2EE))
        hill(Color(0xFF237A47), h * 0.78f, h * 0.68f, h * 0.84f)
        listOf(0.06f, 0.14f, 0.22f, 0.7f, 0.8f, 0.9f).forEachIndexed { i, x ->
            tree(Offset(w * x, h * (0.76f + (i % 3) * 0.03f)), h * 0.09f)
        }

        drawRect(Brush.verticalGradient(listOf(Color.Transparent, night), startY = h * 0.62f, endY = h))
    }
}

private fun DrawScope.cloud(center: Offset, r: Float) {
    val c = Color.White.copy(alpha = 0.9f)
    drawCircle(c, r, center)
    drawCircle(c, r * 0.7f, center + Offset(-r * 0.9f, r * 0.25f))
    drawCircle(c, r * 0.75f, center + Offset(r * 0.95f, r * 0.2f))
}

private fun DrawScope.hill(color: Color, left: Float, peak: Float, right: Float) {
    val w = size.width
    drawPath(Path().apply {
        moveTo(0f, left); cubicTo(w * 0.3f, peak, w * 0.6f, peak, w, right)
        lineTo(w, size.height); lineTo(0f, size.height); close()
    }, color)
}

private fun DrawScope.tree(base: Offset, height: Float) {
    drawPath(Path().apply {
        moveTo(base.x, base.y - height); lineTo(base.x + height * 0.35f, base.y); lineTo(base.x - height * 0.35f, base.y); close()
    }, Color(0xFF145A33))
}
