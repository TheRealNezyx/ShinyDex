package com.espinosa.shinydex.util

import com.espinosa.shinydex.data.model.FaceOffMode

/**
 * Room codes and invite links.
 *
 * Codes use the same alphabet as the server (no 0/O, no 1/I) so one read aloud over a call
 * cannot be misheard. Invites carry a `shinydex://join/CODE` deep link that opens the app
 * straight onto the join screen.
 */
object FaceOffCodes {

    const val LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val SCHEME = "shinydex"
    private const val HOST = "join"

    /** Upper-cases and strips spaces or dashes; null when the result is not a real code. */
    fun normalize(raw: String?): String? {
        val code = raw.orEmpty().uppercase().filter { it.isLetterOrDigit() }
        return code.takeIf { it.length == LENGTH && it.all { c -> c in ALPHABET } }
    }

    /** What the join field keeps while the user types: code characters only, capped. */
    fun sanitizeInput(raw: String): String =
        raw.uppercase().filter { it in ALPHABET }.take(LENGTH)

    fun inviteLink(code: String): String = "$SCHEME://$HOST/$code"

    /** Pulls the code out of an invite link, or null if the link is not ours. */
    fun codeFromLink(link: String?): String? {
        val prefix = "$SCHEME://$HOST/"
        if (link == null || !link.startsWith(prefix, ignoreCase = true)) return null
        return normalize(link.substring(prefix.length).substringBefore('?').substringBefore('/'))
    }

    fun inviteText(code: String, mode: FaceOffMode, pokemonName: String?): String {
        val what = when (mode) {
            FaceOffMode.BATTLE -> "a shiny " + (pokemonName ?: "hunt") + " battle"
            FaceOffMode.LOUNGE -> "a shiny hunting lounge"
        }
        return "Join me in $what on ShinyDex! Room code: $code  " + inviteLink(code)
    }
}
