package com.espinosa.shinydex.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.espinosa.shinydex.ui.theme.ShinyDexTheme
import com.espinosa.shinydex.util.FaceOffCodes

class MainActivity : ComponentActivity() {

    /** Room code from a `shinydex://join/CODE` invite, waiting to be handed to the UI. */
    private val inviteCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readInvite(intent)
        setContent {
            ShinyDexTheme {
                ShinyDexRoot(
                    inviteCode = inviteCode.value,
                    onInviteConsumed = { inviteCode.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readInvite(intent)
    }

    /** Only a well-formed room code is ever taken from the link; anything else is ignored. */
    private fun readInvite(intent: Intent?) {
        FaceOffCodes.codeFromLink(intent?.dataString)?.let { inviteCode.value = it }
    }
}
