package com.sillymobile.rallyx.game

/** Tunable knobs for the playfield, vehicles and scoring. */
object GameConstants {
    const val COLS = 13
    const val ROWS = 17
    const val CELL_SIZE = 64f

    const val LIVES_START = 3

    const val FUEL_MAX = 100f
    const val FUEL_DRAIN_PER_SEC = 1.45f
    const val FLAG_FUEL_BONUS = 11f
    const val SPECIAL_FLAG_FUEL_BONUS = 40f

    const val PLAYER_BASE_SPEED = 3.4f // cells per second
    const val PLAYER_SPEED_PER_LEVEL = 0.12f
    const val PLAYER_MAX_SPEED = 5.2f

    const val ENEMY_BASE_SPEED = 2.5f
    const val ENEMY_SPEED_PER_LEVEL = 0.15f
    const val ENEMY_MAX_SPEED = 4.6f

    const val ENEMY_BASE_COUNT = 3
    const val ENEMY_MAX_COUNT = 7

    const val SMOKE_MAX_CHARGES = 5
    const val SMOKE_RECHARGE_SEC = 7f
    const val SMOKE_DURATION_SEC = 2.2f
    const val STUN_DURATION_SEC = 4.5f

    const val FLAG_SCORE = 100
    const val SPECIAL_FLAG_SCORE = 1000
    const val STUNNED_ENEMY_SCORE = 500

    const val FLAGS_PER_MAZE = 12

    const val CHALLENGE_LEVEL_EVERY = 4
    const val CHALLENGE_TIME_SEC = 35f
    const val CHALLENGE_BONUS_PER_FLAG = 250
}
