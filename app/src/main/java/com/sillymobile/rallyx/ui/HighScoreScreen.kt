package com.sillymobile.rallyx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sillymobile.rallyx.data.HighScore

@Composable
fun HighScoreScreen(scores: List<HighScore>, latestScore: Int?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
    ) {
        Text(
            "HIGH SCORES",
            color = Color(0xFFFFD500),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (scores.isEmpty()) {
            Text("No scores yet - go drive!", color = Color.White)
        } else {
            scores.forEachIndexed { index, entry ->
                val isLatest = latestScore != null && entry.score == latestScore
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${index + 1}.",
                        color = if (isLatest) Color(0xFFFFD500) else Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        "${entry.score}",
                        color = if (isLatest) Color(0xFFFFD500) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Lvl ${entry.level}",
                        color = if (isLatest) Color(0xFFFFD500) else Color(0xFF9AA7B0),
                        fontSize = 18.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(0.7f).padding(bottom = 24.dp)) {
                Text("MAIN MENU")
            }
        }
    }
}
