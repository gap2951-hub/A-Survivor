package com.a_survivor.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.a_survivor.app.data.GameState

@Composable
fun GameRenderer(state: GameState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 배경
        drawRect(color = Color(0xFF1A1A2E))

        // 투사체
        for (proj in state.projectiles) {
            drawCircle(
                color  = Color(0xFFFFEB3B),
                radius = 8f,
                center = Offset(proj.position.x, proj.position.y)
            )
        }

        // 적
        for (enemy in state.enemies) {
            drawEnemy(enemy.position.x, enemy.position.y, enemy.hp / enemy.maxHp)
        }

        // 플레이어
        drawPlayer(state.player.position.x, state.player.position.y)

        // 공격 범위 표시 (반투명 원)
        drawCircle(
            color  = Color(0x1A4CAF50),
            radius = state.player.attackRange,
            center = Offset(state.player.position.x, state.player.position.y)
        )
    }
}

private fun DrawScope.drawPlayer(x: Float, y: Float) {
    // 외곽 글로우
    drawCircle(color = Color(0x334CAF50), radius = 30f, center = Offset(x, y))
    // 몸체
    drawCircle(color = Color(0xFF4CAF50), radius = 22f, center = Offset(x, y))
    // 중심 하이라이트
    drawCircle(color = Color(0xFF81C784), radius = 12f, center = Offset(x, y))
}

private fun DrawScope.drawEnemy(x: Float, y: Float, hpRatio: Float) {
    // 몸체
    drawCircle(color = Color(0xFFE53935), radius = 20f, center = Offset(x, y))
    drawCircle(
        color  = Color(0xFFB71C1C),
        radius = 20f,
        center = Offset(x, y),
        style  = Stroke(width = 2f)
    )
    // HP 바 (적 위)
    val barW = 40f
    val barH = 5f
    val bx   = x - barW / 2f
    val by   = y - 32f
    drawRect(color = Color(0xFF424242), topLeft = Offset(bx, by), size = Size(barW, barH))
    drawRect(
        color    = if (hpRatio > 0.5f) Color(0xFF4CAF50) else Color(0xFFF44336),
        topLeft  = Offset(bx, by),
        size     = Size(barW * hpRatio, barH)
    )
}
