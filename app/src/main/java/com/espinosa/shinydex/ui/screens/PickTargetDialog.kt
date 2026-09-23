package com.espinosa.shinydex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.ui.components.PokemonSprite
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.InkSoft
import com.espinosa.shinydex.ui.theme.Muted

/** Pick a Pokemon from [generation]'s own dex plus a method, for a face-off room. */
@Composable
fun PickTargetDialog(
    title: String,
    subtitle: String,
    confirmLabel: String,
    generation: Generation,
    species: List<PokemonSummary>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PokemonSummary, HuntMethod) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PokemonSummary?>(null) }
    var method by remember(generation) { mutableStateOf(generation.methods.first()) }

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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = InkSoft,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Gold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search the " + generation.region + " dex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                if (species.isEmpty()) {
                    Text(
                        text = "Downloading the " + generation.region + " dex...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(matches, key = { it.id }) { pokemon ->
                        SelectableRow(
                            isSelected = selected?.id == pokemon.id,
                            onClick = { selected = pokemon },
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
                            Text(pokemon.dexNumber, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("Method")
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(generation.methods, key = { it.name }) { entry ->
                        SelectableRow(isSelected = method == entry, onClick = { method = entry }) {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (method == entry) Gold else Bone,
                                modifier = Modifier.weight(1f),
                            )
                            Text(entry.oddsLabel, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
                    TextButton(
                        enabled = selected != null && !busy,
                        onClick = { selected?.let { onConfirm(it, method) } },
                    ) {
                        Text(
                            text = if (busy) "Connecting..." else confirmLabel,
                            color = if (selected == null || busy) Muted else Gold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) InkCard else InkSoft, RoundedCornerShape(10.dp))
            .border(1.dp, if (isSelected) Gold else InkLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
