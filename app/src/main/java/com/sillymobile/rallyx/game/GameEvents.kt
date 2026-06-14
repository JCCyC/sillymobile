package com.sillymobile.rallyx.game

enum class GamePhase {
    READY,
    PLAYING,
    CHALLENGE,
    CRASHED,
    LEVEL_CLEAR,
    GAME_OVER,
}

/** One-shot occurrences emitted during [GameEngine.update], consumed by the UI/audio layer. */
sealed interface GameEvent {
    data class FlagCollected(val special: Boolean) : GameEvent
    data object SmokeReleased : GameEvent
    data object EnemyStunned : GameEvent
    data object EnemyDestroyed : GameEvent
    data object Crashed : GameEvent
    data object LevelComplete : GameEvent
    data object ChallengeStart : GameEvent
    data object ChallengeComplete : GameEvent
    data object GameOver : GameEvent
}
