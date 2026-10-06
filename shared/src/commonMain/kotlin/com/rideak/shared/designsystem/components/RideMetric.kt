package com.rideak.shared.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme

/**
 * One ride statistic. Read by screen readers as a single phrase, e.g. "Distance : 8,42 kilomètres".
 * [spokenUnit] lets TalkBack/VoiceOver say the unit in full instead of "km".
 */
@Composable
fun RideMetric(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    spokenUnit: String = unit,
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label : $value $spokenUnit" },
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = RideakTheme.typography.metric, color = RideakTheme.colors.textPrimary)
            Text(unit, style = MaterialTheme.typography.titleMedium, color = RideakTheme.colors.textSecondary)
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = RideakTheme.colors.textSecondary)
    }
}
