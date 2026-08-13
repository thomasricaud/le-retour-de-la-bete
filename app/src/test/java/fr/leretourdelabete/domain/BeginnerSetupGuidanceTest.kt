package fr.leretourdelabete.domain

import fr.leretourdelabete.model.AiVoice
import fr.leretourdelabete.model.GameMode
import fr.leretourdelabete.model.SetupOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeginnerSetupGuidanceTest {
    @Test
    fun `confirmed guidance is selected by default`() {
        assertEquals(GameMode.CONFIRMED, SetupOptions().mode)
    }

    @Test
    fun `setup guidance defines eleven ordered steps and both voices`() {
        val steps = BeginnerSetupGuidance.steps

        assertEquals((1..11).toList(), steps.map { it.number })
        assertEquals(
            "guidage_homme_debutant1",
            steps.first().audioResource(AiVoice.MALE),
        )
        assertEquals(
            "guidage_femme_debutant11",
            steps.last().audioResource(AiVoice.FEMALE),
        )
        assertTrue(steps.first().showCancel)
        assertFalse(steps.first().showPlaybackControls)
        assertFalse(steps.last().showPlaybackControls)
    }

    @Test
    fun `preview permissions follow setup instructions`() {
        val steps = BeginnerSetupGuidance.steps

        assertEquals(
            SetupPreviewPermission.PLAYER_COUNT_AND_DRAW_MODE,
            steps[1].previewPermission,
        )
        assertEquals(SetupPreviewPermission.PLAYER_COUNT, steps[3].previewPermission)
        assertTrue(steps[4].canPreview)
        assertTrue(steps[6].canPreview)
        assertEquals(SetupPreviewPermission.NONE, steps[4].previewPermission)
        assertEquals(SetupPreviewPermission.NONE, steps[6].previewPermission)
    }
}
