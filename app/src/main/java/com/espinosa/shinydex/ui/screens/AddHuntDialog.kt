package com.espinosa.shinydex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.ui.components.Chip
import com.espinosa.shinydex.ui.components.PokemonSprite
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.InkSoft
import com.espinosa.shinydex.ui.theme.Muted

/**
 * Start a hunt: pick the species, the game and the method.
 *
 * Everything on offer is scoped to [generation] -- its own species, its own games and the
 * methods that generation actually supports.
 */
@Composable
fun AddHuntDialog(
    generation: Generation,
    species: List<PokemonSummary>,
    onDismiss: () -> Unit,
    onConfirm: (PokemonSummary, GameVersion, HuntMethod, Int) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PokemonSummary?>(null) }
    var game by remember(generation) { mutableStateOf(GameVersion.defaultFor(generation)) }
    var method by remember(generation) {
        mutableStateOf(HuntMethod.defaultFor(GameVersion.defaultFor(generation)))
    }
    var startingCount by remember { mutableIntStateOf(0) }
    var startingCountText by remember { mutableStateOf("") }

    val methods = generation.methodsFor(game)
    // Switching game must not leave a method the new game cannot use (Odd Egg on Gold).
    val effectiveMethod = if (method in methods) method else HuntMethod.defaultFor(game)

    val matches = remember(query, species) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            species
        } else {
            species.filter {
                it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.id.toString() == trimmed.trimStart('#')
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = InkSoft,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp)
                .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Text(
                    text = "New hunt",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold,
                )
                Text(
                    text = generation.title + " · " + generation.region,
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search the " + generation.region + " dex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))

                if (species.isEmpty()) {
                    Text(
                        text = "The " + generation.region + " dex has not been downloaded " +
                            "yet. Open the Pokedex tab once while online and it will cache " +
                            "for offline use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(matches, key = { it.id }) { pokemon ->
                        SpeciesRow(
                            pokemon = pokemon,
                            isSelected = selected?.id == pokemon.id,
                            onClick = { selected = pokemon },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("Game")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    generation.games.forEach { entry ->
                        Box(modifier = Modifier.clickable { game = entry }) {
                            Chip(text = entry.displayName, filled = game == entry)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("Method")
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(methods, key = { it.name }) { entry ->
                        MethodRow(
                            method = entry,
                            isSelected = effectiveMethod == entry,
                            onClick = { method = entry },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = startingCountText,
                    onValueChange = { text ->
                        startingCountText = text.filter { it.isDigit() }.take(MAX_COUNT_DIGITS)
                        startingCount = startingCountText.toIntOrNull() ?: 0
                    },
                    label = { Text("Encounters so far (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Muted)
                    }
                    TextButton(
                        enabled = selected != null,
                        onClick = {
                            selected?.let { onConfirm(it, game, effectiveMethod, startingCount) }
                        },
                    ) {
                        Text(
                            text = "Start hunt",
                            color = if (selected == null) Muted else Gold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeciesRow(
    pokemon: PokemonSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) InkCard else InkSoft,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Gold else InkLine,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PokemonSprite(
            url = pokemon.shinySpriteUrl,
            contentDescription = pokemon.displayName,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = pokemon.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = Bone,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pokemon.dexNumber,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

@Composable
private fun MethodRow(
    method: HuntMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) InkCard else InkSoft,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Gold else InkLine,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = method.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Gold else Bone,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = method.oddsLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

private const val MAX_COUNT_DIGITS = 6
