package com.sillymobile.rallyx.audio

import com.sillymobile.rallyx.game.GameEvent
import com.sillymobile.rallyx.game.GamePhase

/** Ties the looping background music to stage type and plays one-shot SFX for game events. */
class AudioManager {
    private val music = ChiptuneEngine()
    private val sfx = SfxPlayer()

    fun onPhaseChanged(phase: GamePhase) {
        when (phase) {
            GamePhase.CHALLENGE -> music.play(Compositions.CHALLENGE_THEME)
            GamePhase.READY -> music.stop()
            GamePhase.GAME_OVER -> Unit
            else -> music.play(Compositions.NORMAL_THEME)
        }
    }

    fun handleEvent(event: GameEvent) {
        when (event) {
            is GameEvent.FlagCollected -> if (event.special) sfx.playSpecialFlag() else sfx.playFlagPickup()
            GameEvent.SmokeReleased -> sfx.playSmoke()
            GameEvent.EnemyStunned, GameEvent.EnemyDestroyed -> sfx.playStun()
            GameEvent.Crashed -> sfx.playCrash()
            GameEvent.LevelComplete, GameEvent.ChallengeComplete -> sfx.playLevelComplete()
            GameEvent.ChallengeStart -> Unit
            GameEvent.GameOver -> {
                sfx.playGameOver()
                music.stop()
            }
        }
    }

    fun release() {
        music.stop()
    }
}
