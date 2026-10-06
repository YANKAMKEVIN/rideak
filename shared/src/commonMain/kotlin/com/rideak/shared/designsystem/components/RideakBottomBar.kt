package com.rideak.shared.designsystem.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rideak.shared.designsystem.RideakTheme

data class BottomBarItem(val label: String, val icon: ImageVector)

/** Labels are always shown: an icon alone is not enough to understand a destination. */
@Composable
fun RideakBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier, containerColor = RideakTheme.colors.surface) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RideakTheme.colors.onAccent,
                    selectedTextColor = RideakTheme.colors.textPrimary,
                    indicatorColor = RideakTheme.colors.exploration,
                    unselectedIconColor = RideakTheme.colors.textSecondary,
                    unselectedTextColor = RideakTheme.colors.textSecondary,
                ),
            )
        }
    }
}
