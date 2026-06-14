package com.sillymobile.rallyx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sillymobile.rallyx.data.HighScoreRepository

private sealed interface Screen {
    data object Menu : Screen
    data object Game : Screen
    data class Scores(val latestScore: Int?) : Screen
}

@Composable
fun RallyXApp() {
    val context = LocalContext.current
    val repository = remember { HighScoreRepository(context) }
    var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
    var scores by remember { mutableStateOf(repository.getScores()) }

    when (val current = screen) {
        is Screen.Menu -> MainMenuScreen(
            onPlay = { screen = Screen.Game },
            onHighScores = {
                scores = repository.getScores()
                screen = Screen.Scores(null)
            },
        )

        is Screen.Game -> GameScreen(
            onGameOver = { score, level ->
                scores = repository.addScore(score, level)
                screen = Screen.Scores(score)
            },
            onExitToMenu = {
                screen = Screen.Menu
            },
        )

        is Screen.Scores -> HighScoreScreen(
            scores = scores,
            latestScore = current.latestScore,
            onBack = { screen = Screen.Menu },
        )
    }
}
