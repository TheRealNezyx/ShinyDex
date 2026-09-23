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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.espinosa.shinydex.data.model.FaceOffMode
import com.espinosa.shinydex.data.model.FaceOffPlayer
import com.espinosa.shinydex.data.model.FaceOffRoom
import com.espinosa.shinydex.ui.components.Chip
import com.espinosa.shinydex.ui.components.PokemonSprite
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.GoldBright
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.Muted

/** A live face-off room: the target (battle), your counter and everyone's standings. */
@Composable
fun FaceOffRoomScreen(
    room: FaceOffRoom,
    myPlayerId: String?,
    connectionLost: Boolean,
    error: String?,
    onAdjust: (Int) -> Unit,
    onFound: () -> Unit,
    onLeave: () -> Unit,
    onShare: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me = room.player(myPlayerId)
    var confirmFound by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { RoomHeader(room, onShare = onShare, onLeave = { confirmLeave = true }) }

        if (connectionLost) item { Notice("Reconnecting to the face-off server...") }
        error?.let { item { ErrorCard(it, onDismissError) } }
        finishedMessage(room, myPlayerId)?.let { item { Banner(it) } }

        item {
            if (room.mode == FaceOffMode.BATTLE) BattleTarget(room) else LoungeSummary(room)
        }

        if (me != null) {
            item {
                MyPanel(
                    me = me,
                    locked = room.finished || me.found,
                    onAdjust = onAdjust,
                    onFound = { confirmFound = true },
                )
            }
        }

        item { SectionLabel("Standings · " + room.players.size + " / " + room.maxPlayers) }
        itemsIndexed(room.players, key = { _, p -> p.id }) { index, player ->
            PlayerRow(
                rank = index + 1,
                player = player,
                isMe = player.id == myPlayerId,
                isWinner = player.id == room.winnerId,
                showPokemon = room.mode == FaceOffMode.LOUNGE,
            )
        }
    }

    if (confirmFound && me != null) {
        AlertDialog(
            onDismissRequest = { confirmFound = false },
            title = { Text("Shiny " + me.pokemon.displayName + "?") },
            text = {
                Text(
                    if (room.mode == FaceOffMode.BATTLE) {
                        "This ends the battle for everyone and crowns you the winner. Only " +
                            "confirm once it is actually sparkling."
                    } else {
                        "Your counter stops here and the room gets to celebrate."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFound = false
                    onFound()
                }) { Text("It's shiny!", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmFound = false }) { Text("Not yet", color = Muted) }
            },
            containerColor = InkCard,
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave room " + room.code + "?") },
            text = { Text("Your counter in this room is lost when you leave.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onLeave()
                }) { Text("Leave", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("Stay", color = Muted) }
            },
            containerColor = InkCard,
        )
    }
}

private fun finishedMessage(room: FaceOffRoom, myPlayerId: String?): String? {
    if (!room.finished) return null
    return when (room.mode) {
        FaceOffMode.BATTLE ->
            if (room.winnerId == myPlayerId) {
                "You won! Shiny " + (room.target?.displayName ?: "") + " is yours."
            } else {
                (room.winnerName ?: "Someone") + " found the shiny " +
                    (room.target?.displayName ?: "") + " first."
            }
        FaceOffMode.LOUNGE -> "Everyone in the lounge found their shiny!"
    }
}

@Composable
private fun RoomHeader(room: FaceOffRoom, onShare: () -> Unit, onLeave: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(room.mode.label, filled = true)
                Chip("Gen " + room.generation.romanNumeral, accent = Muted)
            }
            Spacer(Modifier.height(6.dp))
            Text("Room " + room.code, style = MaterialTheme.typography.titleLarge, color = Bone)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Gold, CircleShape)
                .clickable(onClick = onShare),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share room code",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        TextButton(onClick = onLeave) { Text("LEAVE", color = Muted) }
    }
}

@Composable
private fun BattleTarget(room: FaceOffRoom) {
    val target = room.target ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(18.dp))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionLabel("Everyone is hunting")
        PokemonSprite(
            url = target.shinyArtworkUrl,
            contentDescription = "Shiny " + target.displayName,
            modifier = Modifier.size(120.dp),
        )
        Text(target.displayName, style = MaterialTheme.typography.titleLarge, color = Gold)
        Text(
            text = (room.targetMethod ?: "") + " · 1 / " + room.targetOdds,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(10.dp))
        Text(room.roomProbabilityLabel, style = MaterialTheme.typography.displaySmall, color = Bone)
        Text(
            text = "chance someone has hit it · " + room.totalEncounters + " encounters combined",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoungeSummary(room: FaceOffRoom) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(16.dp))
            .border(1.dp, InkLine, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stat("Hunters", room.players.size.toString())
        Stat("Found", room.foundCount.toString() + " / " + room.players.size)
        Stat("Encounters", room.totalEncounters.toString())
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Gold)
        SectionLabel(label)
    }
}

@Composable
private fun MyPanel(me: FaceOffPlayer, locked: Boolean, onAdjust: (Int) -> Unit, onFound: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(18.dp))
            .border(1.dp, Gold, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PokemonSprite(
                url = me.pokemon.shinySpriteUrl,
                contentDescription = me.pokemon.displayName,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("You · " + me.pokemon.displayName)
                Text(
                    text = me.probabilityLabel + " chance by now",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
            Text(me.encounters.toString(), style = MaterialTheme.typography.displaySmall, color = Bone)
        }

        Spacer(Modifier.height(12.dp))
        if (me.found) {
            Text(
                text = "You found yours. Shiny secured!",
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CounterKey("-1", !locked, Modifier.weight(1f)) { onAdjust(-1) }
                CounterKey("+1", !locked, Modifier.weight(1f), emphasised = true) { onAdjust(1) }
                CounterKey("+10", !locked, Modifier.weight(1f), emphasised = true) { onAdjust(BULK_STEP) }
            }
            Spacer(Modifier.height(8.dp))
            GoldButton(
                label = "FOUND IT!",
                enabled = !locked,
                onClick = onFound,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CounterKey(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    onClick: () -> Unit,
) {
    val filled = emphasised && enabled
    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (filled) Gold else InkCard, RoundedCornerShape(12.dp))
            .border(1.dp, if (filled) Gold else InkLine, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                filled -> MaterialTheme.colorScheme.onPrimary
                enabled -> Bone
                else -> Muted
            },
        )
    }
}

@Composable
private fun PlayerRow(
    rank: Int,
    player: FaceOffPlayer,
    isMe: Boolean,
    isWinner: Boolean,
    showPokemon: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = when {
                    isWinner || player.found -> GoldBright
                    isMe -> Gold.copy(alpha = 0.6f)
                    else -> InkLine
                },
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelLarge,
            color = Muted,
            modifier = Modifier.size(width = 32.dp, height = 20.dp),
        )
        PokemonSprite(
            url = player.pokemon.shinySpriteUrl,
            contentDescription = player.pokemon.displayName,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (player.online) Gold else InkLine, CircleShape),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = player.name + if (isMe) " (you)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Bone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = (if (showPokemon) player.pokemon.displayName + " · " else "") +
                    player.method + " · " + player.probabilityLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = player.encounters.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (player.found) Gold else Bone,
            )
            if (player.found) SectionLabel(if (isWinner) "Winner" else "Shiny!")
        }
    }
}

@Composable
private fun Banner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gold, RoundedCornerShape(14.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.fillMaxWidth())
}

private const val BULK_STEP = 10
