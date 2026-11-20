package com.rideak.shared.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Temporary RIDEAK glyphs (24×24), drawn to match the mockup until the illustrated icon set exists.
 * Single-colour so they can be tinted.
 */
object RideakIcons {
    val Park: ImageVector by lazy {
        filled("Park") {
            moveTo(12f, 2f); lineTo(18f, 10f); horizontalLineTo(15f); lineTo(20f, 16f)
            horizontalLineTo(13f); verticalLineTo(22f); horizontalLineTo(11f); verticalLineTo(16f)
            horizontalLineTo(4f); lineTo(9f, 10f); horizontalLineTo(6f); close()
        }
    }

    val River: ImageVector by lazy {
        stroked("River") {
            wave(7f); wave(12f); wave(17f)
        }
    }

    val City: ImageVector by lazy {
        filled("City") {
            rect(3f, 11f, 5f, 11f); rect(9.5f, 4f, 5f, 18f); rect(16f, 8f, 5f, 14f)
        }
    }

    val Hills: ImageVector by lazy {
        filled("Hills") {
            moveTo(1.5f, 20f); lineTo(9f, 6f); lineTo(13.5f, 14f); lineTo(16.5f, 9.5f); lineTo(22.5f, 20f); close()
        }
    }

    val Leaf: ImageVector by lazy {
        filled("Leaf") {
            moveTo(20f, 3f)
            curveTo(10f, 3f, 4f, 8f, 4f, 15f)
            curveTo(4f, 17.5f, 5f, 20f, 5f, 20f)
            lineTo(7f, 18f)
            curveTo(14f, 19f, 20f, 13f, 20f, 3f)
            close()
        }
    }

    val Bike: ImageVector by lazy {
        stroked("Bike") {
            circle(5.5f, 16f, 3.5f); circle(18.5f, 16f, 3.5f)
            moveTo(5.5f, 16f); lineTo(9.5f, 9f); horizontalLineTo(16f); lineTo(18.5f, 16f)
            moveTo(9.5f, 9f); lineTo(12.5f, 16f); horizontalLineTo(5.5f)
            moveTo(16f, 9f); lineTo(15f, 5.5f); horizontalLineTo(13f)
            moveTo(8f, 6.5f); horizontalLineTo(11.5f)
        }
    }

    val Map: ImageVector by lazy {
        stroked("Map") {
            moveTo(3f, 6f); lineTo(9f, 4f); lineTo(15f, 6f); lineTo(21f, 4f); verticalLineTo(18f)
            lineTo(15f, 20f); lineTo(9f, 18f); lineTo(3f, 20f); close()
            moveTo(9f, 4f); verticalLineTo(18f)
            moveTo(15f, 6f); verticalLineTo(20f)
        }
    }

    val Pause: ImageVector by lazy {
        filled("Pause") { rect(6f, 4f, 4f, 16f); rect(14f, 4f, 4f, 16f) }
    }

    val Stop: ImageVector by lazy {
        filled("Stop") { rect(5f, 5f, 14f, 14f) }
    }

    val Play: ImageVector by lazy {
        filled("Play") { moveTo(7f, 4f); lineTo(20f, 12f); lineTo(7f, 20f); close() }
    }

    val Heart: ImageVector by lazy {
        filled("Heart") {
            moveTo(12f, 21f)
            lineTo(10.6f, 19.7f)
            curveTo(5.4f, 15f, 2f, 11.9f, 2f, 8.2f)
            curveTo(2f, 5.1f, 4.4f, 2.8f, 7.5f, 2.8f)
            curveTo(9.2f, 2.8f, 10.9f, 3.6f, 12f, 4.9f)
            curveTo(13.1f, 3.6f, 14.8f, 2.8f, 16.5f, 2.8f)
            curveTo(19.6f, 2.8f, 22f, 5.1f, 22f, 8.2f)
            curveTo(22f, 11.9f, 18.6f, 15f, 13.4f, 19.7f)
            close()
        }
    }

    /** Distance (mockup: stopwatch outline). */
    val Stopwatch: ImageVector by lazy {
        stroked("Stopwatch") {
            circle(12f, 13.5f, 7.5f)
            moveTo(10f, 2.5f); horizontalLineTo(14f)
            moveTo(12f, 2.5f); verticalLineTo(6f)
            moveTo(12f, 13.5f); lineTo(15f, 10.5f)
            moveTo(18.5f, 5.5f); lineTo(20f, 7f)
        }
    }

    /** Duration (mockup: alarm clock). */
    val Timer: ImageVector by lazy {
        stroked("Timer") {
            circle(12f, 13f, 7.5f)
            moveTo(12f, 9f); verticalLineTo(13f); lineTo(14.5f, 15f)
            moveTo(3f, 6f); lineTo(6f, 3.5f)
            moveTo(21f, 6f); lineTo(18f, 3.5f)
            moveTo(7f, 20f); lineTo(5.5f, 21.5f)
            moveTo(17f, 20f); lineTo(18.5f, 21.5f)
        }
    }

    /** Speed (mockup: gauge). */
    val Speedometer: ImageVector by lazy {
        stroked("Speedometer") {
            moveTo(3.5f, 17f)
            arcToRelative(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 17f, dy1 = 0f)
            moveTo(12f, 17f); lineTo(16f, 10.5f)
            moveTo(3.5f, 17f); horizontalLineTo(6f)
            moveTo(20.5f, 17f); horizontalLineTo(18f)
        }
    }

    /** Elevation (mockup: double peak outline). */
    val Mountain: ImageVector by lazy {
        stroked("Mountain") {
            moveTo(2f, 20f); lineTo(8.5f, 6f); lineTo(12.5f, 14f); lineTo(15.5f, 9f); lineTo(22f, 20f); close()
            moveTo(6.6f, 9f); lineTo(8.5f, 10.5f); lineTo(10.3f, 9.4f)
        }
    }

    /** Elevation loss: a slope going down to the right, with an arrow. */
    val Descent: ImageVector by lazy {
        stroked("Descent") {
            moveTo(2f, 6f); lineTo(9f, 9.5f); lineTo(13f, 14f); lineTo(22f, 19f)
            moveTo(2f, 20f); horizontalLineTo(22f)
            moveTo(19.5f, 9f); verticalLineTo(14.5f)
            moveTo(17f, 12.5f); lineTo(19.5f, 15f); lineTo(22f, 12.5f)
        }
    }

    /** Back, same stroke as the rest of the set (mirrored in right-to-left layouts). */
    val Back: ImageVector by lazy {
        ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f, autoMirror = true)
            .path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20f, 12f); horizontalLineTo(4.5f)
                moveTo(10.5f, 5.5f); lineTo(4f, 12f); lineTo(10.5f, 18.5f)
            }
            .build()
    }

    val Lightning: ImageVector by lazy {
        filled("Lightning") {
            moveTo(14f, 1.5f); lineTo(4.5f, 13.5f); horizontalLineTo(11f); lineTo(9.5f, 22.5f); lineTo(19.5f, 9.5f)
            horizontalLineTo(13f); close()
        }
    }

    private fun PathBuilder.rect(x: Float, y: Float, w: Float, h: Float) {
        moveTo(x, y); horizontalLineToRelative(w); verticalLineToRelative(h); horizontalLineToRelative(-w); close()
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2 * r, dy1 = 0f)
        arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2 * r, dy1 = 0f)
    }

    private fun PathBuilder.wave(y: Float) {
        moveTo(2f, y)
        quadTo(4.5f, y - 2.5f, 7f, y)
        reflectiveQuadTo(12f, y)
        reflectiveQuadTo(17f, y)
        reflectiveQuadTo(22f, y)
    }

    private fun filled(name: String, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black), pathBuilder = block)
            .build()

    private fun stroked(name: String, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
            .build()
}
