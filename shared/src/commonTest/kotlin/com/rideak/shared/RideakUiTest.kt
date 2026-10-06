package com.rideak.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.navigation.RideakApp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RideakUiTest {
    @Test
    fun adventureButtonIsClickableWithLargeTouchTarget() = runComposeUiTest {
        var clicked = false
        setContent { RideakTheme { AdventureButton("PARTIR", onClick = { clicked = true }) } }

        onNodeWithText("PARTIR").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(clicked)
    }

    @Test
    fun bottomBarNavigatesBetweenTopLevelDestinations() = runComposeUiTest {
        setContent { RideakApp() }

        onNodeWithText("Yo ! Prêt à explorer ?").assertIsDisplayed()
        onNodeWithText("Carte").performClick()
        onNodeWithText("Ta world map personnelle se dévoilera ici.").assertIsDisplayed()
    }
}
