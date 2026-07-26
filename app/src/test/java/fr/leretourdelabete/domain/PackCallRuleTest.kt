package fr.leretourdelabete.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PackCallRuleTest {
    @Test
    fun `the villager limit follows the rounded quarter thresholds`() {
        val expectedLimits = mapOf(
            4 to 1,
            5 to 1,
            6 to 2,
            7 to 2,
            8 to 2,
            9 to 2,
            10 to 3,
            11 to 3,
            12 to 3,
            13 to 3,
            14 to 4,
            17 to 4,
            18 to 5,
            20 to 5,
        )

        expectedLimits.forEach { (playerCount, expectedLimit) ->
            assertEquals(
                "Seuil incorrect pour $playerCount joueurs",
                expectedLimit,
                PackCallRule.maxRemainingVillagers(playerCount),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the villager limit rejects a non positive player count`() {
        PackCallRule.maxRemainingVillagers(0)
    }
}
