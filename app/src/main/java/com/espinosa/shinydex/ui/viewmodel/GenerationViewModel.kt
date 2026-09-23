package com.espinosa.shinydex.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.espinosa.shinydex.data.local.SettingsStore
import com.espinosa.shinydex.data.model.Generation
import kotlinx.coroutines.flow.StateFlow

/** Owns the "which generation am I hunting in" choice. */
class GenerationViewModel(private val settings: SettingsStore) : ViewModel() {

    val selected: StateFlow<Generation?> = settings.selectedGeneration

    fun select(generation: Generation) = settings.select(generation)

    fun clear() = settings.clear()
}
