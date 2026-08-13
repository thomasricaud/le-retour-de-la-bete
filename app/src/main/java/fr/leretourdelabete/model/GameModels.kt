package fr.leretourdelabete.model

import fr.leretourdelabete.domain.VioletStoneRules

enum class GameMode {
    BEGINNER,
    CONFIRMED,
}

enum class DrawMode {
    APPLICATION,
    PHYSICAL_CARDS,
}

enum class AiVoice {
    MALE,
    FEMALE,
}

enum class NightColor {
    YELLOW,
    GREEN,
}

enum class GameScreen {
    HOME,
    BLUETOOTH_SETUP,
    SETUP,
    INTRO,
    NIGHT_READY,
    NIGHT,
    DAY,
    DRAW,
    HELP,
    END,
}

enum class DayStage {
    DISCUSSION,
    COUNCIL,
    HEALING,
    HEALING_EFFECT,
}

enum class HealingOutcome {
    VILLAGER,
    GHOUL,
    WEREWOLF,
}

enum class EndReason {
    BLOOD_WOLF_FOUND,
    PACK_CALL_SUCCESS,
    PACK_CALL_FAILURE,
    ABANDONED,
}

data class SetupOptions(
    val playerCount: Int = 6,
    val mode: GameMode = GameMode.CONFIRMED,
    val drawMode: DrawMode = DrawMode.APPLICATION,
    val dayDurationMinutes: Int = 5,
    val dayAmbienceEnabled: Boolean = true,
    val aiVoice: AiVoice = AiVoice.MALE,
)

data class GameSession(
    val screen: GameScreen = GameScreen.HOME,
    val mode: GameMode = GameMode.CONFIRMED,
    val drawMode: DrawMode = DrawMode.APPLICATION,
    val playerCount: Int = 6,
    val violetStonesInWolfBox: Int = VioletStoneRules.initialCount(playerCount),
    val packCallVillagerLimit: Int = 2,
    val dayDurationMinutes: Int = 5,
    val dayAmbienceEnabled: Boolean = false,
    val aiVoice: AiVoice = AiVoice.MALE,
    val round: Int = 1,
    val currentNightColor: NightColor? = null,
    val nextNightColor: NightColor? = null,
    val remainingNightDeck: List<NightColor> = emptyList(),
    val cueIndex: Int = 0,
    val cueRemainingMillis: Long = 0L,
    val dayStage: DayStage = DayStage.DISCUSSION,
    val healingOutcome: HealingOutcome? = null,
    val dayRemainingMillis: Long = 5 * 60_000L,
    val endReason: EndReason? = null,
) {
    val isResumable: Boolean
        get() = screen in setOf(
            GameScreen.INTRO,
            GameScreen.NIGHT_READY,
            GameScreen.NIGHT,
            GameScreen.DAY,
            GameScreen.DRAW,
        )
}
