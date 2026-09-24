package com.espinosa.shinydex.data.repo

import com.espinosa.shinydex.data.local.PokedexDao
import com.espinosa.shinydex.data.local.PokedexEntity
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.PokemonDetail
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.data.remote.PokeApiService
import com.espinosa.shinydex.util.DexEntries
import com.espinosa.shinydex.util.RawDexEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Pokedex data, scoped to one generation at a time.
 *
 * One PokeAPI list call per generation fills a Room cache, and the cache is what the UI
 * observes. That keeps the Pokedex usable offline after the first load and keeps ShinyDex
 * to a single request per generation instead of one per species.
 */
class PokedexRepository(
    private val dao: PokedexDao,
    private val api: PokeApiService,
) {

    /** Only the species introduced in [generation]. */
    fun observePokedex(generation: Generation): Flow<List<PokemonSummary>> =
        dao.observeRange(generation.dexRange.first, generation.dexRange.last)
            .map { rows -> rows.map { PokemonSummary(id = it.id, name = it.name) } }

    suspend fun isCached(generation: Generation): Boolean =
        dao.countInRange(generation.dexRange.first, generation.dexRange.last) >=
            generation.speciesCount

    /** Fetches the names for [generation]'s own dex range and caches them. */
    suspend fun refresh(generation: Generation): Result<Unit> = runCatching {
        val response = api.getPokemonList(
            limit = generation.apiLimit,
            offset = generation.apiOffset,
        )
        val entries = response.results
            .map { PokedexEntity(id = it.id, name = it.name) }
            .filter { it.id in generation.dexRange }
        dao.insertAll(entries)
    }

    /**
     * Stats and the Pokedex entry, fetched in parallel. The entry is a bonus: if its call
     * fails the stats still show, with the entry left empty.
     */
    suspend fun detail(id: Int): Result<PokemonDetail> = runCatching {
        coroutineScope {
            val species = async { runCatching { api.getSpecies(id) }.getOrNull() }
            val dto = api.getPokemon(id)
            val entries = species.await()?.flavorTextEntries.orEmpty().map {
                RawDexEntry(text = it.flavorText, language = it.language.name, version = it.version.name)
            }
            PokemonDetail(
                id = dto.id,
                types = dto.types.sortedBy { it.slot }.map { it.type.name },
                heightMetres = dto.height / DECIMETRES_PER_METRE,
                weightKilograms = dto.weight / HECTOGRAMS_PER_KILOGRAM,
                stats = dto.stats.map { it.stat.name to it.baseStat },
                dexEntry = DexEntries.pick(entries, Generation.ofDexNumber(id)),
            )
        }
    }

    private companion object {
        const val DECIMETRES_PER_METRE = 10.0
        const val HECTOGRAMS_PER_KILOGRAM = 10.0
    }
}
