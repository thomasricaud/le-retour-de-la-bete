package fr.leretourdelabete.data

import android.content.Context
import fr.leretourdelabete.domain.PackCallRule
import fr.leretourdelabete.model.DayStage
import fr.leretourdelabete.model.DrawMode
import fr.leretourdelabete.model.EndReason
import fr.leretourdelabete.model.GameMode
import fr.leretourdelabete.model.GameScreen
import fr.leretourdelabete.model.GameSession
import fr.leretourdelabete.model.NightColor

class GameSessionRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(session: GameSession) {
        preferences.edit()
            .putString(KEY_SCREEN, session.screen.name)
            .putString(KEY_MODE, session.mode.name)
            .putString(KEY_DRAW_MODE, session.drawMode.name)
            .putInt(KEY_PLAYERS, session.playerCount)
            .putInt(KEY_PACK_CALL_VILLAGER_LIMIT, session.packCallVillagerLimit)
            .putInt(KEY_DEPARTURE_SECONDS, session.departureSeconds)
            .putInt(KEY_DAY_MINUTES, session.dayDurationMinutes)
            .putInt(KEY_ROUND, session.round)
            .putString(KEY_CURRENT_COLOR, session.currentNightColor?.name)
            .putString(KEY_NEXT_COLOR, session.nextNightColor?.name)
            .putString(KEY_DECK, session.remainingNightDeck.joinToString(",") { it.name })
            .putInt(KEY_CUE_INDEX, session.cueIndex)
            .putLong(KEY_CUE_REMAINING, session.cueRemainingMillis)
            .putString(KEY_DAY_STAGE, session.dayStage.name)
            .putLong(KEY_DAY_REMAINING, session.dayRemainingMillis)
            .putString(KEY_END_REASON, session.endReason?.name)
            .apply()
    }

    fun load(): GameSession? {
        if (!preferences.contains(KEY_SCREEN)) return null
        val playerCount = preferences.getInt(KEY_PLAYERS, 6).coerceIn(4, 20)
        val packCallVillagerLimit = if (preferences.contains(KEY_PACK_CALL_VILLAGER_LIMIT)) {
            preferences.getInt(
                KEY_PACK_CALL_VILLAGER_LIMIT,
                PackCallRule.maxRemainingVillagers(playerCount),
            ).coerceIn(1, playerCount)
        } else {
            PackCallRule.maxRemainingVillagers(playerCount)
        }
        return GameSession(
            screen = enumValueOrDefault(
                preferences.getString(KEY_SCREEN, null),
                GameScreen.HOME,
            ),
            mode = enumValueOrDefault(
                preferences.getString(KEY_MODE, null),
                GameMode.BEGINNER,
            ),
            drawMode = enumValueOrDefault(
                preferences.getString(KEY_DRAW_MODE, null),
                DrawMode.APPLICATION,
            ),
            playerCount = playerCount,
            packCallVillagerLimit = packCallVillagerLimit,
            departureSeconds = preferences.getInt(KEY_DEPARTURE_SECONDS, 45)
                .takeIf { it == 30 || it == 45 } ?: 45,
            dayDurationMinutes = preferences.getInt(KEY_DAY_MINUTES, 5)
                .coerceIn(0, 30),
            round = preferences.getInt(KEY_ROUND, 1).coerceAtLeast(1),
            currentNightColor = enumValueOrNull<NightColor>(
                preferences.getString(KEY_CURRENT_COLOR, null),
            ),
            nextNightColor = enumValueOrNull<NightColor>(
                preferences.getString(KEY_NEXT_COLOR, null),
            ),
            remainingNightDeck = parseDeck(preferences.getString(KEY_DECK, null)),
            cueIndex = preferences.getInt(KEY_CUE_INDEX, 0).coerceAtLeast(0),
            cueRemainingMillis = preferences.getLong(KEY_CUE_REMAINING, 0L)
                .coerceAtLeast(0L),
            dayStage = enumValueOrDefault(
                preferences.getString(KEY_DAY_STAGE, null),
                DayStage.DISCUSSION,
            ),
            dayRemainingMillis = preferences.getLong(KEY_DAY_REMAINING, 5 * 60_000L)
                .coerceAtLeast(0L),
            endReason = enumValueOrNull<EndReason>(
                preferences.getString(KEY_END_REASON, null),
            ),
        )
    }

    fun hasResumableSession(): Boolean = load()?.isResumable == true

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun parseDeck(raw: String?): List<NightColor> =
        raw.orEmpty()
            .split(",")
            .mapNotNull { token ->
                token.takeIf { it.isNotBlank() }?.let {
                    runCatching { NightColor.valueOf(it) }.getOrNull()
                }
            }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String?,
        default: T,
    ): T = raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private inline fun <reified T : Enum<T>> enumValueOrNull(raw: String?): T? =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private companion object {
        const val PREFERENCES = "game_session"
        const val KEY_SCREEN = "screen"
        const val KEY_MODE = "mode"
        const val KEY_DRAW_MODE = "draw_mode"
        const val KEY_PLAYERS = "players"
        const val KEY_PACK_CALL_VILLAGER_LIMIT = "pack_call_villager_limit"
        const val KEY_DEPARTURE_SECONDS = "departure_seconds"
        const val KEY_DAY_MINUTES = "day_minutes"
        const val KEY_ROUND = "round"
        const val KEY_CURRENT_COLOR = "current_color"
        const val KEY_NEXT_COLOR = "next_color"
        const val KEY_DECK = "deck"
        const val KEY_CUE_INDEX = "cue_index"
        const val KEY_CUE_REMAINING = "cue_remaining"
        const val KEY_DAY_STAGE = "day_stage"
        const val KEY_DAY_REMAINING = "day_remaining"
        const val KEY_END_REASON = "end_reason"
    }
}
