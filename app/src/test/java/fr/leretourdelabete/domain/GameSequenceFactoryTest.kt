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
            packCallVillagerLimit = 2,
        )

        assertFalse(cues.any { it.id == "night_first_wake" })
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
            packCallVillagerLimit = 2,
        )
        val green = GameSequenceFactory.night(
            round = 2,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
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
            packCallVillagerLimit = 2,
        )
        val confirmed = GameSequenceFactory.night(
            round = 3,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
        )

        assertTrue(beginner.size > confirmed.size)
        assertTrue(beginner.any { it.id == "council_identify" })
        assertFalse(confirmed.any { it.id == "council_identify" })
    }

    @Test
    fun `confirmed introduction omits the ready announcement`() {
        val beginner = GameSequenceFactory.intro(GameMode.BEGINNER)
        val confirmed = GameSequenceFactory.intro(GameMode.CONFIRMED)

        assertTrue(beginner.any { it.id == "intro_ready" })
        assertFalse(confirmed.any { it.id == "intro_ready" })
        assertEquals(listOf("intro_synopsis"), confirmed.map { it.id })
    }

    @Test
    fun `departure setting controls one timer with matching soundtrack`() {
        val short = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 30,
            packCallVillagerLimit = 2,
        )
        val long = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
        )

        assertEquals(
            30_000L,
            short.first { it.id == "night_departure_timer" }.fallbackDurationMillis,
        )
        assertEquals(
            "commun_001_nuit_depart_30",
            short.first { it.id == "night_departure_timer" }.audioResource,
        )
        assertEquals(
            45_000L,
            long.first { it.id == "night_departure_timer" }.fallbackDurationMillis,
        )
        assertEquals(
            "commun_001_nuit_depart_45",
            long.first { it.id == "night_departure_timer" }.audioResource,
        )
        assertTrue(
            short.first { it.id == "night_departure_timer" }.text ==
                "C'est la nuit, regagnez vos habitations.",
        )
        listOf("night_departure", "night_first_beeps", "night_sleep", "night_first_wake")
            .forEach { removedId ->
                assertFalse(short.any { it.id == removedId })
                assertFalse(long.any { it.id == removedId })
            }
    }

    @Test
    fun `first council screen carries the requested first night wording`() {
        val cue = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
        ).first { it.id == "night_council_timer" }

        assertEquals("Première nuit", cue.title)
        assertEquals(
            "Le loup garou de sang se réveille et choisit sa première victime.",
            cue.text,
        )
        assertEquals("confirm_premiere_nuit", cue.audioResource)
        assertFalse(cue.loopAudio)
        assertFalse(cue.replayable)
    }

    @Test
    fun `later councils keep ambience and replay`() {
        val cue = GameSequenceFactory.night(
            round = 2,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
        ).first { it.id == "night_council_timer" }

        assertEquals("Conseil des loups", cue.title)
        assertEquals("commun_012_ambiance_nuit_boucle", cue.audioResource)
        assertTrue(cue.loopAudio)
        assertTrue(cue.replayable)
    }

    @Test
    fun `beginner first council keeps ambience and replay`() {
        val cue = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.BEGINNER,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
        ).first { it.id == "night_council_timer" }

        assertEquals("Première nuit", cue.title)
        assertEquals("commun_012_ambiance_nuit_boucle", cue.audioResource)
        assertTrue(cue.loopAudio)
        assertTrue(cue.replayable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `later night requires a color`() {
        GameSequenceFactory.night(
            round = 2,
            color = null,
            mode = GameMode.CONFIRMED,
            departureSeconds = 45,
            packCallVillagerLimit = 2,
        )
    }

    @Test
    fun `beginner pack call cue uses the configured player threshold`() {
        val cues = GameSequenceFactory.night(
            round = 2,
            color = NightColor.YELLOW,
            mode = GameMode.BEGINNER,
            departureSeconds = 45,
            packCallVillagerLimit = 3,
        )

        val packCallCue = cues.first { it.id == "council_end_option" }
        assertTrue("3 villageois ou moins" in packCallCue.text)
        assertEquals(
            "debutant_111_option_fin_partie_seuil_3",
            packCallCue.audioResource,
        )
    }

    @Test
    fun `help and ending cues speak the stored threshold`() {
        val helpCue = GameSequenceFactory.helpCues(4).first { it.id == "help_end" }
        val successCue = GameSequenceFactory.endCue("pack_success", 4)
        val failureCue = GameSequenceFactory.endCue("pack_failure", 4)

        assertTrue("4 villageois ou moins" in helpCue.text)
        assertEquals("aides_441_appel_de_la_meute_seuil_4", helpCue.audioResource)
        assertTrue("4 villageois ou moins" in successCue.text)
        assertEquals("aides_442_fin_b1_seuil_4", successCue.audioResource)
        assertTrue("plus de 4 villageois" in failureCue.text)
        assertEquals("aides_443_fin_b2_seuil_4", failureCue.audioResource)
    }
}
