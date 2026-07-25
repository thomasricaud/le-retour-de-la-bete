package fr.leretourdelabete.domain

import fr.leretourdelabete.model.NightColor
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightDeckTest {
    @Test
    fun `a fresh deck contains four nights of each color`() {
        val deck = NightDeck.shuffled(Random(42))

        assertEquals(8, deck.size)
        assertEquals(4, deck.count { it == NightColor.YELLOW })
        assertEquals(4, deck.count { it == NightColor.GREEN })
    }

    @Test
    fun `automatic draws remove exactly one card`() {
        var remaining = NightDeck.shuffled(Random(7))
        val drawn = mutableListOf<NightColor>()

        repeat(8) {
            val (color, nextDeck) = NightDeck.draw(remaining)
            drawn += requireNotNull(color)
            assertEquals(remaining.size - 1, nextDeck.size)
            remaining = nextDeck
        }

        val (lastColor, emptyDeck) = NightDeck.draw(remaining)
        assertNull(lastColor)
        assertEquals(emptyList<NightColor>(), emptyDeck)
        assertEquals(4, drawn.count { it == NightColor.YELLOW })
        assertEquals(4, drawn.count { it == NightColor.GREEN })
    }

    @Test
    fun `a physical draw only removes an available matching card`() {
        val deck = listOf(
            NightColor.YELLOW,
            NightColor.GREEN,
            NightColor.YELLOW,
        )

        assertEquals(
            listOf(NightColor.GREEN, NightColor.YELLOW),
            NightDeck.removePhysicalDraw(deck, NightColor.YELLOW),
        )
        val withoutGreen = NightDeck.removePhysicalDraw(deck, NightColor.GREEN)
        assertEquals(
            withoutGreen,
            NightDeck.removePhysicalDraw(withoutGreen, NightColor.GREEN),
        )
    }
}
