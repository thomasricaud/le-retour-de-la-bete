package fr.leretourdelabete.domain

import fr.leretourdelabete.model.GameSession
import fr.leretourdelabete.model.SetupOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VioletStoneRulesTest {
    @Test
    fun `initial count is one less than the player count`() {
        assertEquals(3, VioletStoneRules.initialCount(4))
        assertEquals(5, VioletStoneRules.initialCount(6))
        assertEquals(19, VioletStoneRules.initialCount(20))
    }

    @Test
    fun `one stone is consumed after each night without becoming negative`() {
        assertEquals(4, VioletStoneRules.afterNight(5))
        assertEquals(0, VioletStoneRules.afterNight(0))
    }

    @Test
    fun `healing a werewolf returns one stone without exceeding the initial stock`() {
        assertEquals(5, VioletStoneRules.afterWerewolfHealing(4, playerCount = 6))
        assertEquals(5, VioletStoneRules.afterWerewolfHealing(5, playerCount = 6))
    }

    @Test
    fun `new games default to day ambience and the correct violet stock`() {
        assertTrue(SetupOptions().dayAmbienceEnabled)
        assertEquals(8, GameSession(playerCount = 9).violetStonesInWolfBox)
    }
}
