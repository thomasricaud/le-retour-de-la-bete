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
            packCallVillagerLimit = 2,
        )
        val green = GameSequenceFactory.night(
            round = 2,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            packCallVillagerLimit = 2,
        )

        assertEquals(ConfirmedLaterNightTimeline.CUE_ID, yellow.single().id)
        assertEquals(ConfirmedLaterNightTimeline.CUE_ID, green.single().id)
        assertTrue(
            "goules jaunes" in ConfirmedLaterNightTimeline.presentation(
                193_000L,
                NightColor.YELLOW,
                2,
            ).text,
        )
        assertTrue(
            "goules vertes" in ConfirmedLaterNightTimeline.presentation(
                193_000L,
                NightColor.GREEN,
                2,
            ).text,
        )
    }

    @Test
    fun `beginner mode gives more spoken guidance than confirmed mode`() {
        val beginner = GameSequenceFactory.night(
            round = 3,
            color = NightColor.GREEN,
            mode = GameMode.BEGINNER,
            packCallVillagerLimit = 2,
        )
        val confirmed = GameSequenceFactory.night(
            round = 3,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
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
    fun `beginner departure remains 45 seconds`() {
        val beginnerFirstNight = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.BEGINNER,
            packCallVillagerLimit = 2,
        )
        val beginnerLaterNight = GameSequenceFactory.night(
            round = 2,
            color = NightColor.YELLOW,
            mode = GameMode.BEGINNER,
            packCallVillagerLimit = 2,
        )

        listOf(beginnerFirstNight, beginnerLaterNight).forEach { cues ->
            val departure = cues.first { it.id == "night_departure_timer" }
            assertEquals(45_000L, departure.fallbackDurationMillis)
            assertEquals("commun_012_ambiance_nuit_boucle", departure.audioResource)
            assertTrue("45 secondes" in departure.text)
        }
    }

    @Test
    fun `confirmed first night is one continuous 152 second cue`() {
        val cues = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            packCallVillagerLimit = 2,
        )

        assertEquals(listOf(ConfirmedFirstNightTimeline.CUE_ID), cues.map { it.id })
        val cue = cues.single()
        assertEquals(152_000L, cue.fallbackDurationMillis)
        assertEquals("premiere_nuit", cue.audioResource)
        assertEquals(CueKind.TIMER, cue.kind)
        assertFalse(cue.loopAudio)
        assertTrue(cue.replayable)
    }

    @Test
    fun `confirmed first night presentation follows all six boundaries`() {
        val samples = listOf(
            152_000L to "Regagnez vos habitations",
            122_000L to "Restez calme, préparez-vous à dormir",
            107_000L to "Fermez les yeux",
            93_000L to "Le loup-garou de sang se réveille",
            30_000L to "Le jour va bientôt se lever",
            5_000L to "Réveillez-vous",
        )

        samples.forEach { (remaining, title) ->
            assertEquals(title, ConfirmedFirstNightTimeline.presentation(remaining).title)
        }
        assertTrue(
            "30 secondes" in ConfirmedFirstNightTimeline.presentation(152_000L).text,
        )
        assertTrue(
            "15 secondes" in ConfirmedFirstNightTimeline.presentation(122_000L).text,
        )
        assertTrue(
            "25 secondes" in ConfirmedFirstNightTimeline.presentation(30_000L).text,
        )
    }

    @Test
    fun `confirmed later nights are one continuous 258 second cue`() {
        val cue = GameSequenceFactory.night(
            round = 2,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            packCallVillagerLimit = 2,
        ).single()

        assertEquals(ConfirmedLaterNightTimeline.CUE_ID, cue.id)
        assertEquals(258_000L, cue.fallbackDurationMillis)
        assertEquals("nuit", cue.audioResource)
        assertEquals(CueKind.TIMER, cue.kind)
        assertFalse(cue.loopAudio)
        assertTrue(cue.replayable)
    }

    @Test
    fun `confirmed later night presentation follows seven boundaries`() {
        val samples = listOf(
            258_000L to "Regagnez vos habitations",
            228_000L to "Restez calme, préparez-vous à dormir",
            213_000L to "Fermez les yeux",
            193_000L to "Le loup-garou de sang se réveille et appelle sa meute, ses adorateurs",
            170_000L to "Conseil des loups",
            30_000L to "Le jour va bientôt se lever",
            5_000L to "Réveillez-vous",
        )

        samples.forEach { (remaining, title) ->
            assertEquals(
                title,
                ConfirmedLaterNightTimeline.presentation(
                    remaining,
                    NightColor.YELLOW,
                    3,
                ).title,
            )
        }
        val council = ConfirmedLaterNightTimeline.presentation(
            170_000L,
            NightColor.YELLOW,
            3,
        ).text
        assertTrue("3 villageois ou moins" in council)
        assertTrue("Retournez tous dormir." in council)
    }

    @Test
    fun `confirmed later night advance targets select supplied replacement tracks`() {
        val targets = listOf(
            258_000L to 228_000L,
            228_000L to 213_000L,
            213_000L to 170_000L,
            193_000L to 170_000L,
            170_000L to 30_000L,
            30_000L to 5_000L,
        )
        targets.forEach { (remaining, target) ->
            assertEquals(target, ConfirmedLaterNightTimeline.advanceTarget(remaining))
        }
        assertEquals(null, ConfirmedLaterNightTimeline.advanceTarget(5_000L))

        assertEquals(
            "nuit_avance_gong",
            ConfirmedLaterNightTimeline.playback(228_000L).audioResource,
        )
        assertEquals(
            "nuit_avance_fermez_yeux",
            ConfirmedLaterNightTimeline.playback(213_000L).audioResource,
        )
        assertEquals(
            "nuit_avance_conseil_loups",
            ConfirmedLaterNightTimeline.playback(170_000L).audioResource,
        )
        assertEquals(
            "nuit_avance_fin_nuit",
            ConfirmedLaterNightTimeline.playback(30_000L).audioResource,
        )
        assertEquals(
            "nuit_avance_cocorico",
            ConfirmedLaterNightTimeline.playback(5_000L).audioResource,
        )
        assertTrue(ConfirmedLaterNightTimeline.canReplay(258_000L))
        assertFalse(ConfirmedLaterNightTimeline.canReplay(228_000L))
    }

    @Test
    fun `pack call is available only during later night sequences three to five`() {
        listOf(213_000L, 193_000L, 170_000L, 31_000L).forEach { remaining ->
            assertTrue(ConfirmedLaterNightTimeline.canCall(remaining))
        }
        listOf(258_000L, 228_000L, 30_000L, 5_000L).forEach { remaining ->
            assertFalse(ConfirmedLaterNightTimeline.canCall(remaining))
        }
    }

    @Test
    fun `confirmed first night advance targets select supplied replacement tracks`() {
        val targets = listOf(
            152_000L to 122_000L,
            122_000L to 107_000L,
            107_000L to 30_000L,
            93_000L to 30_000L,
            30_000L to 5_000L,
        )
        targets.forEach { (remaining, target) ->
            assertEquals(target, ConfirmedFirstNightTimeline.advanceTarget(remaining))
        }
        assertEquals(null, ConfirmedFirstNightTimeline.advanceTarget(5_000L))

        assertEquals(
            "premiere_nuit_avance_gong",
            ConfirmedFirstNightTimeline.playback(122_000L).audioResource,
        )
        assertEquals(
            "premiere_nuit_avance_fermez_yeux",
            ConfirmedFirstNightTimeline.playback(107_000L).audioResource,
        )
        assertEquals(
            "nuit_avance_fin_nuit",
            ConfirmedFirstNightTimeline.playback(30_000L).audioResource,
        )
        assertEquals(
            "nuit_avance_cocorico",
            ConfirmedFirstNightTimeline.playback(5_000L).audioResource,
        )
        assertTrue(ConfirmedFirstNightTimeline.canReplay(152_000L))
        assertFalse(ConfirmedFirstNightTimeline.canReplay(122_000L))
    }

    @Test
    fun `legacy first night steps are absent from confirmed flow`() {
        val cues = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            packCallVillagerLimit = 2,
        )

        listOf(
            "night_departure_timer",
            "night_council_timer",
            "night_second_beeps",
            "night_sleep_again",
            "night_rooster",
            "night_village_wake",
        ).forEach { removedId ->
            assertFalse(cues.any { it.id == removedId })
        }
    }

    @Test
    fun `confirmed first night no longer references retired resources`() {
        val cue = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.CONFIRMED,
            packCallVillagerLimit = 2,
        ).single()

        listOf(
            "commun_001_nuit_depart_30",
            "commun_001_nuit_depart_45",
            "confirm_premiere_nuit",
        ).forEach { retired ->
            assertFalse(cue.audioResource == retired)
        }
    }

    @Test
    fun `first council screen remains available in beginner mode`() {
        val cue = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.BEGINNER,
            packCallVillagerLimit = 2,
        ).first { it.id == "night_council_timer" }

        assertEquals("Première nuit", cue.title)
        assertEquals("commun_012_ambiance_nuit_boucle", cue.audioResource)
        assertTrue(cue.loopAudio)
        assertTrue(cue.replayable)
    }

    @Test
    fun `intro synopsis uses the shortened requested text`() {
        val synopsis = GameSequenceFactory.intro(GameMode.CONFIRMED).single()

        assertEquals(
            "Villageois, restez unis : reconnaissez le loup-garou de sang " +
                "avant qu'il ne s'empare de toutes nos âmes.",
            synopsis.text,
        )
    }

    @Test
    fun `confirmed later night no longer uses legacy split cues`() {
        val cue = GameSequenceFactory.night(
            round = 2,
            color = NightColor.GREEN,
            mode = GameMode.CONFIRMED,
            packCallVillagerLimit = 2,
        ).single()

        assertEquals(ConfirmedLaterNightTimeline.CUE_ID, cue.id)
        assertEquals("nuit", cue.audioResource)
        assertFalse(cue.loopAudio)
        assertTrue(cue.replayable)
    }

    @Test
    fun `beginner first council keeps ambience and replay`() {
        val cue = GameSequenceFactory.night(
            round = 1,
            color = null,
            mode = GameMode.BEGINNER,
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
            packCallVillagerLimit = 2,
        )
    }

    @Test
    fun `beginner pack call cue uses the configured player threshold`() {
        val cues = GameSequenceFactory.night(
            round = 2,
            color = NightColor.YELLOW,
            mode = GameMode.BEGINNER,
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
        assertTrue("Plus de 4 villageois" in failureCue.text)
        assertEquals("aides_443_fin_b2_seuil_4", failureCue.audioResource)
    }
}
