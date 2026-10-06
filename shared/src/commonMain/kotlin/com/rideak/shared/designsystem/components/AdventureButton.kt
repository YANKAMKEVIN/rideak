package com.rideak.shared.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme

/** The one primary call to action of a screen ("PARTIR À L'AVENTURE"). */
@Composable
fun AdventureButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RideakTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = RideakTheme.colors.action,
            // Large bold label: white passes the 3:1 large-text ratio on Action Red.
            contentColor = Color.White,
        ),
    ) {
        Text(text = text, style = RideakTheme.typography.action, textAlign = TextAlign.Center)
    }
}
