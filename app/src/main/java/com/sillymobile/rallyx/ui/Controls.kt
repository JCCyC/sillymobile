package com.sillymobile.rallyx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sillymobile.rallyx.game.Direction

/** On-screen 4-way digital pad, mirroring the original arcade cabinet's joystick. */
@Composable
fun DPad(
    modifier: Modifier = Modifier,
    onPress: (Direction) -> Unit,
    onRelease: (Direction) -> Unit,
) {
    Box(modifier.size(184.dp)) {
        DPadButton(Direction.UP, "▲", Modifier.align(Alignment.TopCenter), onPress, onRelease)
        DPadButton(Direction.DOWN, "▼", Modifier.align(Alignment.BottomCenter), onPress, onRelease)
        DPadButton(Direction.LEFT, "◀", Modifier.align(Alignment.CenterStart), onPress, onRelease)
        DPadButton(Direction.RIGHT, "▶", Modifier.align(Alignment.CenterEnd), onPress, onRelease)
    }
}

@Composable
private fun DPadButton(
    direction: Direction,
    label: String,
    modifier: Modifier,
    onPress: (Direction) -> Unit,
    onRelease: (Direction) -> Unit,
) {
    Box(
        modifier
            .size(64.dp)
            .background(Color(0x33FFFFFF), CircleShape)
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        onPress(direction)
                        tryAwaitRelease()
                        onRelease(direction)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** Round "smoke screen" button - the original's defensive weapon against the rally cars. */
@Composable
fun SmokeButton(modifier: Modifier = Modifier, charges: Int, onTrigger: () -> Unit) {
    Box(
        modifier
            .size(88.dp)
            .background(if (charges > 0) Color(0x55FF6F00) else Color(0x33555555), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTrigger() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "SMOKE\n$charges",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
