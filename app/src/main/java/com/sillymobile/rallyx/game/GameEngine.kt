package com.sillymobile.rallyx.game

import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

/**
 * Holds and updates the entire simulation state for one play session: the maze, the player's
 * car, pursuing enemy cars, flags, fuel, score, lives and stage progression (including
 * periodic no-enemy "challenge" bonus stages, similar to the original's bonus rounds).
 *
 * The engine is plain Kotlin (no Compose dependency) so it can be driven by a simple frame
 * loop and rendered by reading its public fields directly.
 */
class GameEngine {
    var maze: Maze = Maze(seed = 1L)
        private set

    var player = Car(0, 0)
        private set

    val enemies = mutableListOf<EnemyCar>()
    val flags = mutableListOf<Flag>()
    val smokeClouds = mutableListOf<SmokeCloud>()

    var score = 0
        private set
    var lives = GameConstants.LIVES_START
        private set
    var level = 1
        private set
    var fuel = GameConstants.FUEL_MAX
        private set
    var smokeCharges = GameConstants.SMOKE_MAX_CHARGES
        private set
    var phase = GamePhase.READY
        private set
    var challengeTimeLeft = 0f
        private set
    var gameTime = 0f
        private set

    /** Direction requested by the player's controls; consumed continuously each frame. */
    var inputDirection = Direction.NONE
    private var smokeRequested = false

    private var smokeRechargeTimer = 0f
    private var crashTimer = 0f
    private var levelClearTimer = 0f
    private var lastStageWasChallenge = false
    private var justCompletedChallenge = false

    fun lastCompletedWasChallenge(): Boolean = justCompletedChallenge
    private var garage = Pair(GameConstants.COLS / 2, GameConstants.ROWS / 2)
    private val random = Random(System.nanoTime())

    private val events = mutableListOf<GameEvent>()

    val isChallengeStage: Boolean get() = phase == GamePhase.CHALLENGE

    val playerSpeed: Float
        get() = min(
            GameConstants.PLAYER_BASE_SPEED + (level - 1) * GameConstants.PLAYER_SPEED_PER_LEVEL,
            GameConstants.PLAYER_MAX_SPEED,
        )

    private val enemySpeed: Float
        get() = min(
            GameConstants.ENEMY_BASE_SPEED + (level - 1) * GameConstants.ENEMY_SPEED_PER_LEVEL,
            GameConstants.ENEMY_MAX_SPEED,
        )

    /** Begins a fresh game from level 1. */
    fun start() {
        score = 0
        lives = GameConstants.LIVES_START
        level = 1
        lastStageWasChallenge = false
        startLevel(level, isChallenge = false)
    }

    fun requestSmoke() {
        smokeRequested = true
    }

    /** Returns and clears the queue of one-shot events for the audio/UI layer. */
    fun drainEvents(): List<GameEvent> {
        if (events.isEmpty()) return emptyList()
        val copy = events.toList()
        events.clear()
        return copy
    }

    fun update(dt: Float) {
        gameTime += dt
        when (phase) {
            GamePhase.PLAYING -> updatePlaying(dt)
            GamePhase.CHALLENGE -> updateChallenge(dt)
            GamePhase.CRASHED -> updateCrashed(dt)
            GamePhase.LEVEL_CLEAR -> updateLevelClear(dt)
            GamePhase.READY, GamePhase.GAME_OVER -> Unit
        }
    }

    // ------------------------------------------------------------------
    // Stage setup
    // ------------------------------------------------------------------

    private fun startLevel(levelNum: Int, isChallenge: Boolean) {
        maze = Maze(seed = levelNum * 7919L + 13L)
        player = Car(0, 0).apply {
            facing = maze.openDirections(0, 0).firstOrNull() ?: Direction.RIGHT
        }

        val flagCount = if (isChallenge) GameConstants.FLAGS_PER_MAZE + 4 else GameConstants.FLAGS_PER_MAZE
        val cells = maze.pickFlagCells(flagCount, 0 to 0, random)
        val specialCell = cells.maxByOrNull { bfsPath(maze, 0 to 0, it).size } ?: cells.firstOrNull()
        flags.clear()
        cells.forEach { cell -> flags.add(Flag(cell.first, cell.second, special = !isChallenge && cell == specialCell)) }

        fuel = GameConstants.FUEL_MAX
        smokeCharges = GameConstants.SMOKE_MAX_CHARGES
        smokeRechargeTimer = 0f
        smokeClouds.clear()
        enemies.clear()

        if (isChallenge) {
            challengeTimeLeft = GameConstants.CHALLENGE_TIME_SEC
            phase = GamePhase.CHALLENGE
            events.add(GameEvent.ChallengeStart)
        } else {
            val count = min(GameConstants.ENEMY_BASE_COUNT + (levelNum - 1), GameConstants.ENEMY_MAX_COUNT)
            repeat(count) {
                enemies.add(
                    EnemyCar(garage.first, garage.second).apply {
                        facing = maze.openDirections(garage.first, garage.second).randomOrNull(random) ?: Direction.UP
                    },
                )
            }
            phase = GamePhase.PLAYING
        }
    }

    // ------------------------------------------------------------------
    // Per-phase updates
    // ------------------------------------------------------------------

    private fun updatePlaying(dt: Float) {
        stepPlayer(dt)
        updateSmoke(dt)

        enemies.forEach { updateEnemy(it, dt) }

        fuel -= GameConstants.FUEL_DRAIN_PER_SEC * dt
        if (fuel <= 0f) {
            fuel = 0f
            triggerCrash()
            return
        }

        collectFlags(challengeMode = false)
        checkEnemyCollisions()
        checkLevelComplete()
    }

    private fun updateChallenge(dt: Float) {
        stepPlayer(dt)
        challengeTimeLeft -= dt

        collectFlags(challengeMode = true)

        if (challengeTimeLeft <= 0f || flags.all { it.collected }) {
            challengeTimeLeft = 0f
            justCompletedChallenge = true
            events.add(GameEvent.ChallengeComplete)
            phase = GamePhase.LEVEL_CLEAR
            levelClearTimer = 2f
        }
    }

    private fun updateCrashed(dt: Float) {
        crashTimer -= dt
        if (crashTimer <= 0f) {
            if (lives <= 0) {
                phase = GamePhase.GAME_OVER
                events.add(GameEvent.GameOver)
            } else {
                resetPositionsAfterCrash()
                phase = GamePhase.PLAYING
            }
        }
    }

    private fun updateLevelClear(dt: Float) {
        levelClearTimer -= dt
        if (levelClearTimer <= 0f) {
            level += 1
            if (!lastStageWasChallenge && level % GameConstants.CHALLENGE_LEVEL_EVERY == 0) {
                lastStageWasChallenge = true
                startLevel(level, isChallenge = true)
            } else {
                lastStageWasChallenge = false
                startLevel(level, isChallenge = false)
            }
        }
    }

    // ------------------------------------------------------------------
    // Movement
    // ------------------------------------------------------------------

    /** Advances [car] toward its target cell. Returns true if it just snapped onto a new cell. */
    private fun advance(car: Car, speed: Float, dt: Float): Boolean {
        if (car.moveDir == Direction.NONE) return false
        car.progress += speed * dt
        if (car.progress >= 1f) {
            car.fromCol = car.toCol
            car.fromRow = car.toRow
            car.progress = 0f
            car.moveDir = Direction.NONE
            return true
        }
        return false
    }

    private fun stepPlayer(dt: Float) {
        advance(player, playerSpeed, dt)
        if (player.moveDir == Direction.NONE) {
            val (c, r) = player.fromCol to player.fromRow
            val desired = inputDirection
            val dir = when {
                desired != Direction.NONE && maze.isOpen(c, r, desired) -> desired
                maze.isOpen(c, r, player.facing) -> player.facing
                else -> Direction.NONE
            }
            if (dir != Direction.NONE) {
                player.moveDir = dir
                player.facing = dir
                player.toCol = c + dir.dc
                player.toRow = r + dir.dr
            }
        }
    }

    private fun updateEnemy(enemy: EnemyCar, dt: Float) {
        if (enemy.isStunned) {
            enemy.stunTimer -= dt
            if (enemy.stunTimer < 0f) enemy.stunTimer = 0f
            checkEnemySmoke(enemy)
            return
        }

        advance(enemy, enemySpeed, dt)

        if (enemy.moveDir == Direction.NONE) {
            val (ec, er) = enemy.fromCol to enemy.fromRow
            val (pc, pr) = player.currentCell()

            enemy.repathTimer -= dt
            if (enemy.path.isEmpty() || enemy.repathTimer <= 0f) {
                enemy.path = bfsPath(maze, ec to er, pc to pr)
                enemy.repathTimer = 0.6f + random.nextFloat() * 0.6f
            }

            val nextCell = enemy.path.firstOrNull()
            val dir = if (nextCell != null) {
                directionTo(ec, er, nextCell)
            } else {
                val options = maze.openDirections(ec, er)
                val preferred = options.filter { it != enemy.facing.opposite() }
                (preferred.ifEmpty { options }).randomOrNull(random) ?: Direction.NONE
            }

            if (dir != Direction.NONE) {
                enemy.moveDir = dir
                enemy.facing = dir
                enemy.toCol = ec + dir.dc
                enemy.toRow = er + dir.dr
                if (nextCell != null && directionTo(ec, er, nextCell) == dir) {
                    enemy.path = enemy.path.drop(1)
                }
            }
        }

        checkEnemySmoke(enemy)
    }

    private fun checkEnemySmoke(enemy: EnemyCar) {
        val (ec, er) = enemy.currentCell()
        val inSmoke = smokeClouds.any { it.col == ec && it.row == er && it.timeLeft > 0f }
        if (inSmoke && !enemy.isStunned) {
            enemy.stunTimer = GameConstants.STUN_DURATION_SEC
            enemy.moveDir = Direction.NONE
            enemy.progress = 0f
            enemy.toCol = ec
            enemy.toRow = er
            events.add(GameEvent.EnemyStunned)
        }
    }

    private fun directionTo(c: Int, r: Int, target: Pair<Int, Int>): Direction =
        Direction.entries.firstOrNull { it != Direction.NONE && c + it.dc == target.first && r + it.dr == target.second }
            ?: Direction.NONE

    // ------------------------------------------------------------------
    // Smoke screen
    // ------------------------------------------------------------------

    private fun updateSmoke(dt: Float) {
        smokeClouds.forEach { it.timeLeft -= dt }
        smokeClouds.removeAll { it.timeLeft <= 0f }

        if (smokeCharges < GameConstants.SMOKE_MAX_CHARGES) {
            smokeRechargeTimer += dt
            if (smokeRechargeTimer >= GameConstants.SMOKE_RECHARGE_SEC) {
                smokeCharges += 1
                smokeRechargeTimer = 0f
            }
        }

        if (smokeRequested) {
            smokeRequested = false
            if (smokeCharges > 0) {
                val behind = player.facing.opposite()
                val (pc, pr) = player.currentCell()
                smokeClouds.add(SmokeCloud(pc + behind.dc, pr + behind.dr, GameConstants.SMOKE_DURATION_SEC))
                smokeCharges -= 1
                events.add(GameEvent.SmokeReleased)
            }
        }
    }

    // ------------------------------------------------------------------
    // Collisions & scoring
    // ------------------------------------------------------------------

    private fun collectFlags(challengeMode: Boolean) {
        val (px, py) = player.position()
        for (flag in flags) {
            if (flag.collected) continue
            val (fx, fy) = maze.cellCenter(flag.col, flag.row)
            if (hypot(px - fx, py - fy) < PICKUP_RADIUS) {
                flag.collected = true
                if (challengeMode) {
                    score += GameConstants.CHALLENGE_BONUS_PER_FLAG
                    events.add(GameEvent.FlagCollected(special = false))
                } else if (flag.special) {
                    score += GameConstants.SPECIAL_FLAG_SCORE
                    fuel = min(GameConstants.FUEL_MAX, fuel + GameConstants.SPECIAL_FLAG_FUEL_BONUS)
                    events.add(GameEvent.FlagCollected(special = true))
                } else {
                    score += GameConstants.FLAG_SCORE
                    fuel = min(GameConstants.FUEL_MAX, fuel + GameConstants.FLAG_FUEL_BONUS)
                    events.add(GameEvent.FlagCollected(special = false))
                }
            }
        }
    }

    private fun checkEnemyCollisions() {
        val (px, py) = player.position()
        for (enemy in enemies) {
            val (ex, ey) = enemy.position()
            if (hypot(px - ex, py - ey) < COLLISION_RADIUS) {
                if (enemy.isStunned) {
                    score += GameConstants.STUNNED_ENEMY_SCORE
                    events.add(GameEvent.EnemyDestroyed)
                    respawnEnemy(enemy)
                } else {
                    triggerCrash()
                    return
                }
            }
        }
    }

    private fun checkLevelComplete() {
        if (flags.all { it.collected }) {
            justCompletedChallenge = false
            events.add(GameEvent.LevelComplete)
            phase = GamePhase.LEVEL_CLEAR
            levelClearTimer = 2f
        }
    }

    private fun respawnEnemy(enemy: EnemyCar) {
        enemy.fromCol = garage.first
        enemy.fromRow = garage.second
        enemy.toCol = garage.first
        enemy.toRow = garage.second
        enemy.progress = 0f
        enemy.moveDir = Direction.NONE
        enemy.stunTimer = 0f
        enemy.path = emptyList()
        enemy.facing = maze.openDirections(garage.first, garage.second).randomOrNull(random) ?: Direction.UP
    }

    private fun triggerCrash() {
        events.add(GameEvent.Crashed)
        lives -= 1
        phase = GamePhase.CRASHED
        crashTimer = 1.4f
    }

    private fun resetPositionsAfterCrash() {
        player = Car(0, 0).apply {
            facing = maze.openDirections(0, 0).firstOrNull() ?: Direction.RIGHT
        }
        enemies.forEach { respawnEnemy(it) }
        smokeClouds.clear()
    }

    companion object {
        private const val PICKUP_RADIUS = GameConstants.CELL_SIZE * 0.35f
        private const val COLLISION_RADIUS = GameConstants.CELL_SIZE * 0.55f
    }
}
