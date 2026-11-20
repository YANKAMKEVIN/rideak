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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.shared.format.formatDay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** "Lieux": the real places of the rider's regions, discovered or still hidden. */
@Composable
fun PlacesScreen(onOpenPlace: (Place) -> Unit = {}) {
    val viewModel: PlacesViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = koinInject<PlacesFocus>()
    val requested by focus.categories.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    // Opened from a home shortcut: scroll to that category once the places are known.
    LaunchedEffect(requested, state.loading) {
        val categories = requested ?: return@LaunchedEffect
        if (state.loading) return@LaunchedEffect
        state.sectionIndex(categories)?.let { listState.animateScrollToItem(it) }
        focus.consume()
    }
    Box(Modifier.fillMaxSize().background(RideakTheme.colors.night).windowInsetsPadding(WindowInsets.statusBars)) {
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center)) else PlacesContent(state, listState, onOpenPlace)
    }
}

/** Position in the list of the first section of [categories] (the title block is item 0). */
internal fun PlacesUiState.sectionIndex(categories: Set<PlaceCategory>): Int? {
    var index = 1
    for (section in sections) {
        if (section.category in categories) return index
        index += 1 + section.discovered.size + section.hiddenCards + if (section.hidden > 0) 1 else 0
    }
    return null
}

@Composable
fun PlacesContent(state: PlacesUiState, listState: LazyListState = rememberLazyListState(), onOpenPlace: (Place) -> Unit = {}) {
    val colors = RideakTheme.colors
    LazyColumn(
        state = listState,
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
            items(section.discovered, key = { it.id.value }) { place ->
                PlaceRow(
                    place,
                    discovered = true,
                    detail = section.discoveredAt[place.id]?.let { "découvert le ${formatDay(it)}" },
                    onClick = { onOpenPlace(place) },
                )
            }
            // A few empty slots of the collection, to make the rest worth finding.
            items(section.hiddenCards, key = { "${section.category.name}-hidden-$it" }) { HiddenPlaceRow(section.category) }
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

/** "???" cards shown before the "N lieux encore cachés" line. */
internal val PlaceSection.hiddenCards: Int get() = minOf(hidden, 3)
