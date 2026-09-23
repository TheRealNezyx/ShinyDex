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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.espinosa.shinydex.data.model.Hunt
import com.espinosa.shinydex.ui.components.Chip
import com.espinosa.shinydex.ui.components.PokeballLogo
import com.espinosa.shinydex.ui.components.PokemonSprite
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.Muted

/** The hunt list, with in-place add / subtract on every card. */
@Composable
fun HuntsScreen(
    hunts: List<Hunt>,
    onOpenHunt: (Long) -> Unit,
    onAdjust: (Long, Int) -> Unit,
    onAddHunt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (hunts.isEmpty()) {
            EmptyHunts(onAddHunt)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HuntsSummary(hunts) }
                items(hunts, key = { it.id }) { hunt ->
                    HuntCard(
                        hunt = hunt,
                        onClick = { onOpenHunt(hunt.id) },
                        onAdjust = { delta -> onAdjust(hunt.id, delta) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddHunt,
            containerColor = Gold,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Start a new hunt")
        }
    }
}

@Composable
private fun HuntsSummary(hunts: List<Hunt>) {
    val active = hunts.count { !it.isFound }
    val found = hunts.count { it.isFound }
    val encounters = hunts.sumOf { it.encounters }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(16.dp))
            .border(1.dp, InkLine, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SummaryStat("Active", active.toString())
        SummaryStat("Found", found.toString())
        SummaryStat("Encounters", encounters.toString())
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Gold)
        SectionLabel(label)
    }
}

@Composable
private fun HuntCard(
    hunt: Hunt,
    onClick: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = if (hunt.isFound) Gold else InkLine,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PokemonSprite(
                url = hunt.pokemon.shinySpriteUrl,
                contentDescription = "Shiny " + hunt.pokemon.displayName,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hunt.pokemon.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Bone,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(hunt.game.shortName)
                    Chip(hunt.method.label, accent = Muted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = hunt.encounters.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (hunt.isFound) Gold else Bone,
                )
                SectionLabel(if (hunt.isFound) "Found!" else hunt.oddsLabel)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CounterButton(
                icon = Icons.Filled.Remove,
                description = "Subtract one encounter",
                onClick = { onAdjust(-1) },
            )
            Spacer(Modifier.size(10.dp))
            CounterButton(
                icon = Icons.Filled.Add,
                description = "Add one encounter",
                onClick = { onAdjust(1) },
                emphasised = true,
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { hunt.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Gold,
                    trackColor = InkLine,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hunt.probabilityLabel + " chance by now",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun CounterButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    emphasised: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = if (emphasised) Gold else InkCard,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = if (emphasised) Gold else InkLine,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (emphasised) MaterialTheme.colorScheme.onPrimary else Bone,
        )
    }
}

@Composable
private fun EmptyHunts(onAddHunt: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PokeballLogo(modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No hunts yet",
            style = MaterialTheme.typography.titleLarge,
            color = Bone,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap + to pick a Pokemon, a game and a method, then start counting.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .background(Gold, RoundedCornerShape(12.dp))
                .clickable(onClick = onAddHunt)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "START A HUNT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
