package com.sillymobile.rallyx.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sillymobile.rallyx.audio.AudioManager
import com.sillymobile.rallyx.game.Direction
import com.sillymobile.rallyx.game.GameConstants
import com.sillymobile.rallyx.game.GameEngine
import com.sillymobile.rallyx.game.GameEvent
import com.sillymobile.rallyx.game.GamePhase
import kotlin.math.min

@Composable
fun GameScreen(onGameOver: (score: Int, level: Int) -> Unit, onExitToMenu: () -> Unit) {
    val engine = remember { GameEngine() }
    val audioManager = remember { AudioManager() }
    var tick by remember { mutableIntStateOf(0) }
    var gameOverReported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        engine.start()
        audioManager.onPhaseChanged(engine.phase)
        var lastTimeNanos = -1L
        while (true) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            if (lastTimeNanos < 0) lastTimeNanos = now
            val dt = ((now - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastTimeNanos = now

            val phaseBefore = engine.phase
            engine.update(dt)

            for (event in engine.drainEvents()) {
                audioManager.handleEvent(event)
                if (event == GameEvent.GameOver && !gameOverReported) {
                    gameOverReported = true
                }
            }
            if (engine.phase != phaseBefore) audioManager.onPhaseChanged(engine.phase)

            tick++
        }
    }

    LaunchedEffect(gameOverReported) {
        if (gameOverReported) onGameOver(engine.score, engine.level)
    }

    DisposableEffect(Unit) {
        onDispose { audioManager.release() }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val direction = when (event.key) {
                    Key.DirectionUp -> Direction.UP
                    Key.DirectionDown -> Direction.DOWN
                    Key.DirectionLeft -> Direction.LEFT
                    Key.DirectionRight -> Direction.RIGHT
                    else -> null
                }
                when {
                    direction != null && event.type == KeyEventType.KeyDown -> {
                        engine.inputDirection = direction
                        true
                    }
                    direction != null && event.type == KeyEventType.KeyUp -> {
                        if (engine.inputDirection == direction) engine.inputDirection = Direction.NONE
                        true
                    }
                    event.key == Key.Spacebar && event.type == KeyEventType.KeyDown -> {
                        if (event.nativeKeyEvent.repeatCount == 0) engine.requestSmoke()
                        true
                    }
                    else -> false
                }
            },
    ) {
        HudBar(engine = engine)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Reading `tick` here ties this Canvas's recomposition to the game loop.
            @Suppress("UNUSED_EXPRESSION")
            tick
            Canvas(modifier = Modifier.fillMaxSize()) {
                val mazeW = engine.maze.pixelWidth
                val mazeH = engine.maze.pixelHeight
                val scale = min(size.width / mazeW, size.height / mazeH)
                val offsetX = (size.width - mazeW * scale) / 2f
                val offsetY = (size.height - mazeH * scale) / 2f
                drawGame(engine, scale, offsetX, offsetY)
            }

            PhaseOverlay(engine = engine, onExitToMenu = onExitToMenu)
        }

        ControlsBar(engine = engine)
    }
}

@Composable
private fun HudBar(engine: GameEngine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("SCORE", color = Color.Gray, fontSize = 11.sp)
            Text("${engine.score}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FUEL", color = Color.Gray, fontSize = 11.sp)
            FuelBar(fraction = engine.fuel / GameConstants.FUEL_MAX)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (engine.isChallengeStage) "CHALLENGE" else "LEVEL ${engine.level}",
                color = if (engine.isChallengeStage) Color(0xFFFFC107) else Color.White,
                fontSize = if (engine.isChallengeStage) 14.sp else 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("LIVES: ${"▲".repeat(engine.lives.coerceAtLeast(0))}", color = Color(0xFFFFD500), fontSize = 14.sp)
        }
    }
}

@Composable
private fun FuelBar(fraction: Float) {
    val clamped = fraction.coerceIn(0f, 1f)
    val color = when {
        clamped > 0.5f -> Color(0xFF4CAF50)
        clamped > 0.2f -> Color(0xFFFFC107)
        else -> Color(0xFFE53935)
    }
    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 12.dp)
            .background(Color(0xFF333333)),
    ) {
        Box(
            modifier = Modifier
                .size(width = 90.dp * clamped, height = 12.dp)
                .background(color),
        )
    }
}

@Composable
private fun ControlsBar(engine: GameEngine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DPad(
            onPress = { dir -> engine.inputDirection = dir },
            onRelease = { dir -> if (engine.inputDirection == dir) engine.inputDirection = com.sillymobile.rallyx.game.Direction.NONE },
        )
        SmokeButton(charges = engine.smokeCharges, onTrigger = { engine.requestSmoke() })
    }
}

@Composable
private fun PhaseOverlay(engine: GameEngine, onExitToMenu: () -> Unit) {
    when (engine.phase) {
        GamePhase.CRASHED -> CenteredMessage("CRASH!", Color(0xFFE53935))
        GamePhase.LEVEL_CLEAR -> CenteredMessage(
            if (engine.lastCompletedWasChallenge()) "CHALLENGE COMPLETE!" else "STAGE CLEAR!",
            Color(0xFF4CAF50),
        )
        GamePhase.GAME_OVER -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GAME OVER", color = Color(0xFFE53935), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Final Score: ${engine.score}", color = Color.White, fontSize = 18.sp)
                Button(onClick = onExitToMenu, modifier = Modifier.padding(top = 16.dp)) {
                    Text("MAIN MENU")
                }
            }
        }
        GamePhase.CHALLENGE -> Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color(0x99000000))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    "TIME: ${engine.challengeTimeLeft.toInt()}",
                    color = Color(0xFFFFC107),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun CenteredMessage(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
