package com.a_survivor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 가상 D-pad 컨트롤
 * 터치/드래그로 방향 입력 → onInput(dx, dy) 콜백 (-1f ~ 1f)
 */
@Composable
fun DPadControl(
    size: Dp = 140.dp,
    onInput: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current
    val sizePx  = with(density) { size.toPx() }
    val half    = sizePx / 2f

    // 현재 눌린 방향 표시용 상태
    var activeDir by remember { mutableStateOf(Pair(0f, 0f)) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(Color(0x33FFFFFF), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // 터치 시작
                    val down = awaitFirstDown()
                    var dx = ((down.position.x - half) / half).coerceIn(-1f, 1f)
                    var dy = ((down.position.y - half) / half).coerceIn(-1f, 1f)
                    activeDir = Pair(dx, dy)
                    onInput(dx, dy)

                    // 드래그 추적
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            dx = ((change.position.x - half) / half).coerceIn(-1f, 1f)
                            dy = ((change.position.y - half) / half).coerceIn(-1f, 1f)
                            activeDir = Pair(dx, dy)
                            onInput(dx, dy)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    // 손 뗄 때 정지
                    activeDir = Pair(0f, 0f)
                    onInput(0f, 0f)
                }
            }
    ) {
        // 조이스틱 썸 (눌린 위치 표시)
        val thumbOffsetX = with(density) { (activeDir.first * half * 0.4f).toDp() }
        val thumbOffsetY = with(density) { (activeDir.second * half * 0.4f).toDp() }
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(thumbOffsetX, thumbOffsetY)
                .background(Color(0x88FFFFFF), CircleShape)
        )

        // 방향 화살표 힌트
        Text("↑", color = Color(0x99FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
        Text("↓", color = Color(0x99FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
        Text("←", color = Color(0x99FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
        Text("→", color = Color(0x99FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
    }
}
