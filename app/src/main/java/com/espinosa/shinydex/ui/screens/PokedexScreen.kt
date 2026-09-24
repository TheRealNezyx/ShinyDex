package com.espinosa.shinydex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.ui.components.Chip
import com.espinosa.shinydex.ui.components.PokemonSprite
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.ErrorRed
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.Ink
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.InkSoft
import com.espinosa.shinydex.ui.theme.Muted
import com.espinosa.shinydex.ui.viewmodel.DetailState
import com.espinosa.shinydex.util.DexEntry
import java.util.Locale

/** The Pokedex for the selected generation, with a normal / shiny sprite toggle. */
@Composable
fun PokedexScreen(
    entries: List<PokemonSummary>,
    query: String,
    showShiny: Boolean,
    isRefreshing: Boolean,
    error: String?,
    selected: PokemonSummary?,
    detail: DetailState?,
    onQueryChange: (String) -> Unit,
    onToggleShiny: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (PokemonSummary) -> Unit,
    onDismissDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Name or dex number") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = showShiny,
                    onCheckedChange = { onToggleShiny() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Gold,
                        uncheckedThumbColor = Muted,
                        uncheckedTrackColor = InkCard,
                    ),
                )
                SectionLabel("Shiny")
            }
        }

        if (error != null) {
            ErrorBanner(message = error, onRetry = onRefresh)
        }

        when {
            isRefreshing && entries.isEmpty() ->
                CentredMessage("Downloading the Pokedex from PokeAPI...")

            entries.isEmpty() && query.isNotBlank() ->
                CentredMessage("Nothing in this dex matches that.")

            entries.isEmpty() ->
                CentredMessage("No Pokedex data yet. Connect to the internet and retry.")

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.id }) { entry ->
                    PokedexCell(
                        pokemon = entry,
                        showShiny = showShiny,
                        onClick = { onSelect(entry) },
                    )
                }
            }
        }
    }

    if (selected != null) {
        PokedexDetailDialog(
            pokemon = selected,
            detail = detail,
            onDismiss = onDismissDetail,
        )
    }
}

@Composable
private fun PokedexCell(
    pokemon: PokemonSummary,
    showShiny: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(InkCard, RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = if (showShiny) Gold.copy(alpha = BORDER_ALPHA) else InkLine,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PokemonSprite(
            url = pokemon.sprite(showShiny),
            contentDescription = pokemon.displayName,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = pokemon.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = Bone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pokemon.dexNumber,
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
        )
    }
}

@Composable
private fun PokedexDetailDialog(
    pokemon: PokemonSummary,
    detail: DetailState?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = InkSoft,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = pokemon.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold,
                )
                Text(
                    text = pokemon.dexNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SpriteWithLabel(pokemon.artwork(false), "Normal")
                    SpriteWithLabel(pokemon.artwork(true), "Shiny")
                }

                Spacer(Modifier.height(14.dp))

                when (detail) {
                    null, is DetailState.Loading ->
                        Text(
                            text = "Loading entry...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )

                    is DetailState.Failed ->
                        Text(
                            text = detail.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            textAlign = TextAlign.Center,
                        )

                    is DetailState.Loaded -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            detail.detail.types.forEach { type ->
                                Chip(type.replaceFirstChar { it.uppercase() })
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f m  ·  %.1f kg",
                                detail.detail.heightMetres,
                                detail.detail.weightKilograms,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        Spacer(Modifier.height(12.dp))
                        DexEntryCard(detail.detail.dexEntry)
                        Spacer(Modifier.height(12.dp))
                        SectionLabel("Base stats")
                        Spacer(Modifier.height(4.dp))
                        detail.detail.stats.forEach { (name, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = name.replace('-', ' '),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                )
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Bone,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Gold)
                }
            }
        }
    }
}

/** The in-game Pokedex text, labelled with the game it comes from. */
@Composable
private fun DexEntryCard(entry: DexEntry?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(12.dp))
            .border(1.dp, Gold.copy(alpha = BORDER_ALPHA), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        SectionLabel(entry?.let { "Pokédex · ${it.game.displayName}" } ?: "Pokédex")
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry?.text ?: "No Pokédex entry available for this game right now.",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = if (entry == null) FontStyle.Normal else FontStyle.Italic,
            color = if (entry == null) Muted else Bone,
        )
    }
}

@Composable
private fun SpriteWithLabel(url: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PokemonSprite(
            url = url,
            contentDescription = label,
            modifier = Modifier.size(96.dp),
        )
        SectionLabel(label)
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(InkCard, RoundedCornerShape(12.dp))
            .border(1.dp, ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = ErrorRed,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text("Retry", color = Gold)
        }
    }
}

@Composable
private fun CentredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

private const val BORDER_ALPHA = 0.35f
