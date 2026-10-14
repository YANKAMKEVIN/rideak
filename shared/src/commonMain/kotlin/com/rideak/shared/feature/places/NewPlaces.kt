package com.rideak.shared.feature.places

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideak.exploration.Place
import com.rideak.shared.designsystem.RideakTheme

/** End-of-ride "NOUVEL ENDROIT !" list. Nothing when the ride discovered no place. */
@Composable
fun NewPlaces(places: List<Place>, modifier: Modifier = Modifier) {
    if (places.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (places.size == 1) "NOUVEL ENDROIT !" else "${places.size} NOUVEAUX ENDROITS !",
            style = RideakTheme.typography.impact.copy(fontSize = 26.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
            color = RideakTheme.colors.reward,
            modifier = Modifier.semantics { heading() },
        )
        places.forEach { PlaceRow(it, discovered = true) }
    }
}

/** One place: category icon, name, category. Undiscovered places keep their mystery. */
@Composable
fun PlaceRow(place: Place, discovered: Boolean, modifier: Modifier = Modifier) {
    val colors = RideakTheme.colors
    val tint = if (discovered) place.category.tint else colors.lightSurface
    val name = if (discovered) place.name else "???"
    Row(
        modifier.fillMaxWidth()
            .background(colors.surface, RideakTheme.shapes.medium)
            .border(1.dp, tint.copy(alpha = 0.4f), RideakTheme.shapes.medium)
            .padding(horizontal = RideakTheme.spacing.m, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = if (discovered) "${place.category.label} : ${place.name}" else "${place.category.label} pas encore découvert"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(tint.copy(alpha = 0.15f), RideakTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(place.category.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (discovered) colors.textPrimary else colors.textSecondary, maxLines = 2)
            Text(place.category.label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}
