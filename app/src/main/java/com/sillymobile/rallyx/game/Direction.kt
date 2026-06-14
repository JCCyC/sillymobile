package com.sillymobile.rallyx.game

/** Cardinal directions used for grid-based movement, mirroring the original's 4-way joystick. */
enum class Direction(val dc: Int, val dr: Int) {
    NONE(0, 0),
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
        NONE -> NONE
    }
}
