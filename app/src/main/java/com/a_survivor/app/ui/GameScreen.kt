package com.a_survivor.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a_survivor.app.data.GamePhase
import com.a_survivor.app.game.GameViewModel

@Composable
fun GameScreen(modifier: Modifier = Modifier, vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    // PC 에뮬레이터 방향키 지원: 눌린 키 집합으로 대각선 이동도 처리
    val pressedKeys = remember { mutableSetOf<Key>() }
    fun syncKeyInput() {
        vm.inputX = (if (Key.DirectionRight in pressedKeys) 1f else 0f) -
                    (if (Key.DirectionLeft  in pressedKeys) 1f else 0f)
        vm.inputY = (if (Key.DirectionDown  in pressedKeys) 1f else 0f) -
                    (if (Key.DirectionUp    in pressedKeys) 1f else 0f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                val key = keyEvent.key
                if (key in setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)) {
                    when (keyEvent.type) {
                        KeyEventType.KeyDown -> pressedKeys.add(key)
                        KeyEventType.KeyUp   -> pressedKeys.remove(key)
                    }
                    syncKeyInput()
                    true
                } else false
            }
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // 게임 시작 (스크린 크기 확정 후 한 번만) + 포커스 요청
        LaunchedEffect(w, h) {
            if (w > 0f && h > 0f) vm.startGame(w, h)
            focusRequester.requestFocus()
        }

        // ── 게임 월드 렌더링 (Canvas) ──────────────────────
        GameRenderer(state = state)

        // ── HUD (상단) ─────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            HudOverlay(state = state)
        }

        // ── D-pad (좌하단) ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, bottom = 32.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            DPadControl(size = 140.dp) { dx, dy ->
                vm.inputX = dx
                vm.inputY = dy
            }
        }

        // ── 업그레이드 선택 오버레이 ───────────────────────
        if (state.phase == GamePhase.UPGRADE_SELECTION) {
            UpgradeSelectionUI(
                choices  = state.upgradeChoices,
                onSelect = { vm.selectUpgrade(it) }
            )
        }

        // ── 게임 오버 오버레이 ─────────────────────────────
        if (state.phase == GamePhase.GAME_OVER) {
            GameOverScreen(
                state     = state,
                onRestart = { vm.restartGame() }
            )
        }
    }
}
