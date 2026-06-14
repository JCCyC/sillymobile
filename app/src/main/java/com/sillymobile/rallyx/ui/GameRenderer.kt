package com.sillymobile.rallyx.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sillymobile.rallyx.game.Car
import com.sillymobile.rallyx.game.Direction
import com.sillymobile.rallyx.game.GameConstants
import com.sillymobile.rallyx.game.GameEngine
import com.sillymobile.rallyx.game.Maze
import com.sillymobile.rallyx.game.SmokeCloud
import kotlin.math.abs
import kotlin.math.sin

private val ROAD_COLOR = Color(0xFF2E4A66)
private val ROAD_EDGE_COLOR = Color(0xFF3D6286)
private val PLAYER_COLOR = Color(0xFFFFD500)
private val ENEMY_COLOR = Color(0xFFE53935)
private val STUNNED_COLOR = Color(0xFF66E0FF)
private val FLAG_COLOR = Color(0xFFEFEFEF)
private val SPECIAL_FLAG_COLOR = Color(0xFFFFC107)
private val SMOKE_COLOR = Color(0xFFB0B0B0)

private const val ROAD_SIZE = GameConstants.CELL_SIZE * 0.55f
private const val CAR_WIDTH = GameConstants.CELL_SIZE * 0.42f
private const val CAR_LENGTH = GameConstants.CELL_SIZE * 0.52f

/** Draws the whole playfield (maze, smoke, flags, enemy and player cars) scaled to fit the canvas. */
fun DrawScope.drawGame(engine: GameEngine, scale: Float, offsetX: Float, offsetY: Float) {
    withTransform({
        translate(offsetX, offsetY)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawMaze(engine.maze)
        drawSmoke(engine.smokeClouds)
        drawFlags(engine)
        for (enemy in engine.enemies) drawCar(enemy, ENEMY_COLOR, engine.gameTime, stunned = enemy.isStunned)
        drawCar(engine.player, PLAYER_COLOR, engine.gameTime, stunned = false)
    }
}

private fun DrawScope.drawMaze(maze: Maze) {
    val cell = GameConstants.CELL_SIZE
    for (r in 0 until maze.rows) {
        for (c in 0 until maze.cols) {
            val (cx, cy) = maze.cellCenter(c, r)
            // Road node at this cell.
            drawRect(
                color = ROAD_COLOR,
                topLeft = Offset(cx - ROAD_SIZE / 2f, cy - ROAD_SIZE / 2f),
                size = Size(ROAD_SIZE, ROAD_SIZE),
            )
            // Connector to the right neighbour.
            if (maze.isOpen(c, r, Direction.RIGHT)) {
                drawRect(
                    color = ROAD_EDGE_COLOR,
                    topLeft = Offset(cx - ROAD_SIZE / 2f, cy - ROAD_SIZE / 2f),
                    size = Size(cell, ROAD_SIZE),
                )
            }
            // Connector to the cell below.
            if (maze.isOpen(c, r, Direction.DOWN)) {
                drawRect(
                    color = ROAD_EDGE_COLOR,
                    topLeft = Offset(cx - ROAD_SIZE / 2f, cy - ROAD_SIZE / 2f),
                    size = Size(ROAD_SIZE, cell),
                )
            }
        }
    }
}

private fun DrawScope.drawSmoke(clouds: List<SmokeCloud>) {
    for (cloud in clouds) {
        val (cx, cy) = Pair(
            cloud.col * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
            cloud.row * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
        )
        val alpha = (cloud.timeLeft / GameConstants.SMOKE_DURATION_SEC).coerceIn(0f, 1f)
        drawCircle(
            color = SMOKE_COLOR.copy(alpha = alpha * 0.75f),
            radius = GameConstants.CELL_SIZE * 0.45f,
            center = Offset(cx, cy),
        )
    }
}

private fun DrawScope.drawFlags(engine: GameEngine) {
    for (flag in engine.flags) {
        if (flag.collected) continue
        val (cx, cy) = engine.maze.cellCenter(flag.col, flag.row)
        val color = if (flag.special) {
            val blink = (sin(engine.gameTime * 8f) + 1f) / 2f
            SPECIAL_FLAG_COLOR.copy(alpha = 0.5f + blink * 0.5f)
        } else {
            FLAG_COLOR
        }
        drawFlagIcon(cx, cy, color)
    }
}

private fun DrawScope.drawFlagIcon(cx: Float, cy: Float, color: Color) {
    val poleHeight = GameConstants.CELL_SIZE * 0.34f
    val flagWidth = GameConstants.CELL_SIZE * 0.22f
    // Pole
    drawRect(
        color = Color(0xFFCCCCCC),
        topLeft = Offset(cx - 1.5f, cy - poleHeight / 2f),
        size = Size(3f, poleHeight),
    )
    // Pennant
    drawRect(
        color = color,
        topLeft = Offset(cx + 1.5f, cy - poleHeight / 2f),
        size = Size(flagWidth, poleHeight * 0.5f),
    )
}

/** Draws a vehicle as a rotated rectangle with a small windshield, facing [car]'s direction. */
private fun DrawScope.drawCar(car: Car, color: Color, gameTime: Float, stunned: Boolean) {
    val (x, y) = car.position()
    val angle = directionAngle(car.facing)
    val drawColor = if (stunned) {
        val blink = abs(sin(gameTime * 12f))
        if (blink > 0.5f) STUNNED_COLOR else color
    } else {
        color
    }
    val spinAngle = if (stunned) gameTime * 540f else 0f

    withTransform({
        translate(x, y)
        rotate(angle + spinAngle, pivot = Offset.Zero)
    }) {
        drawRect(
            color = drawColor,
            topLeft = Offset(-CAR_WIDTH / 2f, -CAR_LENGTH / 2f),
            size = Size(CAR_WIDTH, CAR_LENGTH),
        )
        // Windshield indicates the front of the car.
        drawRect(
            color = Color(0xFF222222),
            topLeft = Offset(-CAR_WIDTH / 2f + 3f, -CAR_LENGTH / 2f + 3f),
            size = Size(CAR_WIDTH - 6f, CAR_LENGTH * 0.35f),
        )
    }
}

private fun directionAngle(direction: Direction): Float = when (direction) {
    Direction.UP -> 0f
    Direction.RIGHT -> 90f
    Direction.DOWN -> 180f
    Direction.LEFT -> 270f
    Direction.NONE -> 0f
}
