package fr.leretourdelabete.model

enum class GameMode {
    BEGINNER,
    CONFIRMED,
}

enum class DrawMode {
    APPLICATION,
    PHYSICAL_CARDS,
}

enum class NightColor {
    YELLOW,
    GREEN,
}

enum class GameScreen {
    HOME,
    SETUP,
    INTRO,
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
    val mode: GameMode = GameMode.BEGINNER,
    val drawMode: DrawMode = DrawMode.APPLICATION,
    val dayDurationMinutes: Int = 5,
)

data class GameSession(
    val screen: GameScreen = GameScreen.HOME,
    val mode: GameMode = GameMode.BEGINNER,
    val drawMode: DrawMode = DrawMode.APPLICATION,
    val playerCount: Int = 6,
    val packCallVillagerLimit: Int = 2,
    val dayDurationMinutes: Int = 5,
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
            GameScreen.NIGHT,
            GameScreen.DAY,
            GameScreen.DRAW,
        )
}
