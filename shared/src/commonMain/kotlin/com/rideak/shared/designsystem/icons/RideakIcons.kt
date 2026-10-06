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
