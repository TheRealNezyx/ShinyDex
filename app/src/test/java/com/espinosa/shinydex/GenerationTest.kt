package com.espinosa.shinydex

import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.PokemonSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationTest {

    @Test
    fun `Gen I stays locked because it has no shiny mechanic`() {
        assertFalse(Generation.GEN_I.unlocked)
    }

    @Test
    fun `Gen II, III and IV are playable`() {
        assertEquals(
            listOf(Generation.GEN_II, Generation.GEN_III, Generation.GEN_IV),
            Generation.unlockedEntries,
        )
    }

    @Test
    fun `dex ranges are contiguous and never overlap`() {
        val ordered = Generation.entries.sortedBy { it.dexRange.first }
        ordered.zipWithNext { previous, next ->
            assertEquals(
                "${next.name} does not start where ${previous.name} ends",
                previous.dexRange.last + 1,
                next.dexRange.first,
            )
        }
        assertEquals(1, ordered.first().dexRange.first)
    }

    @Test
    fun `each generation holds the expected number of species`() {
        assertEquals(151, Generation.GEN_I.speciesCount)
        assertEquals(100, Generation.GEN_II.speciesCount)
        assertEquals(135, Generation.GEN_III.speciesCount)
        assertEquals(107, Generation.GEN_IV.speciesCount)
    }

    @Test
    fun `a dex number resolves to the generation that introduced it`() {
        assertEquals(Generation.GEN_I, Generation.ofDexNumber(25))
        assertEquals(Generation.GEN_II, Generation.ofDexNumber(152))
        assertEquals(Generation.GEN_II, Generation.ofDexNumber(251))
        assertEquals(Generation.GEN_III, Generation.ofDexNumber(252))
        assertEquals(Generation.GEN_IV, Generation.ofDexNumber(493))
    }

    @Test
    fun `the API window matches the dex range`() {
        Generation.entries.forEach { generation ->
            assertEquals(generation.dexRange.first - 1, generation.apiOffset)
            assertEquals(generation.speciesCount, generation.apiLimit)
        }
    }

    @Test
    fun `every unlocked generation has games and methods`() {
        Generation.unlockedEntries.forEach { generation ->
            assertTrue("${generation.name} has no games", generation.games.isNotEmpty())
            assertTrue("${generation.name} has no methods", generation.methods.isNotEmpty())
            generation.games.forEach { game ->
                assertTrue(
                    "${game.name} has no method in ${generation.name}",
                    generation.methodsFor(game).isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `every game belongs to exactly one generation`() {
        assertEquals(GameVersion.entries.size, Generation.entries.sumOf { it.games.size })
    }

    @Test
    fun `sprites come from the era the species belongs to`() {
        val chikorita = PokemonSummary(id = 152, name = "chikorita")
        val treecko = PokemonSummary(id = 252, name = "treecko")
        val turtwig = PokemonSummary(id = 387, name = "turtwig")

        assertTrue(chikorita.shinySpriteUrl.contains("generation-ii/crystal"))
        assertTrue(treecko.shinySpriteUrl.contains("generation-iii/emerald"))
        assertTrue(turtwig.shinySpriteUrl.contains("generation-iv/platinum"))

        listOf(chikorita, treecko, turtwig).forEach {
            assertTrue("${it.name} shiny url is not the shiny one", it.shinySpriteUrl.contains("/shiny/"))
            assertFalse("${it.name} normal url points at shiny", it.spriteUrl.contains("/shiny/"))
        }
    }
}
