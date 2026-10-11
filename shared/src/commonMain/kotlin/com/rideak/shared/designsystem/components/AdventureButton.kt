package com.rideak.shared.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme

/** The one primary call to action of a screen ("PARTIR À L'AVENTURE"). */
@Composable
fun AdventureButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = 72.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RideakTheme.shapes.pill,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RideakTheme.colors.action,
            // Large bold label: white passes the 3:1 large-text ratio on Action Red.
            contentColor = Color.White,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Text(text = text, style = RideakTheme.typography.action, textAlign = TextAlign.Center)
        }
    }
}
