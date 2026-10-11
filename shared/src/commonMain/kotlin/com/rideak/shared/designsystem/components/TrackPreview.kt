package com.rideak.shared.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rideak.exploration.GeoPoint
import com.rideak.shared.designsystem.RideakTheme
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

private const val EARTH_RADIUS = 6_378_137.0
private const val HALF_WORLD = PI * EARTH_RADIUS
// Same cells as SlippyTileGrid(zoom = 17), in Web-Mercator meters.
private const val CELL_METERS = 2 * HALF_WORLD / (1 shl 17)

private data class Projected(val x: Double, val y: Double)

private fun GeoPoint.project() = Projected(
    x = EARTH_RADIUS * longitude * PI / 180,
    y = EARTH_RADIUS * ln(tan(PI / 4 + latitude * PI / 360)),
)

/**
 * Stand-in for the real map until MapLibre lands: the ride drawn from its own data —
 * explored cells (the "fog" lifting), the route with a glow, spots and the current position.
 * Decorative for screen readers: the same information is spelled out by the surrounding stats.
 */
@Composable
fun TrackPreview(
    segments: List<List<GeoPoint>>,
    modifier: Modifier = Modifier,
    spots: List<GeoPoint> = emptyList(),
    showCurrentPosition: Boolean = false,
    minSpanMeters: Double = 900.0,
) {
    val colors = RideakTheme.colors
    val projected = remember(segments) { segments.map { s -> s.map { it.project() } } }
    val cells = remember(projected) {
        projected.flatten().mapTo(HashSet()) { floor((it.x + HALF_WORLD) / CELL_METERS) to floor((HALF_WORLD - it.y) / CELL_METERS) }
    }
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E2B27), Color(0xFF0A1C22), colors.night)))
        val all = projected.flatten() + spots.map { it.project() }
        if (all.isEmpty()) {
            drawGrid(Color.White.copy(alpha = 0.05f), 48f, Offset.Zero)
            return@Canvas
        }
        val minX = all.minOf { it.x }; val maxX = all.maxOf { it.x }
        val minY = all.minOf { it.y }; val maxY = all.maxOf { it.y }
        val spanX = max(maxX - minX, minSpanMeters); val spanY = max(maxY - minY, minSpanMeters)
        val scale = min(size.width / (spanX * 1.25), size.height / (spanY * 1.25))
        val cx = (minX + maxX) / 2; val cy = (minY + maxY) / 2
        fun Projected.toOffset() = Offset(
            (size.width / 2 + (x - cx) * scale).toFloat(),
            (size.height / 2 - (y - cy) * scale).toFloat(),
        )
        val cellPx = (CELL_METERS * scale).toFloat()
        // Grid lines on the real cell boundaries (computed in Double: Float would drift at world scale).
        if (cellPx >= 6f) {
            val grid = Color.White.copy(alpha = 0.05f)
            val halfW = size.width / 2 / scale; val halfH = size.height / 2 / scale
            var col = floor((cx - halfW + HALF_WORLD) / CELL_METERS)
            while (col * CELL_METERS - HALF_WORLD <= cx + halfW) {
                val x = Projected(col * CELL_METERS - HALF_WORLD, cy).toOffset().x
                drawLine(grid, Offset(x, 0f), Offset(x, size.height)); col++
            }
            var row = floor((HALF_WORLD - (cy + halfH)) / CELL_METERS)
            while (HALF_WORLD - row * CELL_METERS >= cy - halfH) {
                val y = Projected(cx, HALF_WORLD - row * CELL_METERS).toOffset().y
                drawLine(grid, Offset(0f, y), Offset(size.width, y)); row++
            }
        }

        cells.forEach { (col, row) ->
            val topLeft = Projected(col * CELL_METERS - HALF_WORLD, HALF_WORLD - row * CELL_METERS).toOffset()
            drawRect(colors.nature.copy(alpha = 0.22f), topLeft, Size(cellPx, cellPx))
            drawRect(colors.nature.copy(alpha = 0.55f), topLeft, Size(cellPx, cellPx), style = Stroke(1.5f))
        }

        projected.filter { it.size > 1 }.forEach { segment ->
            val path = Path().apply {
                val first = segment.first().toOffset()
                moveTo(first.x, first.y)
                segment.drop(1).forEach { val o = it.toOffset(); lineTo(o.x, o.y) }
            }
            drawPath(path, colors.action.copy(alpha = 0.3f), style = Stroke(26f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, colors.action, style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        spots.forEach { spot ->
            val o = spot.project().toOffset()
            drawCircle(Color.White, 13f, o)
            drawCircle(colors.action, 10f, o)
        }
        if (showCurrentPosition) projected.lastOrNull { it.isNotEmpty() }?.last()?.toOffset()?.let { o ->
            drawCircle(colors.exploration.copy(alpha = 0.25f), 34f, o)
            drawCircle(Color.White, 16f, o)
            drawCircle(colors.exploration, 11f, o)
        }
    }
}

private fun DrawScope.drawGrid(color: Color, step: Float, offset: Offset) {
    if (step < 6f) return
    var x = offset.x
    while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height)); x += step }
    var y = offset.y
    while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y)); y += step }
}
