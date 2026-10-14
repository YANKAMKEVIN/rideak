package com.rideak.shared.feature.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import org.koin.compose.viewmodel.koinViewModel

/** "Lieux": the real places of the rider's regions, discovered or still hidden. */
@Composable
fun PlacesScreen() {
    val viewModel: PlacesViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(RideakTheme.colors.night).windowInsetsPadding(WindowInsets.statusBars)) {
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center)) else PlacesContent(state)
    }
}

@Composable
fun PlacesContent(state: PlacesUiState) {
    val colors = RideakTheme.colors
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Lieux",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                when {
                    state.total == 0 -> "Aucun lieu connu ici pour l'instant."
                    state.discovered == 0 -> "${state.total} lieux t'attendent. Roule pour les découvrir !"
                    else -> "${state.discovered} / ${state.total} lieux découverts"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
        state.sections.forEach { section ->
            item(key = section.category.name) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp).semantics(mergeDescendants = true) { heading() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(section.category.plural, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("${section.discovered.size} / ${section.total}", style = MaterialTheme.typography.labelLarge, color = section.category.tint)
                }
            }
            items(section.discovered, key = { it.id.value }) { PlaceRow(it, discovered = true) }
            if (section.hidden > 0) {
                item(key = "${section.category.name}-hidden") {
                    Text(
                        if (section.hidden == 1) "1 lieu encore caché" else "${section.hidden} lieux encore cachés",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
