package com.espinosa.shinydex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkCard
import com.espinosa.shinydex.ui.theme.Muted

/** A small pill used for games, methods and status. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Gold,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .background(
                color = if (filled) accent else InkCard,
                shape = RoundedCornerShape(50),
            )
            .border(
                width = 1.dp,
                color = if (filled) accent else accent.copy(alpha = BORDER_ALPHA),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** An all-caps section header. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Muted,
        modifier = modifier,
    )
}

/** A Pokemon sprite loaded from the PokeAPI sprite CDN. */
@Composable
fun PokemonSprite(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

private const val BORDER_ALPHA = 0.35f
