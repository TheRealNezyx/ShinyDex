package com.espinosa.shinydex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espinosa.shinydex.data.local.SettingsStore
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.PokemonDetail
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.data.repo.PokedexRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the Pokedex entry sheet: loading, loaded or failed. */
sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val detail: PokemonDetail) : DetailState
    data class Failed(val message: String) : DetailState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PokedexViewModel(
    private val repository: PokedexRepository,
    settings: SettingsStore,
) : ViewModel() {

    val generation: StateFlow<Generation?> = settings.selectedGeneration

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _showShiny = MutableStateFlow(true)
    val showShiny: StateFlow<Boolean> = _showShiny.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selected = MutableStateFlow<PokemonSummary?>(null)
    val selected: StateFlow<PokemonSummary?> = _selected.asStateFlow()

    private val _detail = MutableStateFlow<DetailState?>(null)
    val detail: StateFlow<DetailState?> = _detail.asStateFlow()

    private val allEntries: StateFlow<List<PokemonSummary>> =
        settings.selectedGeneration
            .flatMapLatest { generation ->
                if (generation == null) flowOf(emptyList()) else repository.observePokedex(generation)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val entries: StateFlow<List<PokemonSummary>> =
        combine(allEntries, _query) { all, query ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                all
            } else {
                all.filter { entry ->
                    entry.displayName.contains(trimmed, ignoreCase = true) ||
                        entry.id.toString() == trimmed.trimStart('#', '0').ifEmpty { "0" }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        // Switching generation clears the old selection and downloads the new dex once.
        viewModelScope.launch {
            settings.selectedGeneration.filterNotNull().collect { generation ->
                clearSelection()
                _query.value = ""
                if (!repository.isCached(generation)) refresh()
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleShiny() {
        _showShiny.value = !_showShiny.value
    }

    fun dismissError() {
        _error.value = null
    }

    fun refresh() {
        val generation = this.generation.value ?: return
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refresh(generation)
                .onFailure { _error.value = it.message ?: "Could not reach PokeAPI." }
                .onSuccess { _error.value = null }
            _isRefreshing.value = false
        }
    }

    fun select(pokemon: PokemonSummary) {
        _selected.value = pokemon
        _detail.value = DetailState.Loading
        viewModelScope.launch {
            repository.detail(pokemon.id)
                .onSuccess { _detail.value = DetailState.Loaded(it) }
                .onFailure {
                    _detail.value = DetailState.Failed(
                        it.message ?: "Could not load this entry.",
                    )
                }
        }
    }

    fun clearSelection() {
        _selected.value = null
        _detail.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
