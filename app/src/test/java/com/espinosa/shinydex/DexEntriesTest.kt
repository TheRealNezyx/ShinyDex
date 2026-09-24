package com.espinosa.shinydex

import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.util.DexEntries
import com.espinosa.shinydex.util.RawDexEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DexEntriesTest {

    private fun entry(version: String, text: String = "Text from $version.", language: String = "en") =
        RawDexEntry(text = text, language = language, version = version)

    @Test
    fun `the sprite game's entry wins`() {
        val picked = DexEntries.pick(
            listOf(entry("gold"), entry("crystal"), entry("silver")),
            Generation.GEN_II,
        )
        assertEquals(GameVersion.CRYSTAL, picked?.game)
        assertEquals("Text from crystal.", picked?.text)
    }

    @Test
    fun `falls back to another game of the same generation`() {
        val picked = DexEntries.pick(listOf(entry("ruby"), entry("sapphire")), Generation.GEN_III)
        assertEquals(GameVersion.SAPPHIRE, picked?.game)
    }

    @Test
    fun `entries from other generations are never used`() {
        val picked = DexEntries.pick(listOf(entry("emerald"), entry("x")), Generation.GEN_IV)
        assertNull(picked)
    }

    @Test
    fun `only English entries are used`() {
        val picked = DexEntries.pick(
            listOf(entry("platinum", "Texto en español.", "es"), entry("pearl")),
            Generation.GEN_IV,
        )
        assertEquals(GameVersion.PEARL, picked?.game)
    }

    @Test
    fun `game text is cleaned into one paragraph`() {
        val raw = "It lives in\nforests. Its POKéMON\u000Cbody is warm-­\nblooded."
        assertEquals("It lives in forests. Its Pokémon body is warm-blooded.", DexEntries.clean(raw))
    }

    @Test
    fun `a blank entry counts as missing`() {
        val picked = DexEntries.pick(listOf(entry("crystal", " \n "), entry("gold")), Generation.GEN_II)
        assertEquals(GameVersion.GOLD, picked?.game)
    }
}
