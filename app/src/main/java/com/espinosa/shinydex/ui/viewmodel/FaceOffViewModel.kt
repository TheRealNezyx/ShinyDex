package com.espinosa.shinydex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espinosa.shinydex.data.local.ActiveFaceOff
import com.espinosa.shinydex.data.local.FaceOffSession
import com.espinosa.shinydex.data.model.FaceOffMode
import com.espinosa.shinydex.data.model.FaceOffRoom
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.data.repo.FaceOffError
import com.espinosa.shinydex.data.repo.FaceOffRepository
import com.espinosa.shinydex.data.repo.FaceOffSeat
import com.espinosa.shinydex.data.repo.PokedexRepository
import com.espinosa.shinydex.util.FaceOffCodes
import com.espinosa.shinydex.util.MotivationalMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FaceOffUiState(
    val name: String = "",
    val serverUrl: String = FaceOffSession.DEFAULT_SERVER,
    val codeInput: String = "",
    val room: FaceOffRoom? = null,
    val myPlayerId: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    /** True while polling keeps failing; the last known room stays on screen. */
    val connectionLost: Boolean = false,
    /** A lounge room we are about to join, waiting for our Pokemon pick. */
    val pendingLounge: FaceOffRoom? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FaceOffViewModel(
    private val repository: FaceOffRepository,
    private val session: FaceOffSession,
    private val pokedex: PokedexRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        FaceOffUiState(name = session.displayName, serverUrl = session.serverUrl),
    )
    val state: StateFlow<FaceOffUiState> = _state.asStateFlow()

    private val _milestone = MutableStateFlow<Milestone?>(null)
    val milestone: StateFlow<Milestone?> = _milestone.asStateFlow()

    /** Which generation the species picker is currently showing. */
    private val pickerGeneration = MutableStateFlow<Generation?>(null)

    val pickerSpecies: StateFlow<List<PokemonSummary>> = pickerGeneration
        .flatMapLatest { if (it == null) flowOf(emptyList()) else pokedex.observePokedex(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private var pollJob: Job? = null

    /** Bumped on every write, so a poll that started before it cannot paint stale data. */
    private var writeVersion = 0

    init {
        resume()
    }

    /* ------------------------------------------------ form ------------- */

    fun onNameChange(value: String) {
        val name = value.take(MAX_NAME)
        session.displayName = name
        _state.update { it.copy(name = name) }
    }

    fun onServerChange(value: String) {
        session.serverUrl = value.trim()
        _state.update { it.copy(serverUrl = value.trim()) }
    }

    fun onCodeChange(value: String) {
        _state.update { it.copy(codeInput = FaceOffCodes.sanitizeInput(value)) }
    }

    /** Called when the app is opened from an invite link. */
    fun prefillCode(code: String) {
        if (_state.value.room == null) _state.update { it.copy(codeInput = code) }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun dismissMilestone() {
        _milestone.value = null
    }

    /** Points the species picker at [generation] and downloads that dex if needed. */
    fun preparePicker(generation: Generation) {
        pickerGeneration.value = generation
        viewModelScope.launch {
            if (!pokedex.isCached(generation)) pokedex.refresh(generation)
        }
    }

    /* ------------------------------------------------ rooms ------------ */

    fun create(mode: FaceOffMode, generation: Generation, pokemon: PokemonSummary, method: HuntMethod) {
        val name = requireName() ?: return
        launchBusy {
            repository.create(_state.value.serverUrl, mode, generation, name, pokemon, method)
                .onSuccess(::enter)
                .onFailure(::showError)
        }
    }

    /** The Join button: look first, because a lounge needs a Pokemon before we can sit down. */
    fun lookUpRoom() {
        val name = requireName() ?: return
        val code = FaceOffCodes.normalize(_state.value.codeInput)
        if (code == null) {
            _state.update { it.copy(error = "Room codes are ${FaceOffCodes.LENGTH} letters and numbers.") }
            return
        }
        launchBusy {
            repository.peek(_state.value.serverUrl, code)
                .onSuccess { room ->
                    when {
                        room.finished -> showError(FaceOffError("That face-off is already over."))
                        room.mode == FaceOffMode.BATTLE -> join(code, name, null, null)
                        else -> {
                            preparePicker(room.generation)
                            _state.update { it.copy(pendingLounge = room, busy = false) }
                        }
                    }
                }
                .onFailure(::showError)
        }
    }

    fun confirmLoungeJoin(pokemon: PokemonSummary, method: HuntMethod) {
        val room = _state.value.pendingLounge ?: return
        val name = requireName() ?: return
        launchBusy { join(room.code, name, pokemon, method) }
    }

    fun cancelLoungeJoin() = _state.update { it.copy(pendingLounge = null) }

    private suspend fun join(code: String, name: String, pokemon: PokemonSummary?, method: HuntMethod?) {
        repository.join(_state.value.serverUrl, code, name, pokemon, method)
            .onSuccess(::enter)
            .onFailure(::showError)
    }

    private fun enter(seat: FaceOffSeat) {
        session.active = ActiveFaceOff(seat.room.code, seat.playerId, seat.token)
        _state.update {
            it.copy(
                room = seat.room,
                myPlayerId = seat.playerId,
                pendingLounge = null,
                busy = false,
                error = null,
                codeInput = "",
                connectionLost = false,
            )
        }
        startPolling()
    }

    /* ------------------------------------------------ in the room ------ */

    fun adjust(delta: Int) {
        val active = session.active ?: return
        writeVersion++
        viewModelScope.launch {
            repository.addEncounters(_state.value.serverUrl, active.code, active.token, delta)
                .onSuccess { room ->
                    _state.update { it.copy(room = room, connectionLost = false) }
                    val mine = room.player(active.playerId)?.encounters ?: return@onSuccess
                    if (delta > 0 && MotivationalMessages.isMilestone(mine)) {
                        _milestone.value = Milestone(huntId = -1, count = mine,
                            message = MotivationalMessages.forCount(mine))
                    }
                }
                .onFailure(::showError)
        }
    }

    fun markFound() {
        val active = session.active ?: return
        writeVersion++
        viewModelScope.launch {
            repository.markFound(_state.value.serverUrl, active.code, active.token)
                .onSuccess { room -> _state.update { it.copy(room = room) } }
                .onFailure(::showError)
        }
    }

    fun leave() {
        val active = session.active
        pollJob?.cancel()
        session.active = null
        _state.update { it.copy(room = null, myPlayerId = null, connectionLost = false) }
        if (active != null) {
            // Best effort: if the server is unreachable the room simply expires on its own.
            viewModelScope.launch {
                repository.leave(_state.value.serverUrl, active.code, active.token)
            }
        }
    }

    /* ------------------------------------------------ syncing ---------- */

    private fun resume() {
        val active = session.active ?: return
        _state.update { it.copy(myPlayerId = active.playerId) }
        viewModelScope.launch {
            repository.room(_state.value.serverUrl, active.code, active.token)
                .onSuccess { room ->
                    if (room.player(active.playerId) == null) {
                        dropRoom("You are no longer in room ${active.code}.")
                    } else {
                        _state.update { it.copy(room = room) }
                        startPolling()
                    }
                }
                .onFailure { e ->
                    if (e.isRoomGone()) dropRoom("Room ${active.code} has closed.") else startPolling()
                }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val active = session.active ?: break
                val versionAtStart = writeVersion
                repository.room(_state.value.serverUrl, active.code, active.token)
                    .onSuccess { room ->
                        if (room.player(active.playerId) == null) {
                            dropRoom("You are no longer in room ${active.code}.")
                        } else if (versionAtStart == writeVersion) {
                            _state.update { it.copy(room = room, connectionLost = false) }
                        }
                    }
                    .onFailure { e ->
                        if (e.isRoomGone()) {
                            dropRoom("Room ${active.code} has closed.")
                        } else {
                            _state.update { it.copy(connectionLost = true) }
                        }
                    }
            }
        }
    }

    private fun dropRoom(message: String) {
        pollJob?.cancel()
        session.active = null
        _state.update { it.copy(room = null, myPlayerId = null, error = message) }
    }

    /* ------------------------------------------------ helpers ---------- */

    private fun requireName(): String? {
        val name = _state.value.name.trim()
        if (name.isEmpty()) _state.update { it.copy(error = "Pick a name so the others know who you are.") }
        return name.ifEmpty { null }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            block()
            _state.update { it.copy(busy = false) }
        }
    }

    private fun showError(e: Throwable) {
        _state.update { it.copy(error = e.message ?: "Something went wrong.", busy = false) }
    }

    private fun Throwable.isRoomGone(): Boolean =
        this is FaceOffError && (httpStatus == HTTP_NOT_FOUND || httpStatus == HTTP_FORBIDDEN)

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_NAME = 20
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
    }
}
