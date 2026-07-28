package fr.leretourdelabete.domain

import fr.leretourdelabete.model.HealingOutcome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealingEffectRulesTest {
    @Test
    fun `ghoul cannot be selected during first round`() {
        assertFalse(HealingEffectRules.isGhoulOptionAvailable(round = 1))
        assertTrue(HealingEffectRules.isGhoulOptionAvailable(round = 2))
    }

    @Test
    fun `each healing result gives the requested physical instructions`() {
        val villager = HealingEffectRules.text(HealingOutcome.VILLAGER)
        val ghoul = HealingEffectRules.text(HealingOutcome.GHOUL)
        val werewolf = HealingEffectRules.text(HealingOutcome.WEREWOLF)

        assertTrue("garde sa pierre bleue" in villager)
        assertTrue("pierre jaune ou verte dans le sac de guérison" in ghoul)
        assertTrue("pierre violette dans la boîte des loups" in werewolf)
        assertTrue("loup-garou" in werewolf)
    }
}
