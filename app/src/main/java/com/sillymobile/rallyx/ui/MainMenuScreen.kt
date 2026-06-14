package com.sillymobile.rallyx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun MainMenuScreen(onPlay: () -> Unit, onHighScores: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "RALLY DASH",
            color = Color(0xFFFFD500),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "a maze chase rally",
            color = Color(0xFF9AA7B0),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("PLAY")
        }

        Button(
            onClick = onHighScores,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(top = 12.dp),
        ) {
            Text("HIGH SCORES")
        }

        Text(
            "Collect every flag while dodging the rally cars.\n" +
                "Drop smoke to stun chasers, watch your fuel,\n" +
                "and grab the gold flag for a big bonus.\n" +
                "Every few stages brings a flag-rush challenge round.",
            color = Color(0xFF7F8C99),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}
