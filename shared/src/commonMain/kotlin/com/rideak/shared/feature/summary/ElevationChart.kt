package com.rideak.shared.feature.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rideak.ride.ProfilePoint
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.format.formatKilometers
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Altitude along the ride (mockup "5. FIN DE SORTIE"): red line with a fading fill, light axes. */
@Composable
fun ElevationChart(profile: List<ProfilePoint>, modifier: Modifier = Modifier) {
    val colors = RideakTheme.colors
    val label = MaterialTheme.typography.labelSmall
    if (profile.size < 2) {
        Text("Pas de données d'altitude pour cette sortie.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = modifier)
        return
    }
    val minAlt = floor(profile.minOf { it.altitudeMeters } / 10) * 10
    val maxAlt = max(ceil(profile.maxOf { it.altitudeMeters } / 10) * 10, minAlt + 20)
    val totalDistance = max(profile.last().distanceMeters, 1.0)
    Column(
        modifier.clearAndSetSemantics {
            contentDescription = "Profil d'altitude : entre ${minAlt.toInt()} et ${maxAlt.toInt()} mètres sur ${formatKilometers(totalDistance)} kilomètres"
        },
    ) {
        Row {
            Column(Modifier.width(36.dp).height(120.dp)) {
                Text("${maxAlt.toInt()}", style = label, color = colors.textSecondary)
                Text("${((maxAlt + minAlt) / 2).toInt()}", style = label, color = colors.textSecondary, modifier = Modifier.padding(top = 34.dp))
                Text("${minAlt.toInt()}", style = label, color = colors.textSecondary, modifier = Modifier.padding(top = 34.dp))
            }
            Canvas(Modifier.weight(1f).height(120.dp)) {
                val grid = colors.lightSurface.copy(alpha = 0.15f)
                listOf(0f, 0.5f, 1f).forEach { f -> drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f)) }
                fun point(p: ProfilePoint) = Offset(
                    (p.distanceMeters / totalDistance * size.width).toFloat(),
                    (size.height - (p.altitudeMeters - minAlt) / (maxAlt - minAlt) * size.height).toFloat(),
                )
                val line = Path().apply {
                    val first = point(profile.first()); moveTo(first.x, first.y)
                    profile.drop(1).forEach { val o = point(it); lineTo(o.x, o.y) }
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(point(profile.last()).x, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(colors.action.copy(alpha = 0.55f), colors.action.copy(alpha = 0.05f))))
                drawPath(line, colors.action, style = Stroke(3.5f, join = StrokeJoin.Round))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp)) {
            Text("0 km", style = label, color = colors.textSecondary, modifier = Modifier.weight(1f))
            Text(formatKilometers(totalDistance / 2), style = label, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("${formatKilometers(totalDistance)} km", style = label, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}
