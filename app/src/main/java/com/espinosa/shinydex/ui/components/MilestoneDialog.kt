package com.espinosa.shinydex.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.espinosa.shinydex.ui.theme.Bone
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.InkSoft

/** The every-hundred-encounters pep talk. */
@Composable
fun MilestoneDialog(
    count: Int,
    message: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = InkSoft,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(2.dp, Gold, RoundedCornerShape(20.dp)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PokeballLogo(modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = count.toString() + " ENCOUNTERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Gold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Bone,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("KEEP HUNTING", color = Gold)
                }
            }
        }
    }
}
