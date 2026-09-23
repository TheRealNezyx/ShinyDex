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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.espinosa.shinydex.data.model.Hunt
import com.espinosa.shinydex.ui.components.Chip
import com.espinosa.shinydex.ui.components.PokemonSprite
import com.espinosa.shinydex.ui.components.SectionLabel
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.ErrorRed
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.InkLine
import com.espinosa.shinydex.ui.theme.Muted

/** One hunt, full screen: the big counter plus everything known about the method. */
@Composable
fun HuntDetailScreen(
    hunt: Hunt,
    onBack: () -> Unit,
    onAdjust: (Int) -> Unit,
    onToggleFound: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to hunts",
                tint = Gold,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = hunt.pokemon.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = Bone,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PokemonSprite(
                url = hunt.pokemon.shinyArtworkUrl,
                contentDescription = "Shiny " + hunt.pokemon.displayName,
                modifier = Modifier.size(170.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(hunt.pokemon.dexNumber, accent = Muted)
                Chip(hunt.game.displayName.removePrefix("Pokemon "))
                Chip(hunt.method.label, accent = Muted)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = hunt.encounters.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontSize = MaterialTheme.typography.displaySmall.fontSize * COUNTER_SCALE,
            color = if (hunt.isFound) Gold else Bone,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel(
            text = if (hunt.isFound) "Shiny secured" else "Encounters",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdjustButton("-10", Modifier.weight(1f)) { onAdjust(-BULK_STEP) }
            AdjustButton("-1", Modifier.weight(1f)) { onAdjust(-1) }
            AdjustButton("+1", Modifier.weight(1f), emphasised = true) { onAdjust(1) }
            AdjustButton("+10", Modifier.weight(1f), emphasised = true) { onAdjust(BULK_STEP) }
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(InkCard, RoundedCornerShape(16.dp))
                .border(1.dp, InkLine, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock("Odds", hunt.oddsLabel, Modifier.weight(1f))
                StatBlock("Chance by now", hunt.probabilityLabel, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { hunt.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Gold,
                trackColor = InkLine,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Progress towards one full odds cycle (" +
                    hunt.method.oddsDenominator + " encounters).",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(InkCard, RoundedCornerShape(16.dp))
                .border(1.dp, InkLine, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            SectionLabel("How this method works")
            Spacer(Modifier.height(6.dp))
            Text(
                text = hunt.method.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Bone,
            )
        }

        Spacer(Modifier.height(20.dp))

        ActionRow(
            label = if (hunt.isFound) "Mark as still hunting" else "Mark as found",
            accent = Gold,
            onClick = onToggleFound,
        )
        Spacer(Modifier.height(10.dp))
        ActionRow(label = "Reset counter to zero", accent = Muted, onClick = onReset)
        Spacer(Modifier.height(10.dp))
        ActionRow(label = "Delete this hunt", accent = ErrorRed, onClick = onDelete)

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Gold)
    }
}

@Composable
private fun AdjustButton(
    label: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .background(
                color = if (emphasised) Gold else InkCard,
                shape = RoundedCornerShape(14.dp),
            )
            .border(
                width = 1.dp,
                color = if (emphasised) Gold else InkLine,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (emphasised) MaterialTheme.colorScheme.onPrimary else Bone,
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkCard, RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = ACTION_BORDER_ALPHA), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = accent)
    }
}

private const val BULK_STEP = 10
private const val COUNTER_SCALE = 1.6f
private const val ACTION_BORDER_ALPHA = 0.5f
