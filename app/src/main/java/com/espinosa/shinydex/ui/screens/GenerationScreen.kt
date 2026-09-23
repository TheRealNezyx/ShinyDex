package com.espinosa.shinydex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.ui.components.Chip
import com.espinosa.shinydex.ui.components.PokeballLogo
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.InkSoft
import com.espinosa.shinydex.ui.theme.Muted

/** Entry screen: pick the generation you are hunting in. */
@Composable
fun GenerationScreen(
    selected: Generation?,
    onSelect: (Generation) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PokeballLogo(modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "SHINYDEX",
                    style = MaterialTheme.typography.displaySmall,
                    color = Gold,
                )
                Text(
                    text = "Shiny hunt tracker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
            }
        }

        item { SectionLabel("Choose your generation", Modifier.padding(vertical = 4.dp)) }

        items(Generation.entries.toList(), key = { it.name }) { generation ->
            GenerationCard(
                generation = generation,
                isSelected = generation == selected,
                onClick = { onSelect(generation) },
            )
        }

        item {
            Text(
                text = "Shiny Pokemon were introduced in Generation II, so Gen I has " +
                    "nothing to hunt. Picking a generation scopes the whole app to that " +
                    "region: only its Pokemon, its games and its methods.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun GenerationCard(
    generation: Generation,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val enabled = generation.unlocked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) InkCard else InkSoft,
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Gold
                    enabled -> Gold.copy(alpha = 0.4f)
                    else -> InkLine
                },
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = generation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else Muted,
                )
                Spacer(Modifier.size(8.dp))
                if (enabled) {
                    Chip(text = generation.region, filled = isSelected)
                } else {
                    Chip(text = "Locked", accent = Muted)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = generation.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            if (enabled) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "#" + generation.dexRange.first + " - #" + generation.dexRange.last +
                        "  ·  " + generation.speciesCount + " species",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold.copy(alpha = 0.75f),
                )
            }
        }

        Icon(
            imageVector = if (enabled) Icons.Filled.ChevronRight else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (enabled) Gold else Muted,
        )
    }
}
