package com.a_survivor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a_survivor.app.data.GameState

@Composable
fun HudOverlay(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // HP 바
        StatBar(
            label    = "HP",
            value    = state.player.hp,
            max      = state.player.maxHp,
            barColor = Color(0xFFE53935)
        )
        Spacer(Modifier.height(4.dp))
        // EXP 바
        StatBar(
            label    = "EXP",
            value    = state.player.exp,
            max      = state.player.expToNextLevel,
            barColor = Color(0xFF7C4DFF)
        )
        Spacer(Modifier.height(6.dp))
        // 레벨 / 점수 / 생존 시간
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lv. ${state.player.level}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Score  ${state.score}",     color = Color.White, fontSize = 13.sp)
            Text("${state.survivalTime.toInt()}s", color = Color(0xFFFFEB3B), fontSize = 13.sp)
        }
    }
}

@Composable
private fun StatBar(label: String, value: Float, max: Float, barColor: Color) {
    val ratio = (value / max).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = label,
            color    = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color(0xFF424242), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(barColor, RoundedCornerShape(5.dp))
            )
        }
        Text(
            text     = "${value.toInt()}/${max.toInt()}",
            color    = Color.White,
            fontSize = 10.sp,
            modifier = Modifier
                .width(64.dp)
                .padding(start = 6.dp)
        )
    }
}
