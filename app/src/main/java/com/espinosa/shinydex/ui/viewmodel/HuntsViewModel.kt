package com.espinosa.shinydex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espinosa.shinydex.data.local.SettingsStore
import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.Hunt
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.data.repo.HuntRepository
import com.espinosa.shinydex.data.repo.PokedexRepository
import com.espinosa.shinydex.util.MotivationalMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A milestone worth a pop-up: fired every [MotivationalMessages.MILESTONE_INTERVAL] encounters. */
data class Milestone(val huntId: Long, val count: Int, val message: String)

@OptIn(ExperimentalCoroutinesApi::class)
class HuntsViewModel(
    private val hunts: HuntRepository,
    pokedex: PokedexRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val generation: StateFlow<Generation?> = settings.selectedGeneration

    val allHunts: StateFlow<List<Hunt>> = hunts.observeHunts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /**
     * Species available to pick when starting a hunt: only the ones introduced in the
     * generation the trainer selected.
     */
    val species: StateFlow<List<PokemonSummary>> =
        settings.selectedGeneration
            .flatMapLatest { generation ->
                if (generation == null) flowOf(emptyList()) else pokedex.observePokedex(generation)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _milestone = MutableStateFlow<Milestone?>(null)
    val milestone: StateFlow<Milestone?> = _milestone.asStateFlow()

    fun huntById(id: Long): Hunt? = allHunts.value.firstOrNull { it.id == id }

    fun addHunt(
        pokemon: PokemonSummary,
        game: GameVersion,
        method: HuntMethod,
        startingCount: Int = 0,
    ) {
        val generation = settings.selectedGeneration.value ?: game.generation
        viewModelScope.launch {
            hunts.addHunt(
                pokemon = pokemon,
                generation = generation,
                game = game,
                method = method,
                startingCount = startingCount,
            )
        }
    }

    /** Adds [delta] encounters and raises a [Milestone] when the new total lands on one. */
    fun adjust(huntId: Long, delta: Int) {
        viewModelScope.launch {
            val updated = hunts.adjustEncounters(huntId, delta) ?: return@launch
            if (delta > 0 && MotivationalMessages.isMilestone(updated)) {
                _milestone.value = Milestone(
                    huntId = huntId,
                    count = updated,
                    message = MotivationalMessages.forCount(updated),
                )
            }
        }
    }

    fun dismissMilestone() {
        _milestone.value = null
    }

    fun setFound(huntId: Long, found: Boolean) {
        viewModelScope.launch { hunts.setFound(huntId, found) }
    }

    fun resetCounter(huntId: Long) {
        viewModelScope.launch { hunts.resetCounter(huntId) }
    }

    fun delete(huntId: Long) {
        viewModelScope.launch { hunts.delete(huntId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
