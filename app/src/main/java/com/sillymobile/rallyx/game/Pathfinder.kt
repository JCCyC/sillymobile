package com.sillymobile.rallyx.game

/** Breadth-first search over the maze's road graph. Returns the path from (exclusive) [start] to
 *  (inclusive) [goal], or an empty list if [start] == [goal] or no path exists. */
fun bfsPath(maze: Maze, start: Pair<Int, Int>, goal: Pair<Int, Int>): List<Pair<Int, Int>> {
    if (start == goal) return emptyList()

    val cameFrom = HashMap<Pair<Int, Int>, Pair<Int, Int>>()
    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.addLast(start)
    cameFrom[start] = start

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (current == goal) break
        for (dir in maze.openDirections(current.first, current.second)) {
            val next = (current.first + dir.dc) to (current.second + dir.dr)
            if (next !in cameFrom) {
                cameFrom[next] = current
                queue.addLast(next)
            }
        }
    }

    if (goal !in cameFrom) return emptyList()

    val path = mutableListOf<Pair<Int, Int>>()
    var cur = goal
    while (cur != start) {
        path.add(cur)
        cur = cameFrom[cur] ?: return emptyList()
    }
    return path.reversed()
}
