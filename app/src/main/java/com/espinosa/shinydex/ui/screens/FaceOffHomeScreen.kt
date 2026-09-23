package com.espinosa.shinydex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.espinosa.shinydex.data.model.FaceOffMode
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.ErrorRed
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.Muted
import com.espinosa.shinydex.ui.viewmodel.FaceOffUiState
import com.espinosa.shinydex.util.FaceOffCodes

/** Face-off lobby: set a name, then start a Battle or a Lounge, or join one with a code. */
@Composable
fun FaceOffHomeScreen(
    state: FaceOffUiState,
    generation: Generation?,
    pickerSpecies: List<PokemonSummary>,
    onNameChange: (String) -> Unit,
    onServerChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPreparePicker: (Generation) -> Unit,
    onCreate: (FaceOffMode, Generation, PokemonSummary, HuntMethod) -> Unit,
    onJoin: () -> Unit,
    onConfirmLoungeJoin: (PokemonSummary, HuntMethod) -> Unit,
    onCancelLoungeJoin: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf<FaceOffMode?>(null) }
    var showServer by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("Face-off", style = MaterialTheme.typography.titleLarge, color = Gold)
            Text(
                text = "Hunt with friends, live. Share a room code and race or relax together.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        }

        state.error?.let { ErrorCard(it, onDismissError) }

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Your trainer name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(
            if (generation == null) "Start a room" else "Start a " + generation.title + " room",
        )
        FaceOffMode.entries.forEach { mode ->
            ModeCard(
                mode = mode,
                icon = if (mode == FaceOffMode.BATTLE) Icons.Filled.EmojiEvents else Icons.Filled.Groups,
                enabled = generation != null && !state.busy,
                onClick = {
                    generation?.let { onPreparePicker(it) }
                    creating = mode
                },
            )
        }

        SectionLabel("Join a room")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.codeInput,
                onValueChange = onCodeChange,
                label = { Text("Room code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(10.dp))
            GoldButton(
                label = if (state.busy) "..." else "JOIN",
                enabled = !state.busy && state.codeInput.length == FaceOffCodes.LENGTH,
                onClick = onJoin,
            )
        }

        Column {
            Text(
                text = (if (showServer) "Hide" else "Server") + " · " + state.serverUrl,
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.clickable { showServer = !showServer },
            )
            if (showServer) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = onServerChange,
                    label = { Text("Face-off server") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Emulator: 10.0.2.2:8080. A phone on the same Wi-Fi: the laptop's IP, " +
                        "for example 192.168.1.20:8080. Everyone in a room must use the same server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    val mode = creating
    if (mode != null && generation != null) {
        PickTargetDialog(
            title = mode.label + " room",
            subtitle = if (mode == FaceOffMode.BATTLE) {
                "Everyone will hunt the Pokemon you pick"
            } else {
                "Pick the Pokemon you will hunt"
            },
            confirmLabel = "Create room",
            generation = generation,
            species = pickerSpecies,
            busy = state.busy,
            onDismiss = { creating = null },
            onConfirm = { pokemon, method ->
                onCreate(mode, generation, pokemon, method)
                creating = null
            },
        )
    }

    state.pendingLounge?.let { room ->
        PickTargetDialog(
            title = "Join lounge " + room.code,
            subtitle = room.generation.title + " · pick the Pokemon you will hunt",
            confirmLabel = "Join",
            generation = room.generation,
            species = pickerSpecies,
            busy = state.busy,
            onDismiss = onCancelLoungeJoin,
            onConfirm = onConfirmLoungeJoin,
        )
    }
}

@Composable
private fun ModeCard(mode: FaceOffMode, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(16.dp))
            .border(1.dp, if (enabled) Gold.copy(alpha = 0.5f) else InkLine, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Gold.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Gold)
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(mode.label, style = MaterialTheme.typography.titleMedium, color = Bone)
            Text(mode.tagline, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
internal fun GoldButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (enabled) Gold else InkCard, RoundedCornerShape(12.dp))
            .border(1.dp, if (enabled) Gold else InkLine, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else Muted,
        )
    }
}

@Composable
internal fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        TextButton(onClick = onDismiss) { Text("OK", color = Gold) }
    }
}
