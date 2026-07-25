package fr.leretourdelabete.domain

import fr.leretourdelabete.model.GameMode
import fr.leretourdelabete.model.NightColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSequenceFactoryTest {
    @Test
    fun `the first night never wakes colored ghouls`() {
        val cues = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.BEGINNER,
            departureSeconds = 45,
        )

        assertTrue(cues.any { it.id == "night_first_wake" })
        assertFalse(cues.any { it.id.startsWith("wake_pack_") })
        assertFalse(cues.any { "goules jaunes" in it.text.lowercase() })
        assertFalse(cues.any { "goules vertes" in it.text.lowercase() })
    }

    @Test
    fun `later night wording follows the drawn color`() {
        val yellow = GameSequenceFactory.night(
            round = 2,
            color = NightColor.YELLOW,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
        )
        val green = GameSequenceFactory.night(
            round = 2,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
        )

        assertTrue(yellow.any { it.audioResource == "jaune_303_reveil_meute_jaune" })
        assertFalse(yellow.any { it.audioResource == "vert_303_reveil_meute_verte" })
        assertTrue(green.any { it.audioResource == "vert_303_reveil_meute_verte" })
        assertFalse(green.any { it.audioResource == "jaune_303_reveil_meute_jaune" })
    }

    @Test
    fun `beginner mode gives more spoken guidance than confirmed mode`() {
        val beginner = GameSequenceFactory.night(
            round = 3,
            color = NightColor.GREEN,
            mode = GameMode.BEGINNER,
            departureSeconds = 45,
        )
        val confirmed = GameSequenceFactory.night(
            round = 3,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
        )

        assertTrue(beginner.size > confirmed.size)
        assertTrue(beginner.any { it.id == "council_identify" })
        assertFalse(confirmed.any { it.id == "council_identify" })
    }

    @Test
    fun `departure setting controls the timer and matching narration`() {
        val short = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 30,
        )
        val long = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
        )

        assertEquals(
            30_000L,
            short.first { it.id == "night_departure_timer" }.fallbackDurationMillis,
        )
        assertEquals(
            "commun_001_nuit_depart_30",
            short.first { it.id == "night_departure" }.audioResource,
        )
        assertEquals(
            45_000L,
            long.first { it.id == "night_departure_timer" }.fallbackDurationMillis,
        )
        assertEquals(
            "commun_001_nuit_depart_45",
            long.first { it.id == "night_departure" }.audioResource,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `later night requires a color`() {
        GameSequenceFactory.night(
            round = 2,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
        )
    }
}
