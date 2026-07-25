package fr.leretourdelabete.domain

import fr.leretourdelabete.model.NightColor
import kotlin.random.Random

object NightDeck {
    fun shuffled(random: Random = Random.Default): List<NightColor> =
        buildList {
            repeat(4) { add(NightColor.YELLOW) }
            repeat(4) { add(NightColor.GREEN) }
        }.shuffled(random)

    fun draw(deck: List<NightColor>): Pair<NightColor?, List<NightColor>> =
        if (deck.isEmpty()) {
            null to emptyList()
        } else {
            deck.first() to deck.drop(1)
        }

    fun removePhysicalDraw(
        deck: List<NightColor>,
        color: NightColor,
    ): List<NightColor> {
        val mutable = deck.toMutableList()
        mutable.remove(color)
        return mutable
    }
}
