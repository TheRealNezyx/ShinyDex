package com.espinosa.shinydex.ui.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.espinosa.shinydex.ShinyDexApp

/** Wires the [com.espinosa.shinydex.AppContainer] into every ViewModel in the app. */
object ViewModelFactories {

    val Factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as ShinyDexApp
            GenerationViewModel(app.container.settings)
        }
        initializer {
            val app = this[APPLICATION_KEY] as ShinyDexApp
            HuntsViewModel(
                hunts = app.container.huntRepository,
                pokedex = app.container.pokedexRepository,
                settings = app.container.settings,
            )
        }
        initializer {
            val app = this[APPLICATION_KEY] as ShinyDexApp
            PokedexViewModel(
                repository = app.container.pokedexRepository,
                settings = app.container.settings,
            )
        }
        initializer {
            val app = this[APPLICATION_KEY] as ShinyDexApp
            FaceOffViewModel(
                repository = app.container.faceOffRepository,
                session = app.container.faceOffSession,
                pokedex = app.container.pokedexRepository,
            )
        }
    }
}
