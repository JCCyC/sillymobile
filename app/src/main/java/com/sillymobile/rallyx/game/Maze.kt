package com.sillymobile.rallyx.game

import kotlin.random.Random

/**
 * A grid-based maze of [GameConstants.COLS] x [GameConstants.ROWS] cells. Each cell records
 * which of its four edges are open roads, generated with a randomized depth-first carve
 * (like a perfect maze) and then "braided" by knocking down extra walls so multiple routes
 * exist between rooms - similar in spirit to the looping circuits of the original arcade game.
 */
class Maze(seed: Long) {
    val cols = GameConstants.COLS
    val rows = GameConstants.ROWS

    // Bitmask per cell: bit0=UP open, bit1=RIGHT open, bit2=DOWN open, bit3=LEFT open
    private val openings = IntArray(cols * rows)

    init {
        generate(Random(seed))
    }

    private fun index(c: Int, r: Int) = r * cols + c

    private fun inBounds(c: Int, r: Int) = c in 0 until cols && r in 0 until rows

    fun isOpen(c: Int, r: Int, dir: Direction): Boolean {
        if (!inBounds(c, r)) return false
        val bit = when (dir) {
            Direction.UP -> 1
            Direction.RIGHT -> 2
            Direction.DOWN -> 4
            Direction.LEFT -> 8
            Direction.NONE -> return false
        }
        return (openings[index(c, r)] and bit) != 0
    }

    private fun setOpen(c: Int, r: Int, dir: Direction) {
        val bit = when (dir) {
            Direction.UP -> 1
            Direction.RIGHT -> 2
            Direction.DOWN -> 4
            Direction.LEFT -> 8
            Direction.NONE -> return
        }
        openings[index(c, r)] = openings[index(c, r)] or bit
    }

    /** Carves both ends of the wall between (c,r) and its neighbour in [dir]. */
    private fun openBetween(c: Int, r: Int, dir: Direction) {
        val nc = c + dir.dc
        val nr = r + dir.dr
        if (!inBounds(nc, nr)) return
        setOpen(c, r, dir)
        setOpen(nc, nr, dir.opposite())
    }

    private fun generate(random: Random) {
        val visited = BooleanArray(cols * rows)
        val stack = ArrayDeque<Pair<Int, Int>>()
        val start = 0 to 0
        visited[index(start.first, start.second)] = true
        stack.addLast(start)

        while (stack.isNotEmpty()) {
            val (c, r) = stack.last()
            val neighbours = Direction.entries
                .filter { it != Direction.NONE }
                .map { it to (c + it.dc to r + it.dr) }
                .filter { (_, pos) -> inBounds(pos.first, pos.second) && !visited[index(pos.first, pos.second)] }

            if (neighbours.isEmpty()) {
                stack.removeLast()
                continue
            }

            val (dir, next) = neighbours[random.nextInt(neighbours.size)]
            openBetween(c, r, dir)
            visited[index(next.first, next.second)] = true
            stack.addLast(next)
        }

        // Braid the maze: knock down ~22% of the remaining walls to create loops and
        // alternate routes, so chasers can be evaded the way they could in the arcade original.
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                for (dir in arrayOf(Direction.RIGHT, Direction.DOWN)) {
                    if (!isOpen(c, r, dir) && random.nextFloat() < 0.22f) {
                        openBetween(c, r, dir)
                    }
                }
            }
        }
    }

    fun openDirections(c: Int, r: Int): List<Direction> =
        Direction.entries.filter { it != Direction.NONE && isOpen(c, r, it) }

    fun cellCenter(c: Int, r: Int): Pair<Float, Float> {
        val size = GameConstants.CELL_SIZE
        return (c * size + size / 2f) to (r * size + size / 2f)
    }

    val pixelWidth: Float get() = cols * GameConstants.CELL_SIZE
    val pixelHeight: Float get() = rows * GameConstants.CELL_SIZE

    /** Picks [count] distinct cells, optionally biased to be far from [avoid]. */
    fun pickFlagCells(count: Int, avoid: Pair<Int, Int>, random: Random): List<Pair<Int, Int>> {
        val all = (0 until rows).flatMap { r -> (0 until cols).map { c -> c to r } }
            .filter { (c, r) -> kotlin.math.abs(c - avoid.first) + kotlin.math.abs(r - avoid.second) >= 2 }
            .shuffled(random)
        return all.take(count)
    }
}
