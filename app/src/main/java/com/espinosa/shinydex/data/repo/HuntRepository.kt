package com.espinosa.shinydex.data.repo

import com.espinosa.shinydex.data.local.HuntDao
import com.espinosa.shinydex.data.local.HuntEntity
import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.Hunt
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.data.model.toHunt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes hunts. All storage is on-device; there is no hunt sync. */
class HuntRepository(private val dao: HuntDao) {

    fun observeHunts(): Flow<List<Hunt>> =
        dao.observeAll().map { rows -> rows.map { it.toHunt() } }

    fun observeHunt(id: Long): Flow<Hunt?> =
        dao.observeById(id).map { it?.toHunt() }

    suspend fun addHunt(
        pokemon: PokemonSummary,
        generation: Generation,
        game: GameVersion,
        method: HuntMethod,
        startingCount: Int = 0,
    ): Long = dao.insert(
        HuntEntity(
            pokemonId = pokemon.id,
            pokemonName = pokemon.name,
            generation = generation.name,
            game = game.name,
            method = method.name,
            encounters = startingCount.coerceAtLeast(0),
        ),
    )

    /**
     * Applies [delta] to the hunt's counter and returns the new total, or null if the hunt
     * no longer exists. The counter is clamped at zero -- you cannot un-encounter a Pokemon.
     */
    suspend fun adjustEncounters(id: Long, delta: Int): Int? {
        val current = dao.findById(id) ?: return null
        val updated = (current.encounters + delta).coerceAtLeast(0)
        dao.setEncounters(id, updated, System.currentTimeMillis())
        return updated
    }

    suspend fun setFound(id: Long, found: Boolean) {
        val current = dao.findById(id) ?: return
        dao.update(current.copy(isFound = found, updatedAt = System.currentTimeMillis()))
    }

    suspend fun resetCounter(id: Long) {
        dao.setEncounters(id, 0, System.currentTimeMillis())
    }

    suspend fun delete(id: Long) {
        dao.findById(id)?.let { dao.delete(it) }
    }
}
