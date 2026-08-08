package fr.leretourdelabete.domain

object VioletStoneRules {
    fun initialCount(playerCount: Int): Int = (playerCount - 1).coerceAtLeast(0)

    fun afterNight(currentCount: Int): Int = (currentCount - 1).coerceAtLeast(0)

    fun afterWerewolfHealing(currentCount: Int, playerCount: Int): Int =
        (currentCount + 1).coerceAtMost(initialCount(playerCount))
}
