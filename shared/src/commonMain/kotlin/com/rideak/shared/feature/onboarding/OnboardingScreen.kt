package com.rideak.shared.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.icons.RideakIcons
import com.rideak.shared.resources.Res
import com.rideak.shared.resources.discovery_park
import com.rideak.shared.resources.onboarding
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One pane: an illustration (or a big icon when there is none), a manga title and one sentence. */
private data class Pane(val title: String, val body: String, val image: DrawableResource? = null, val icon: ImageVector? = null)

// Illustrations from design/generated (prompts in docs/assets/onboarding.prompt.txt and discovery_park.prompt.txt).
private val panes = listOf(
    Pane(
        "EXPLORE TON MONDE À VÉLO",
        "Chaque sortie enregistrée dissipe le brouillard de ta carte. Les rues que tu n'as jamais prises t'attendent.",
        image = Res.drawable.onboarding,
    ),
    Pane(
        "DÉCOUVRE LES LIEUX CACHÉS",
        "Parcs, rivières, points de vue… Passe à côté pour les révéler, gagne de l'XP et débloque des badges.",
        image = Res.drawable.discovery_park,
    ),
    Pane(
        "TES TRACES RESTENT SUR TON TÉLÉPHONE",
        "Ta position n'est utilisée que pendant une sortie. Tes trajets ne quittent pas ton téléphone, et tu peux masquer ton domicile avec les zones privées.",
        icon = RideakIcons.Map,
    ),
)

/**
 * First launch: three swipeable panes explaining the game, then "PARTIR À L'AVENTURE", which opens
 * the ride screen where the location permission is asked with its explanation (docs/privacy.md:
 * position asked at the moment of leaving). "Plus tard" goes to the home screen.
 */
@Composable
fun OnboardingScreen(onStartRide: () -> Unit, onSkip: () -> Unit) {
    val pager = rememberPagerState { panes.size }
    val scope = rememberCoroutineScope()
    val colors = RideakTheme.colors
    val last = pager.currentPage == panes.lastIndex
    Column(Modifier.fillMaxSize().background(colors.night)) {
        HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page -> PaneContent(panes[page]) }
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(RideakTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageDots(pager.currentPage, panes.size)
            AdventureButton(
                text = if (last) "PARTIR\nÀ L'AVENTURE" else "SUIVANT",
                icon = if (last) RideakIcons.Bike else null,
                minHeight = 64.dp,
                onClick = { if (last) onStartRide() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            )
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = RideakTheme.spacing.touchTarget)) {
                Text(if (last) "Plus tard" else "Passer", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PaneContent(pane: Pane) {
    val colors = RideakTheme.colors
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (pane.image != null) {
                Image(
                    painterResource(pane.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(horizontalBias = 0f, verticalBias = 0.3f),
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to colors.night)))
            } else if (pane.icon != null) {
                Box(
                    Modifier.align(Alignment.Center).windowInsetsPadding(WindowInsets.statusBars).size(168.dp)
                        .clip(RideakTheme.shapes.pill).background(Brush.linearGradient(listOf(colors.exploration, colors.nature))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(pane.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(96.dp))
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = RideakTheme.spacing.l).padding(top = RideakTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
        ) {
            Text(
                pane.title,
                style = RideakTheme.typography.impact.copy(fontSize = 38.sp, lineHeight = 40.sp, shadow = Shadow(Color.Black, Offset(3f, 4f), 6f)),
                color = colors.mangaWhite,
                modifier = Modifier.semantics { heading() },
            )
            Text(pane.body, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    val colors = RideakTheme.colors
    Row(
        Modifier.clearAndSetSemantics { contentDescription = "Page ${current + 1} sur $count" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { index ->
            Box(
                Modifier.size(width = if (index == current) 24.dp else 8.dp, height = 8.dp)
                    .clip(RideakTheme.shapes.pill)
                    .background(if (index == current) colors.reward else colors.lightSurface.copy(alpha = 0.4f)),
            )
        }
    }
}
