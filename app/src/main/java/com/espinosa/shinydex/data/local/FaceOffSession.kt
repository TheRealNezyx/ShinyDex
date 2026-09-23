package com.espinosa.shinydex.data.local

import android.content.Context

/** The face-off room this device is currently in, so a restart drops you back into it. */
data class ActiveFaceOff(val code: String, val playerId: String, val token: String)

/**
 * Face-off preferences: display name, server address and the active room.
 *
 * The player token is a per-room secret that only lets this device change its own counter.
 * It lives in MODE_PRIVATE preferences, which data_extraction_rules.xml already keeps out of
 * cloud backups and device transfers.
 */
class FaceOffSession(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var displayName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, null) ?: DEFAULT_SERVER
        set(value) = prefs.edit().putString(KEY_SERVER, value).apply()

    var active: ActiveFaceOff?
        get() {
            val code = prefs.getString(KEY_CODE, null) ?: return null
            val id = prefs.getString(KEY_PLAYER, null) ?: return null
            val token = prefs.getString(KEY_TOKEN, null) ?: return null
            return ActiveFaceOff(code, id, token)
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_CODE)
                    remove(KEY_PLAYER)
                    remove(KEY_TOKEN)
                } else {
                    putString(KEY_CODE, value.code)
                    putString(KEY_PLAYER, value.playerId)
                    putString(KEY_TOKEN, value.token)
                }
            }.apply()
        }

    companion object {
        /** The Android emulator reaches the host machine's localhost at 10.0.2.2. */
        const val DEFAULT_SERVER = "http://10.0.2.2:8080"

        private const val FILE = "shinydex_faceoff"
        private const val KEY_NAME = "display_name"
        private const val KEY_SERVER = "server_url"
        private const val KEY_CODE = "room_code"
        private const val KEY_PLAYER = "player_id"
        private const val KEY_TOKEN = "player_token"
    }
}
