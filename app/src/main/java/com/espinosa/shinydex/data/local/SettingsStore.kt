package com.espinosa.shinydex.data.local

import android.content.Context
import com.espinosa.shinydex.data.model.Generation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remembers which generation the trainer picked.
 *
 * Exposed as a [StateFlow] because the choice scopes the whole app: the Pokedex, the
 * species picker and the sprite set all react to it.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _selectedGeneration = MutableStateFlow(readGeneration())
    val selectedGeneration: StateFlow<Generation?> = _selectedGeneration.asStateFlow()

    fun select(generation: Generation) {
        if (!generation.unlocked) return
        prefs.edit().putString(KEY_GENERATION, generation.name).apply()
        _selectedGeneration.value = generation
    }

    fun clear() {
        prefs.edit().remove(KEY_GENERATION).apply()
        _selectedGeneration.value = null
    }

    private fun readGeneration(): Generation? =
        prefs.getString(KEY_GENERATION, null)?.let { stored ->
            Generation.entries.firstOrNull { it.name == stored && it.unlocked }
        }

    private companion object {
        // MODE_PRIVATE only; nothing sensitive is stored here.
        const val FILE = "shinydex_settings"
        const val KEY_GENERATION = "selected_generation"
    }
}
