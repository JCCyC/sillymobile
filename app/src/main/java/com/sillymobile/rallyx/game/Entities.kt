package com.sillymobile.rallyx.game

/** A vehicle that travels along the maze's road graph, cell-center to cell-center. */
open class Car(var col: Int, var row: Int) {
    var fromCol = col
    var fromRow = row
    var toCol = col
    var toRow = row
    var progress = 0f // 0..1 between "from" and "to" cell centers
    var moveDir = Direction.NONE
    var facing = Direction.UP

    /** World pixel position interpolated between the from/to cells. */
    fun position(): Pair<Float, Float> {
        val (fx, fy) = cellCenterOf(fromCol, fromRow)
        val (tx, ty) = cellCenterOf(toCol, toRow)
        return (fx + (tx - fx) * progress) to (fy + (ty - fy) * progress)
    }

    private fun cellCenterOf(c: Int, r: Int): Pair<Float, Float> {
        val size = GameConstants.CELL_SIZE
        return (c * size + size / 2f) to (r * size + size / 2f)
    }

    fun isAtCellCenter(): Boolean = moveDir == Direction.NONE && progress == 0f

    fun currentCell(): Pair<Int, Int> = if (progress == 0f) fromCol to fromRow else toCol to toRow
}

/** Pursuit AI state for an enemy "rally car". */
class EnemyCar(col: Int, row: Int) : Car(col, row) {
    var stunTimer = 0f
    var repathTimer = 0f
    var path: List<Pair<Int, Int>> = emptyList()

    val isStunned: Boolean get() = stunTimer > 0f
}

data class Flag(
    val col: Int,
    val row: Int,
    val special: Boolean = false,
    var collected: Boolean = false,
)

/** A temporary smoke cloud dropped behind the player that stuns chasers driving through it. */
data class SmokeCloud(val col: Int, val row: Int, var timeLeft: Float)
