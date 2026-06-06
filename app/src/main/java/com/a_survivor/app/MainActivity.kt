package com.a_survivor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.activity.compose.BackHandler
import com.a_survivor.app.model.CameraState
import com.a_survivor.app.model.DamageNumber
import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.DialogueSession
import com.a_survivor.app.model.DropItem
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.GroundItem
import com.a_survivor.app.model.MapType
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.MonsterState
import com.a_survivor.app.model.Npc
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.Portal
import com.a_survivor.app.model.QuestState
import com.a_survivor.app.model.QuestStatus
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.model.StatType
import com.a_survivor.app.model.attackRange
import com.a_survivor.app.model.Weapon
import com.a_survivor.app.viewmodel.InventoryItem
import com.a_survivor.app.viewmodel.MainViewModel
import com.a_survivor.app.viewmodel.UiState
import kotlin.math.roundToInt

// ── 색상 ──────────────────────────────────────────────────────────────────────
private val BgDark         = Color(0xFF0C0802)
private val PanelBg        = Color(0xFF1A1008)
private val PanelHeader    = Color(0xFF241500)
private val BorderGold     = Color(0xFF8B6914)
private val TextGold       = Color(0xFFFFDF7E)
private val TextMuted      = Color(0xFF9A7D52)
private val ColorSuccess   = Color(0xFF66BB6A)
private val ColorFailure   = Color(0xFFEF5350)
private val ColorDestroyed = Color(0xFFFF4081)
private val ColorError     = Color(0xFFFFB300)
private val ColorDisabled  = Color(0xFF3A2A14)
private val SlotEmpty      = Color(0xFF1E1408)
private val SlotBorder     = Color(0xFF4A3010)
private val DotNormal      = Color(0xFFFFD54F)
private val DotWhite       = Color(0xFF80DEEA)
private val DropHighlight  = Color(0xFF42A5F5)

// ── 아이템 툴팁 색상 (맵스토리 스타일) ────────────────────────────────────────
private val TipBg      = Color(0xFF262630)
private val TipSection = Color(0xFF1E1E28)
private val TipBorder  = Color(0xFF484858)
private val TipImgBg   = Color(0xFF383848)
private val TipImgBdr  = Color(0xFF585868)
private val TipText    = Color(0xFFEEEEEE)
private val TipOrange  = Color(0xFFFFAA00)
private val TipMuted   = Color(0xFF888899)
private val TipLine    = Color(0xFF3A3A4A)

// ── 드래그 상태 ───────────────────────────────────────────────────────────────
class DragDropState {
    var scrollType by mutableStateOf<ScrollType?>(null)
    var position   by mutableStateOf(Offset.Zero)
    val isDragging get() = scrollType != null
}

// ── 화면 상태 ─────────────────────────────────────────────────────────────────
sealed class AppScreen {
    object Title    : AppScreen()
    object JobSelect : AppScreen()
    object Game     : AppScreen()
}

// ── Activity ──────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val moveVm: MainViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        val dx = when (event.keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT,  android.view.KeyEvent.KEYCODE_A -> -1f
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_D ->  1f
            else -> 0f
        }
        val dy = when (event.keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_UP,   android.view.KeyEvent.KEYCODE_W -> -1f
            android.view.KeyEvent.KEYCODE_DPAD_DOWN, android.view.KeyEvent.KEYCODE_S ->  1f
            else -> 0f
        }
        if (dx != 0f || dy != 0f) {
            moveVm.movePlayer(dx, dy)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 풀스크린 몰입 모드 (상태바·내비게이션바 숨김)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.uiState.collectAsState()
            var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Title) }

            when (currentScreen) {
                AppScreen.Title -> TitleScreen(
                    onStart = {
                        vm.startGame()
                        currentScreen = AppScreen.Game
                    }
                )
                AppScreen.JobSelect -> JobSelectScreen(
                    onConfirm = { job ->
                        vm.startGame(job)
                        currentScreen = AppScreen.Game
                    },
                    onBack = { currentScreen = AppScreen.Title }
                )
                AppScreen.Game -> MainScreen(
                    state               = state,
                    onScrollSelected    = vm::selectScroll,
                    onEnhance           = vm::useSelectedScroll,
                    onUnequip           = vm::unequipEquipment,
                    onReset             = vm::resetEquipment,
                    onUnequipWeapon     = vm::unequipWeapon,
                    onResetWeapon       = vm::resetWeapon,
                    onMovePlayer        = vm::movePlayer,
                    onAllocateStat      = vm::allocateStat,
                    onAdvanceJob        = vm::advanceJob,
                    onClearResult       = vm::clearLastResult,
                    onTalkToNpc         = vm::startDialogue,
                    onNextDialoguePage  = vm::nextDialoguePage,
                    onChooseOption      = vm::chooseDialogueOption,
                    onCloseDialogue     = vm::closeDialogue
                )
            }
        }
    }
}

// ── 메인 화면 ─────────────────────────────────────────────────────────────────
@Composable
fun MainScreen(
    state: UiState,
    onScrollSelected: (ScrollType) -> Unit,
    onEnhance: () -> Unit,
    onUnequip: () -> Unit,
    onReset: () -> Unit,
    onUnequipWeapon: () -> Unit,
    onResetWeapon: () -> Unit,
    onMovePlayer: (Float, Float) -> Unit,
    onAllocateStat: (StatType) -> Unit,
    onAdvanceJob: (PlayerJob) -> Unit = {},
    onClearResult: () -> Unit = {},
    onTalkToNpc: (Int) -> Unit = {},
    onNextDialoguePage: () -> Unit = {},
    onChooseOption: (Int) -> Unit = {},
    onCloseDialogue: () -> Unit = {}
) {
    val dragState        = remember { DragDropState() }
    var isEquipmentOpen  by remember { mutableStateOf(false) }
    var isInventoryOpen  by remember { mutableStateOf(false) }
    var isStatOpen       by remember { mutableStateOf(false) }
    var equipSlotBounds  by remember { mutableStateOf<Rect?>(null) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }

    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }


    // 플로팅 창 위치 및 경계 계산
    val density       = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenW  = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenH  = with(density) { configuration.screenHeightDp.dp.toPx() }
    val panelWPx = with(density) { 280.dp.toPx() }
    val headerPx = with(density) { 36.dp.toPx() }
    val initPad  = with(density) { 8.dp.toPx() }

    var equipOffset  by remember { mutableStateOf(Offset(initPad, initPad)) }
    var statOffset   by remember { mutableStateOf(Offset(initPad, initPad)) }
    var inventOffset by remember { mutableStateOf(Offset(initPad, initPad)) }

    fun clampWin(o: Offset) = Offset(
        o.x.coerceIn(0f, (screenW - panelWPx).coerceAtLeast(0f)),
        o.y.coerceIn(0f, (screenH - headerPx).coerceAtLeast(0f))
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (joystickDirX != 0f || joystickDirY != 0f) {
                onMovePlayer(joystickDirX, joystickDirY)
            }
        }
    }

    val isDragOver = dragState.isDragging &&
            isEquipmentOpen &&
            state.equipment != null &&
            !state.equipment.destroyed &&
            equipSlotBounds?.contains(dragState.position) == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .onGloballyPositioned { rootWindowOffset = it.positionInWindow() }
    ) {
        // ① 게임 캔버스 렌더링
        GameCanvas(
            player        = state.player,
            monsters      = state.monsters,
            world         = state.world,
            groundItems   = state.groundItems,
            damageNumbers = state.damageNumbers,
            portals       = state.portals,
            projectiles   = state.projectiles,
            npcs          = state.npcs
        )

        // ② 상단 HUD
        GameHud(
            player = state.player,
            modifier = Modifier.align(Alignment.TopStart)
        )


        // ③ 강화 결과 메시지 (2초 후 자동 소멸)
        LaunchedEffect(state.lastResult) {
            if (state.lastResult != null) {
                delay(2000)
                onClearResult()
            }
        }
        state.lastResult?.let {
            ResultPanel(
                result = it,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            )
        }


        // ④ 장비창 플로팅
        if (isEquipmentOpen) {
            Column(
                modifier = Modifier
                    .zIndex(10f)
                    .offset { IntOffset(equipOffset.x.roundToInt(), equipOffset.y.roundToInt()) }
                    .width(280.dp)
            ) {
                EquipmentWindow(
                    equipment = state.equipment,
                    weapon = state.weapon,
                    isDragOver = isDragOver,
                    onSlotBounds = { equipSlotBounds = it },
                    onUnequip = onUnequip,
                    onReset = onReset,
                    onUnequipWeapon = onUnequipWeapon,
                    onResetWeapon = onResetWeapon,
                    onClose = { isEquipmentOpen = false },
                    onDrag = { delta -> equipOffset = clampWin(equipOffset + delta) }
                )
            }
        }

        // ⑤ 스탯창 플로팅
        if (isStatOpen) {
            Column(
                modifier = Modifier
                    .zIndex(10f)
                    .offset { IntOffset(statOffset.x.roundToInt(), statOffset.y.roundToInt()) }
                    .width(280.dp)
            ) {
                StatWindow(
                    player       = state.player,
                    derivedStats = state.derivedStats,
                    onAllocate   = onAllocateStat,
                    onClose      = { isStatOpen = false },
                    onDrag       = { delta -> statOffset = clampWin(statOffset + delta) }
                )
            }
        }

        // ⑥ 인벤토리창 플로팅
        if (isInventoryOpen) {
            Column(
                modifier = Modifier
                    .zIndex(10f)
                    .offset { IntOffset(inventOffset.x.roundToInt(), inventOffset.y.roundToInt()) }
                    .width(280.dp)
            ) {
                InventoryWindow(
                    inventory        = state.inventory,
                    dragState        = dragState,
                    rootWindowOffset = rootWindowOffset,
                    onClose          = { isInventoryOpen = false },
                    onDragEnd        = { scrollType, dropPos ->
                        if (isEquipmentOpen &&
                            equipSlotBounds?.contains(dropPos) == true &&
                            state.equipment != null && !state.equipment.destroyed) {
                            onScrollSelected(scrollType)
                            onEnhance()
                        }
                    },

                    onDrag = { delta -> inventOffset = clampWin(inventOffset + delta) }
                )
            }
        }

        // ⑦ 오른쪽 하단 HUD 버튼
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudButton(label = "스탯", isActive = isStatOpen)       { isStatOpen = !isStatOpen }
            HudButton(label = "장비", isActive = isEquipmentOpen)  { isEquipmentOpen = !isEquipmentOpen }
            HudButton(label = "인벤", isActive = isInventoryOpen)  { isInventoryOpen = !isInventoryOpen }
        }

        // ⑦ 가상 조이스틱 (왼쪽 하단 고정)
        JoystickControl(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp),
            onDirectionChange = { dx, dy ->
                joystickDirX = dx
                joystickDirY = dy
            }
        )

        // ⑧ 드래그 중인 아이템 고스트
        if (dragState.isDragging) {
            DragGhost(
                scrollType = dragState.scrollType!!,
                windowPosition = dragState.position,
                rootOffset = rootWindowOffset
            )
        }

        // ⑨ 전직 팝업
        if (state.jobAdvancementPending) {
            JobAdvancementDialog(onAdvance = onAdvanceJob)
        }

        // ⑩ NPC 근접 감지 및 [대화하기] 버튼
        val nearbyNpc = state.npcs.firstOrNull { npc ->
            val dx = state.player.positionX - npc.worldX
            val dy = state.player.positionY - npc.worldY
            kotlin.math.sqrt(dx * dx + dy * dy) <= npc.interactRange
        }
        if (nearbyNpc != null && state.activeDialogue == null) {
            Button(
                onClick = { onTalkToNpc(nearbyNpc.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B6914)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Text("대화하기", color = Color.White, fontSize = 14.sp)
            }
        }

        // ⑪ 대화창
        state.activeDialogue?.let { dlg ->
            DialogueWindow(
                session = dlg,
                onNext = onNextDialoguePage,
                onChoose = onChooseOption,
                onClose = onCloseDialogue
            )
        }

        // ⑫ 퀘스트 트래커
        if (state.questState.status == QuestStatus.IN_PROGRESS || state.questState.status == QuestStatus.READY_TO_COMPLETE) {
            QuestTrackerPanel(
                questState = state.questState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 80.dp)
            )
        }
    }
}

// ── 장비창 ────────────────────────────────────────────────────────────────────
@Composable
fun EquipmentWindow(
    equipment: Equipment?,
    weapon: Weapon?,
    isDragOver: Boolean,
    onSlotBounds: (Rect) -> Unit,
    onUnequip: () -> Unit,
    onReset: () -> Unit,
    onUnequipWeapon: () -> Unit,
    onResetWeapon: () -> Unit,
    onClose: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("장비창", onClose = onClose, onDrag = onDrag)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ① 머리 — 모자
            BodyRow { LockedSlot("모자", Modifier.size(SlotSize)) }

            // ② 얼굴 — 얼굴장식 / 눈장식 / 귀걸이
            BodyRow {
                LockedSlot("얼굴", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                LockedSlot("눈장식", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                LockedSlot("귀걸이", Modifier.size(SlotSize))
            }

            // ③ 목 — 목걸이
            BodyRow { LockedSlot("목걸이", Modifier.size(SlotSize)) }

            // ④ 어깨·상의·망토
            BodyRow {
                LockedSlot("어깨", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                LockedSlot("상의", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                LockedSlot("망토", Modifier.size(SlotSize))
            }

            // ⑤ 장갑·하의·무기  (장갑 = 드롭 타겟)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlovesSlot(
                    equipment = equipment,
                    isDragOver = isDragOver,
                    onBoundsChanged = onSlotBounds,
                    onUnequip = onUnequip,
                    onReset = onReset
                )
                Spacer(Modifier.width(6.dp))
                LockedSlot("하의", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                WeaponSlot(
                    weapon = weapon,
                    onUnequip = onUnequipWeapon,
                    onReset = onResetWeapon
                )
            }

            // ⑥ 신발·벨트
            BodyRow {
                LockedSlot("신발", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                LockedSlot("벨트", Modifier.size(SlotSize))
            }
        }
    }
}

private val SlotSize = 40.dp

@Composable
private fun BodyRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun WindowTitleBar(
    title: String,
    onClose: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelHeader)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .then(
                if (onDrag != null) Modifier.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(BorderGold)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = TextGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (onClose != null) {
            Text(
                text = "✕",
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClose() }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptySlot(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SlotEmpty)
            .border(1.dp, SlotBorder, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextMuted.copy(alpha = 0.45f),
            fontSize = 7.sp,
            textAlign = TextAlign.Center,
            lineHeight = 9.sp
        )
    }
}

@Composable
private fun LockedSlot(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ColorDisabled)
            .border(1.dp, SlotBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = TextMuted.copy(alpha = 0.25f),
                fontSize = 7.sp,
                textAlign = TextAlign.Center,
                lineHeight = 9.sp
            )
            Text(
                text = "잠금",
                color = TextMuted.copy(alpha = 0.18f),
                fontSize = 6.sp
            )
        }
    }
}

// 장갑 슬롯 — 이미지만 표시, 탭=정보, 꾹=해제/초기화
@Composable
private fun GlovesSlot(
    equipment: Equipment?,
    isDragOver: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onUnequip: () -> Unit,
    onReset: () -> Unit
) {
    var showInfo    by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val borderColor = when {
        isDragOver                        -> DropHighlight
        equipment?.destroyed == true      -> ColorDestroyed
        equipment != null                 -> Color(0xFF6A8800)
        else                              -> SlotBorder
    }
    val bgColor = when {
        isDragOver                        -> DropHighlight.copy(alpha = 0.18f)
        equipment?.destroyed == true      -> Color(0xFF2A0010)
        equipment != null                 -> Color(0xFF1A2200)
        else                              -> SlotEmpty
    }

    Box(
        modifier = Modifier
            .size(SlotSize)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(if (isDragOver) 2.dp else 1.dp, borderColor, RoundedCornerShape(4.dp))
            .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) }
            .then(
                if (equipment != null)
                    Modifier.pointerInput(equipment.destroyed) {
                        detectTapGestures(
                            onTap = { showInfo = true },
                            onLongPress = { showConfirm = true }
                        )
                    }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            equipment == null -> {
                Text("장갑", color = TextMuted.copy(alpha = 0.45f), fontSize = 7.sp)
            }
            equipment.destroyed -> {
                Text("✕", color = ColorDestroyed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.nogada_glove),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // isDragOver 힌트
        if (isDragOver) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DropHighlight.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text("↓", color = DropHighlight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 정보 다이얼로그
    if (showInfo && equipment != null) {
        ItemInfoDialog(equipment = equipment, onDismiss = { showInfo = false })
    }

    // 해제/초기화 확인 다이얼로그
    if (showConfirm && equipment != null) {
        if (equipment.destroyed) {
            ConfirmDialog(
                title = "장비 초기화",
                message = "${equipment.name}이(가) 파괴되었습니다.\n새 장비로 초기화하시겠습니까?",
                confirmText = "초기화",
                onConfirm = { showConfirm = false; onReset() },
                onDismiss = { showConfirm = false }
            )
        } else {
            ConfirmDialog(
                title = "장비 해제",
                message = "${equipment.name}을(를) 해제하시겠습니까?",
                confirmText = "해제",
                onConfirm = { showConfirm = false; onUnequip() },
                onDismiss = { showConfirm = false }
            )
        }
    }
}

// 무기 슬롯 — 탭=정보, 꾹=해제/초기화
@Composable
private fun WeaponSlot(
    weapon: Weapon?,
    onUnequip: () -> Unit,
    onReset: () -> Unit
) {
    var showInfo    by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val borderColor = if (weapon != null) Color(0xFF6A5500) else SlotBorder
    val bgColor     = if (weapon != null) Color(0xFF1C1600) else SlotEmpty

    Box(
        modifier = Modifier
            .size(SlotSize)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .then(
                if (weapon != null)
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showInfo = true },
                            onLongPress = { showConfirm = true }
                        )
                    }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            weapon != null -> Image(
                painter = painterResource(id = R.drawable.nogada_sword),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                contentScale = ContentScale.Fit
            )
            else -> Text("무기", color = TextMuted.copy(alpha = 0.45f), fontSize = 7.sp)
        }
    }

    if (showInfo && weapon != null) {
        WeaponInfoDialog(weapon = weapon, onDismiss = { showInfo = false })
    }

    if (showConfirm && weapon != null) {
        ConfirmDialog(
            title = "무기 해제",
            message = "${weapon.name}을(를) 해제하시겠습니까?",
            confirmText = "해제",
            onConfirm = { showConfirm = false; onUnequip() },
            onDismiss = { showConfirm = false }
        )
    }
}

// ── 아이템 정보 다이얼로그 ────────────────────────────────────────────────────
@Composable
private fun ItemInfoDialog(equipment: Equipment, onDismiss: () -> Unit) {
    val isDestroyed = equipment.destroyed
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(10.dp))
                .background(TipBg)
                .border(1.dp, TipBorder, RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())
        ) {
            // ① 헤더: 이름 + 상태
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = equipment.name,
                    color = if (isDestroyed) ColorDestroyed else TipText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isDestroyed) "파괴됨" else "교환 불가",
                    color = if (isDestroyed) ColorDestroyed else TipOrange,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = TipLine)

            // ② 이미지 + 강화 정보
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDestroyed) Color(0xFF3E0010) else TipImgBg)
                        .border(1.dp, if (isDestroyed) ColorDestroyed else TipImgBdr, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDestroyed) {
                        Text("✕", color = ColorDestroyed, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.nogada_glove),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    WeaponReqRow("최대 강화", "${equipment.maxUpgradeCount}회")
                    WeaponReqRow("남은 강화", "${equipment.remainingUpgradeCount}회")
                    WeaponReqRow(
                        "실패 횟수",
                        if (equipment.failedUpgradeCount > 0) "${equipment.failedUpgradeCount}회" else "-"
                    )
                }
            }

            HorizontalDivider(color = TipLine)

            // ③ 직업 탭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TipSection)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    PlayerJob.BEGINNER to "초보자",
                    PlayerJob.WARRIOR  to "전사",
                    PlayerJob.MAGE     to "마법사",
                    PlayerJob.ARCHER   to "궁수",
                    PlayerJob.THIEF    to "도적",
                    PlayerJob.PIRATE   to "해적"
                ).forEachIndexed { i, (job, label) ->
                    if (i > 0) {
                        Box(modifier = Modifier.width(1.dp).height(14.dp).background(TipLine))
                    }
                    val canEquip = equipment.availableJobs.contains(job)
                    Text(
                        text = label,
                        color = if (canEquip) TipOrange else TipMuted,
                        fontSize = 12.sp,
                        fontWeight = if (canEquip) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            HorizontalDivider(color = TipLine)

            // ④ 스탯
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeaponStatRow("공격력", "+${equipment.attackPower}")
                WeaponStatRow(
                    "업그레이드 가능 횟수",
                    "${equipment.remainingUpgradeCount} / ${equipment.maxUpgradeCount}"
                )
                WeaponStatRow(
                    "실패 횟수",
                    equipment.failedUpgradeCount.toString(),
                    isOrange = equipment.failedUpgradeCount > 0
                )
            }

            // ⑤ 설명
            if (equipment.description.isNotBlank()) {
                HorizontalDivider(color = TipLine)
                Text(
                    text = equipment.description,
                    color = TipMuted,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

// ── 무기 정보 다이얼로그 ──────────────────────────────────────────────────────
@Composable
private fun WeaponInfoDialog(weapon: Weapon, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(10.dp))
                .background(TipBg)
                .border(1.dp, TipBorder, RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())
        ) {
            // ① 헤더: 이름 + 교환 불가
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = weapon.name,
                    color = TipText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(text = "교환 불가", color = TipOrange, fontSize = 13.sp)
            }

            HorizontalDivider(color = TipLine)

            // ② 이미지 + 요구사항
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TipImgBg)
                        .border(1.dp, TipImgBdr, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nogada_sword),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    WeaponReqRow("REQ LEV", weapon.reqLevel.toString())
                    WeaponReqRow("REQ STR", weapon.reqStr.toString())
                    WeaponReqRow("REQ DEX", weapon.reqDex.toString())
                    WeaponReqRow("REQ INT", weapon.reqInt.toString())
                    WeaponReqRow("REQ LUK", weapon.reqLuk.toString())
                    WeaponReqRow("REQ POP", "-")
                }
            }

            HorizontalDivider(color = TipLine)

            // ③ 직업 탭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TipSection)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    PlayerJob.BEGINNER to "초보자",
                    PlayerJob.WARRIOR  to "전사",
                    PlayerJob.MAGE     to "마법사",
                    PlayerJob.ARCHER   to "궁수",
                    PlayerJob.THIEF    to "도적",
                    PlayerJob.PIRATE   to "해적"
                ).forEachIndexed { i, (job, label) ->
                    if (i > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(TipLine)
                        )
                    }
                    val canEquip = weapon.availableJobs.contains(job)
                    Text(
                        text = label,
                        color = if (canEquip) TipOrange else TipMuted,
                        fontSize = 12.sp,
                        fontWeight = if (canEquip) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            HorizontalDivider(color = TipLine)

            // ④ 스탯 목록
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeaponStatRow("무기분류", weapon.weaponType)
                WeaponStatRow("공격속도", weapon.attackSpeed)
                WeaponStatRow("STR", "+${weapon.strBonus}")
                WeaponStatRow("공격력", "+${weapon.attackPower}")
                WeaponStatRow("업그레이드 가능 횟수", weapon.maxUpgradeCount.toString())
                WeaponStatRow("가위 사용 가능 횟수", weapon.scissorCount.toString(), isOrange = true)
            }

            HorizontalDivider(color = TipLine)

            // ⑤ 설명
            if (weapon.description.isNotBlank()) {
                Text(
                    text = weapon.description,
                    color = TipMuted,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun WeaponReqRow(label: String, value: String) {
    Row {
        Text(text = "$label : ", color = TipText, fontSize = 12.sp)
        Text(text = value,       color = TipText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeaponStatRow(label: String, value: String, isOrange: Boolean = false) {
    val color = if (isOrange) TipOrange else TipText
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("· ",         color = if (isOrange) TipOrange else TipMuted, fontSize = 13.sp)
        Text("$label : ", color = color, fontSize = 13.sp)
        Text(value,        color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ── 확인 다이얼로그 ───────────────────────────────────────────────────────────
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PanelBg)
                .border(1.dp, BorderGold, RoundedCornerShape(10.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = TextGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(message, color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1800))
                ) {
                    Text("취소", color = TextMuted)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                ) {
                    Text(confirmText, color = TextGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── 결과 패널 ─────────────────────────────────────────────────────────────────
@Composable
fun ResultPanel(result: EnhancementResult, modifier: Modifier = Modifier) {
    val (bg, fg) = when (result) {
        is EnhancementResult.Success   -> Color(0xFF1B5E20) to ColorSuccess
        is EnhancementResult.Failure   -> Color(0xFF4E1210) to ColorFailure
        is EnhancementResult.Destroyed -> Color(0xFF3E0030) to ColorDestroyed
        is EnhancementResult.Error     -> Color(0xFF3E2400) to ColorError
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text(
            text = result.message,
            color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── 비트맵 로더 ──────────────────────────────────────────────────────────────
private fun loadBitmap(context: android.content.Context, resId: Int, maxSize: Int): ImageBitmap {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, opts)
    val rawMax = maxOf(opts.outWidth, opts.outHeight)
    opts.inJustDecodeBounds = false
    opts.inSampleSize = (rawMax / maxSize).coerceAtLeast(1).let { s ->
        var p = 1; while (p * 2 <= s) p *= 2; p
    }
    opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    return BitmapFactory.decodeResource(context.resources, resId, opts).asImageBitmap()
}

// ── 게임 캔버스 ───────────────────────────────────────────────────────────────
@Composable
private fun GameCanvas(
    player: Player,
    monsters: List<Monster>,
    world: GameWorld,
    groundItems: List<GroundItem>,
    damageNumbers: List<DamageNumber>,
    portals: List<Portal>,
    projectiles: List<com.a_survivor.app.model.Projectile> = emptyList(),
    npcs: List<Npc> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapBitmap       = remember { loadBitmap(context, R.drawable.map_beginner, 2048) }
    val townBitmap      = remember { loadBitmap(context, R.drawable.map_town, 2048) }
    val slimeBitmap     = remember { loadBitmap(context, R.drawable.slime, 256) }
    val scroll100Bitmap = remember { loadBitmap(context, R.drawable.scroll_100, 256) }
    val scroll60Bitmap  = remember { loadBitmap(context, R.drawable.scroll_60,  256) }
    val scroll10Bitmap  = remember { loadBitmap(context, R.drawable.scroll_10,  256) }
    val gloveBitmap       = remember { loadBitmap(context, R.drawable.nogada_glove, 256) }
    val energyBolt1       = remember { loadBitmap(context, R.drawable.energy_bolt_1, 128) }
    val energyBolt2       = remember { loadBitmap(context, R.drawable.energy_bolt_2, 128) }
    val energyBolt3       = remember { loadBitmap(context, R.drawable.energy_bolt_3, 128) }
    val energyBoltFrames  = remember { listOf(energyBolt1, energyBolt2, energyBolt3) }
    val npcChuchu         = remember { loadBitmap(context, R.drawable.npc_chuchu, 128) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val zoom = maxOf(size.width / world.width, size.height / world.height)
        val cam = CameraState(zoom = zoom)
            .followPlayer(player.positionX, player.positionY)
            .clampToWorld(world, size.width, size.height)

        val currentMapBitmap = if (world.mapType == MapType.TOWN) townBitmap else mapBitmap
        drawWorldBackground(cam, world, currentMapBitmap)
        groundItems.forEach { drawGroundItem(it, cam, scroll100Bitmap, scroll60Bitmap, scroll10Bitmap, gloveBitmap) }
        portals.forEach { drawPortal(it, cam) }
        npcs.forEach { drawNpc(it, cam, npcChuchu) }
        drawAttackRange(player, cam)
        monsters.forEach { drawMonster(it, cam, slimeBitmap) }
        projectiles.forEach { drawProjectile(it, cam, energyBoltFrames) }
        drawPlayer(player, cam)
        damageNumbers.forEach { drawDamageNumber(it, cam) }
    }
}

private fun DrawScope.drawWorldBackground(cam: CameraState, world: GameWorld, mapBitmap: ImageBitmap) {
    // 화면 전체를 어두운 색으로 먼저 채움 (월드 바깥 영역)
    drawRect(color = Color(0xFF040A04), size = size)

    // 월드 영역의 화면 좌표 계산
    val tl = cam.toScreenOffset(0f, 0f, size.width, size.height)
    val br = cam.toScreenOffset(world.width, world.height, size.width, size.height)
    val dstX = tl.x.toInt()
    val dstY = tl.y.toInt()
    val dstW = (br.x - tl.x).toInt().coerceAtLeast(1)
    val dstH = (br.y - tl.y).toInt().coerceAtLeast(1)

    // 맵 이미지를 월드 영역에 맞게 그리기
    drawImage(
        image         = mapBitmap,
        dstOffset     = androidx.compose.ui.unit.IntOffset(dstX, dstY),
        dstSize       = IntSize(dstW, dstH),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.High
    )
}

private fun DrawScope.drawGroundItem(
    item: GroundItem,
    cam: CameraState,
    scroll100: ImageBitmap,
    scroll60: ImageBitmap,
    scroll10: ImageBitmap,
    glove: ImageBitmap
) {
    val pos      = cam.toScreenOffset(item.positionX, item.positionY, size.width, size.height)
    val iconSize = (size.height * 0.048f).toInt().coerceAtLeast(12)
    val sp       = size.height / 1080f

    // 바닥 글로우
    drawCircle(Color(0x44FFEE44), radius = iconSize * 0.75f, center = pos)

    // 아이템 이미지
    val bitmap = when (val drop = item.dropItem) {
        is DropItem.ScrollDrop -> when (drop.scrollType) {
            ScrollType.GLOVE_ATK_100 -> scroll100
            ScrollType.GLOVE_ATK_60  -> scroll60
            else                     -> scroll10
        }
        is DropItem.EquipmentDrop -> glove
    }
    drawImage(
        image         = bitmap,
        dstOffset     = androidx.compose.ui.unit.IntOffset(
            (pos.x - iconSize / 2).toInt(),
            (pos.y - iconSize).toInt()
        ),
        dstSize       = IntSize(iconSize, iconSize),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.High
    )

    // 아이템 이름 텍스트
    val label = when (val drop = item.dropItem) {
        is DropItem.ScrollDrop    -> ScrollCatalog.get(drop.scrollType).name
        is DropItem.EquipmentDrop -> drop.equipment.name
    }
    val labelPaint = android.graphics.Paint().apply {
        color       = android.graphics.Color.parseColor("#FFEE66")
        textSize    = 14f * sp
        textAlign   = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(label, pos.x, pos.y + 18f * sp, labelPaint)
}

private fun DrawScope.drawPortal(portal: Portal, cam: CameraState) {
    val pos = cam.toScreenOffset(portal.worldX, portal.worldY, size.width, size.height)
    val sp  = size.height / 1080f
    val r   = 24f * cam.zoom

    // 다층 글로우
    drawCircle(Color(0x2266BBFF), radius = r * 2.6f, center = pos)
    drawCircle(Color(0x4466BBFF), radius = r * 1.8f, center = pos)
    // 내부 채움
    drawCircle(Color(0x884499DD), radius = r, center = pos)
    // 외곽 링
    drawCircle(Color(0xFFAADDFF), radius = r, center = pos,
        style = Stroke(width = 2.5f * sp))
    drawCircle(Color(0x8866AAFF), radius = r * 1.1f, center = pos,
        style = Stroke(width = 1.5f * sp))

    // 맵 이름 레이블
    val paint = android.graphics.Paint().apply {
        color       = android.graphics.Color.parseColor("#AADDFF")
        textSize    = 15f * sp
        textAlign   = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(5f, 0f, 2f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(portal.label, pos.x, pos.y - r - 8f * sp, paint)
}

private fun DrawScope.drawAttackRange(player: Player, cam: CameraState) {
    val center = cam.toScreenOffset(player.positionX, player.positionY, size.width, size.height)
    val r = player.job.attackRange() * cam.zoom
    drawCircle(Color.White.copy(alpha = 0.05f), radius = r, center = center)
    drawCircle(Color.White.copy(alpha = 0.12f), radius = r, center = center,
        style = Stroke(width = 1f))
}

private fun DrawScope.drawProjectile(
    proj: com.a_survivor.app.model.Projectile,
    cam: CameraState,
    energyBoltFrames: List<ImageBitmap> = emptyList()
) {
    val c  = cam.toScreenOffset(proj.positionX, proj.positionY, size.width, size.height)
    val sp = size.height / 1080f

    when (proj.type) {
        com.a_survivor.app.model.ProjectileType.ENERGY_BOLT -> {
            if (energyBoltFrames.isNotEmpty()) {
                val frameIndex = ((System.currentTimeMillis() / 100L) % energyBoltFrames.size).toInt()
                val bitmap  = energyBoltFrames[frameIndex]
                val imgSize = (size.height * 0.10f).toInt().coerceAtLeast(24)
                val dx = proj.targetX - proj.positionX
                val dy = proj.targetY - proj.positionY
                val angleDeg = (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * (180.0 / kotlin.math.PI)).toFloat() + 180f
                withTransform({
                    rotate(angleDeg, pivot = c)
                }) {
                    drawImage(
                        image         = bitmap,
                        dstOffset     = IntOffset((c.x - imgSize / 2).toInt(), (c.y - imgSize / 2).toInt()),
                        dstSize       = IntSize(imgSize, imgSize),
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                    )
                }
            } else {
                drawCircle(Color(0xFF4488FF).copy(alpha = 0.5f), radius = 10f * sp, center = c)
                drawCircle(Color(0xFF88CCFF), radius = 5f * sp, center = c)
            }
        }
        com.a_survivor.app.model.ProjectileType.ARROW -> {
            val dx  = proj.targetX - proj.positionX
            val dy  = proj.targetY - proj.positionY
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val nx  = dx / len
            val ny  = dy / len
            drawLine(
                color       = Color(0xFFAA7744),
                start       = Offset(c.x - nx * 14f * sp, c.y - ny * 14f * sp),
                end         = c,
                strokeWidth = 2.5f * sp
            )
        }
        com.a_survivor.app.model.ProjectileType.THROWING_STAR -> {
            drawCircle(Color(0xFFCCCCCC), radius = 5f * sp, center = c)
            drawCircle(Color(0xFF888888).copy(alpha = 0.5f), radius = 8f * sp, center = c,
                style = Stroke(width = 1f * sp))
        }
        com.a_survivor.app.model.ProjectileType.BULLET -> {
            drawCircle(Color(0xFF555555), radius = 4f * sp, center = c)
            drawCircle(Color(0xFFAAAAAA), radius = 2f * sp, center = c)
        }
    }
}

private fun DrawScope.drawPlayer(player: Player, cam: CameraState) {
    val c = cam.toScreenOffset(player.positionX, player.positionY, size.width, size.height)
    val r = size.height * 0.026f

    // 그림자
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = r * 1.15f,
        center = Offset(c.x, c.y + r * 0.4f))
    // 몸통
    drawCircle(Color(0xFFFFAA33), radius = r, center = c)
    // 테두리
    drawCircle(Color(0xFFFFDD88), radius = r, center = c, style = Stroke(width = 2f))
    // 중심 하이라이트
    drawCircle(Color.White.copy(alpha = 0.5f), radius = r * 0.28f,
        center = Offset(c.x - r * 0.2f, c.y - r * 0.2f))
}

private fun DrawScope.drawMonster(monster: Monster, cam: CameraState, slimeBitmap: ImageBitmap) {
    val c       = cam.toScreenOffset(monster.positionX, monster.positionY, size.width, size.height)
    val imgSize = (size.height * 0.088f).toInt().coerceAtLeast(20)
    val sp      = size.height / 1080f

    // 그림자
    drawCircle(
        Color.Black.copy(alpha = 0.3f),
        radius = imgSize * 0.35f,
        center = Offset(c.x, c.y + imgSize * 0.3f)
    )

    // 슬라임 이미지
    drawImage(
        image         = slimeBitmap,
        dstOffset     = androidx.compose.ui.unit.IntOffset(
            (c.x - imgSize / 2).toInt(),
            (c.y - imgSize / 2).toInt()
        ),
        dstSize       = IntSize(imgSize, imgSize),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.High
    )

    // HP 바
    val isAggro = monster.state != MonsterState.IDLE
    val barW = imgSize * 1.2f
    val barH = 4f * sp
    val barX = c.x - barW / 2f
    val barY = c.y - imgSize * 0.6f - 6f * sp
    val frac = (monster.hp.toFloat() / monster.maxHp).coerceIn(0f, 1f)
    drawRect(Color(0xFF661111), topLeft = Offset(barX, barY), size = Size(barW, barH))
    if (frac > 0f) drawRect(
        if (isAggro) Color(0xFFFF6600) else Color(0xFF44BB00),
        topLeft = Offset(barX, barY), size = Size(barW * frac, barH)
    )

    // 어그로 상태 "!" 표시
    if (isAggro) {
        val aggroPaint = android.graphics.Paint().apply {
            color       = android.graphics.Color.RED
            textSize    = 16f * sp
            textAlign   = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawContext.canvas.nativeCanvas.drawText(
            "!",
            c.x,
            barY - 4f * sp,
            aggroPaint
        )
    }
}

private fun DrawScope.drawDamageNumber(num: DamageNumber, cam: CameraState) {
    val elapsed  = System.currentTimeMillis() - num.createdAt
    val progress = (elapsed / 800f).coerceIn(0f, 1f)
    val alpha    = (1f - progress).coerceIn(0f, 1f)
    if (alpha <= 0f) return

    val floatY = num.worldY - 55f * progress
    val pos    = cam.toScreenOffset(num.worldX, floatY, size.width, size.height)

    val paint = android.graphics.Paint().apply {
        color = when {
            num.isMiss         -> android.graphics.Color.parseColor("#AADDFF")
            num.isPlayerDamage -> android.graphics.Color.parseColor("#FF4444")
            else               -> android.graphics.Color.parseColor("#FFEE00")
        }
        textSize       = (if (num.isPlayerDamage || num.isMiss) 20f else 17f) * (size.height / 1080f)
        textAlign      = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias    = true
        this.alpha     = (alpha * 255).toInt()
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }
    val text = if (num.isMiss) "MISS" else num.value.toString()
    drawContext.canvas.nativeCanvas.drawText(text, pos.x, pos.y, paint)
}

// ── 상단 HUD ─────────────────────────────────────────────────────────────────
@Composable
private fun GameHud(player: Player, modifier: Modifier = Modifier) {
    val requiredExp = player.level * 20
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Lv.${player.level}  ${player.job.koreanName()}",
            color = TextGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        HpBar(current = player.hp, max = player.maxHp)
        ExpBar(current = player.exp, max = requiredExp)
    }
}

@Composable
private fun ExpBar(current: Int, max: Int) {
    val fraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF1A1A00))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Color(0xFFCCCC00))
        )
        Text(
            text = "$current / $max",
            color = Color.White,
            fontSize = 7.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun HpBar(current: Int, max: Int) {
    val fraction = (current.toFloat() / max).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF3A0000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Color(0xFFCC2222))
        )
        Text(
            text = "$current / $max",
            color = Color.White,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private fun PlayerJob.koreanName() = when (this) {
    PlayerJob.BEGINNER -> "초보자"
    PlayerJob.WARRIOR  -> "전사"
    PlayerJob.MAGE     -> "마법사"
    PlayerJob.ARCHER   -> "궁수"
    PlayerJob.THIEF    -> "도적"
    PlayerJob.PIRATE   -> "해적"
}

private fun PlayerJob.themeColor() = when (this) {
    PlayerJob.WARRIOR  -> Color(0xFF5C1010)
    PlayerJob.MAGE     -> Color(0xFF1A1055)
    PlayerJob.ARCHER   -> Color(0xFF104020)
    PlayerJob.THIEF    -> Color(0xFF251040)
    PlayerJob.PIRATE   -> Color(0xFF10305C)
    PlayerJob.BEGINNER -> Color(0xFF3A3010)
}

private fun PlayerJob.accentColor() = when (this) {
    PlayerJob.WARRIOR  -> Color(0xFFE53935)
    PlayerJob.MAGE     -> Color(0xFF9575CD)
    PlayerJob.ARCHER   -> Color(0xFF66BB6A)
    PlayerJob.THIEF    -> Color(0xFFAB47BC)
    PlayerJob.PIRATE   -> Color(0xFF42A5F5)
    PlayerJob.BEGINNER -> Color(0xFFFFD54F)
}

private fun PlayerJob.tagline() = when (this) {
    PlayerJob.WARRIOR  -> "강인한 근접 전투사"
    PlayerJob.MAGE     -> "강력한 마법 사용자"
    PlayerJob.ARCHER   -> "날렵한 원거리 궁수"
    PlayerJob.THIEF    -> "민첩한 암습 전문가"
    PlayerJob.PIRATE   -> "바다를 누비는 해적"
    PlayerJob.BEGINNER -> "모험을 시작하는 초보자"
}

private fun PlayerJob.jobDescription() = when (this) {
    PlayerJob.WARRIOR  -> "STR을 올리면 공격력이 크게 증가합니다.\nDEX를 올리면 명중률이 높아집니다."
    PlayerJob.MAGE     -> "INT를 올리면 마력이 크게 증가합니다.\nLUK를 올리면 명중률과 회피율이 높아집니다."
    PlayerJob.ARCHER   -> "DEX를 올리면 공격력과 명중률이 함께 증가합니다.\nSTR로 추가 공격력을 확보하세요."
    PlayerJob.THIEF    -> "LUK를 올리면 공격력과 회피율이 크게 증가합니다.\nDEX로 명중률을 보완하세요."
    PlayerJob.PIRATE   -> "DEX를 올리면 공격력과 명중률이 높아집니다.\nSTR로 추가 공격력을 확보하세요."
    PlayerJob.BEGINNER -> "모든 스탯이 균형 잡혀 있습니다.\n어떤 방향으로든 자유롭게 성장할 수 있습니다."
}

private fun PlayerJob.startLabel() = when (this) {
    PlayerJob.WARRIOR  -> "전사로 시작"
    PlayerJob.MAGE     -> "마법사로 시작"
    PlayerJob.ARCHER   -> "궁수로 시작"
    PlayerJob.THIEF    -> "도적으로 시작"
    PlayerJob.PIRATE   -> "해적으로 시작"
    PlayerJob.BEGINNER -> "초보자로 시작"
}

private fun PlayerJob.advanceLabel() = when (this) {
    PlayerJob.WARRIOR  -> "전사로 전직하기"
    PlayerJob.MAGE     -> "마법사로 전직하기"
    PlayerJob.ARCHER   -> "궁수로 전직하기"
    PlayerJob.THIEF    -> "도적으로 전직하기"
    PlayerJob.PIRATE   -> "해적으로 전직하기"
    PlayerJob.BEGINNER -> "초보자로 전직하기"
}

// ── HUD 버튼 ─────────────────────────────────────────────────────────────────
@Composable
private fun HudButton(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFF3A2800) else Color(0xFF1A1008))
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) BorderGold else BorderGold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) TextGold else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── 스탯창 ───────────────────────────────────────────────────────────────────
@Composable
private fun StatWindow(
    player: Player,
    derivedStats: DerivedStats,
    onAllocate: (StatType) -> Unit,
    onClose: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null
) {
    val hasPoints = player.availableStatPoint > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("스탯창", onClose = onClose, onDrag = onDrag)

        Column(modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
        ) {

            // 남은 SP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("남은 SP", color = TextMuted, fontSize = 13.sp)
                Text(
                    text = "${player.availableStatPoint}",
                    color = if (hasPoints) ColorSuccess else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                color = BorderGold.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 기본 스탯 행
            StatAllocRow("STR", player.stats.str, hasPoints) { onAllocate(StatType.STR) }
            StatAllocRow("DEX", player.stats.dex, hasPoints) { onAllocate(StatType.DEX) }
            StatAllocRow("INT", player.stats.`int`, hasPoints) { onAllocate(StatType.INT) }
            StatAllocRow("LUK", player.stats.luk, hasPoints) { onAllocate(StatType.LUK) }

            HorizontalDivider(
                color = BorderGold.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 전투 능력치 헤더
            Text(
                text = "전투 능력치",
                color = TextGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            DerivedStatRow("공격력",      derivedStats.attackPower.toString())
            DerivedStatRow("마력",        derivedStats.magicPower.toString())
            DerivedStatRow("명중률",      derivedStats.accuracy.toString())
            DerivedStatRow("회피율",      derivedStats.avoidability.toString())

            Spacer(Modifier.height(4.dp))

            DerivedStatRow("물리방어력",  derivedStats.physicalDefense.toString(), note = "장비 효과")
            DerivedStatRow("마법방어력",  derivedStats.magicDefense.toString(),    note = "장비 효과")
            DerivedStatRow("치명타율",    "${(derivedStats.criticalRate * 100).toInt()}%", note = "장비 효과")
            DerivedStatRow("이동속도",    if (derivedStats.moveSpeed == 0f) "기본" else "+${derivedStats.moveSpeed.toInt()}", note = "장비 효과")
            DerivedStatRow("공격속도",    if (derivedStats.attackSpeed == 0f) "기본" else "+${derivedStats.attackSpeed.toInt()}", note = "장비 효과")
        }
    }
}

@Composable
private fun DerivedStatRow(label: String, value: String, note: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        if (note != null) {
            Text(
                text = note,
                color = TextMuted.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        Text(
            text = value,
            color = TextGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatAllocRow(
    label: String,
    value: Int,
    canAllocate: Boolean,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = value.toString(),
            color = TextGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (canAllocate) Color(0xFF3A2800) else ColorDisabled)
                .border(
                    1.dp,
                    if (canAllocate) BorderGold else SlotBorder,
                    RoundedCornerShape(4.dp)
                )
                .then(
                    if (canAllocate) Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onPlus() } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = if (canAllocate) TextGold else TextMuted.copy(alpha = 0.3f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── 패널 오버레이 (스크림 + 스크롤 가능한 패널) ─────────────────────────────
@Composable
private fun PanelOverlay(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            content()
        }
    }
}

// ── 인벤토리창 ────────────────────────────────────────────────────────────────
@Composable
fun InventoryWindow(
    inventory: List<InventoryItem>,
    dragState: DragDropState,
    rootWindowOffset: Offset,
    onDragEnd: (ScrollType, Offset) -> Unit,
    onClose: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("인벤토리 - 주문서", onClose = onClose, onDrag = onDrag)

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "아이템을 꾹 눌러 장갑 슬롯으로 드래그하세요",
                color = TextMuted, fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))

            val rows = inventory.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        DraggableScrollItem(
                            item = item,
                            dragState = dragState,
                            rootWindowOffset = rootWindowOffset,
                            onDragEnd = onDragEnd,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DraggableScrollItem(
    item: InventoryItem,
    dragState: DragDropState,
    rootWindowOffset: Offset,
    onDragEnd: (ScrollType, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll   = ScrollCatalog.get(item.scrollType)
    val isEmpty  = item.quantity <= 0
    val isBeingDragged = dragState.isDragging && dragState.scrollType == item.scrollType

    var itemWindowPos by remember { mutableStateOf(Offset.Zero) }

    val bgColor     = if (scroll.isWhiteScroll) Color(0xFF002244) else Color(0xFF2A1A00)
    val borderColor = if (scroll.isWhiteScroll) DotWhite else DotNormal

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isEmpty) ColorDisabled else bgColor)
            .border(
                1.dp,
                if (isEmpty) SlotBorder else borderColor.copy(alpha = 0.6f),
                RoundedCornerShape(6.dp)
            )
            .alpha(if (isBeingDragged) 0.25f else 1f)
            .onGloballyPositioned { itemWindowPos = it.positionInWindow() }
            .pointerInput(item.scrollType, isEmpty) {
                if (isEmpty) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        dragState.scrollType = item.scrollType
                        dragState.position = itemWindowPos + localOffset
                    },
                    onDrag = { _, delta ->
                        dragState.position += delta
                    },
                    onDragEnd = {
                        val finalPos   = dragState.position
                        val scrollType = dragState.scrollType
                        dragState.scrollType = null
                        if (scrollType != null) onDragEnd(scrollType, finalPos)
                    },
                    onDragCancel = { dragState.scrollType = null }
                )
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (scroll.isWhiteScroll) "백의" else "${scroll.successRate}%",
                color = if (isEmpty) TextMuted.copy(0.3f)
                        else if (scroll.isWhiteScroll) DotWhite else DotNormal,
                fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
            Text(
                text = "×${item.quantity}",
                color = if (isEmpty) ColorDisabled else TextGold,
                fontSize = 11.sp
            )
        }
    }
}

// ── 가상 조이스틱 ─────────────────────────────────────────────────────────────
@Composable
fun JoystickControl(
    modifier: Modifier = Modifier,
    onDirectionChange: (Float, Float) -> Unit
) {
    val baseSize   = 120.dp
    val thumbSize  = 44.dp
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .size(baseSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val raw    = change.position - center
                        val maxR   = size.width / 2f
                        val dist   = raw.getDistance()
                        val clamped = if (dist <= maxR) raw else raw * (maxR / dist)
                        thumbOffset = clamped
                        onDirectionChange(clamped.x / maxR, clamped.y / maxR)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onDirectionChange(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onDirectionChange(0f, 0f)
                    }
                )
            }
    ) {
        val center  = this.center
        val baseR   = size.minDimension / 2f
        val thumbR  = thumbSize.toPx() / 2f

        // 베이스 원
        drawCircle(color = Color.White.copy(alpha = 0.12f), radius = baseR,  center = center)
        drawCircle(color = Color.White.copy(alpha = 0.30f), radius = baseR,  center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

        // 썸(조이스틱 핸들)
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = thumbR, center = center + thumbOffset)
        drawCircle(color = Color.White.copy(alpha = 0.25f), radius = thumbR, center = center + thumbOffset,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
    }
}

// ── 드래그 고스트 ─────────────────────────────────────────────────────────────
@Composable
private fun DragGhost(
    scrollType: ScrollType,
    windowPosition: Offset,
    rootOffset: Offset
) {
    val scroll = ScrollCatalog.get(scrollType)
    val localPos = windowPosition - rootOffset
    val ghostSize = 72.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (localPos.x - ghostSize.toPx() / 2).roundToInt(),
                    (localPos.y - ghostSize.toPx() / 2).roundToInt()
                )
            }
            .size(ghostSize)
            .zIndex(100f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                (if (scroll.isWhiteScroll) Color(0xDD003366) else Color(0xDD332200))
            )
            .border(
                2.dp,
                if (scroll.isWhiteScroll) DotWhite else DotNormal,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (scroll.isWhiteScroll) "백의" else "${scroll.successRate}%",
                color = if (scroll.isWhiteScroll) DotWhite else DotNormal,
                fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Text(
                text = "주문서",
                color = TextGold.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}

// ── 타이틀 화면 ───────────────────────────────────────────────────────────────
@Composable
fun TitleScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "A-Survivor",
                color = TextGold,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Text(
                text = "메이플스토리 스타일 생존 RPG",
                color = TextMuted,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            HorizontalDivider(
                color = BorderGold.copy(alpha = 0.5f),
                modifier = Modifier
                    .width(220.dp)
                    .padding(vertical = 8.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF241500))
                    .border(1.dp, BorderGold, RoundedCornerShape(6.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onStart() }
                    .padding(horizontal = 52.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "게임 시작",
                    color = TextGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── 직업 선택 화면 ────────────────────────────────────────────────────────────
@Composable
fun JobSelectScreen(
    onConfirm: (PlayerJob) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selected by remember { mutableStateOf(PlayerJob.WARRIOR) }

    val row1 = listOf(PlayerJob.WARRIOR, PlayerJob.MAGE,  PlayerJob.ARCHER)
    val row2 = listOf(PlayerJob.THIEF,   PlayerJob.PIRATE, PlayerJob.BEGINNER)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelHeader)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, SlotBorder, RoundedCornerShape(4.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("<  뒤로", color = TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "직업을 선택하세요",
                color = TextGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(60.dp))
        }

        // 본문
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 왼쪽: 직업 카드 그리드
            Column(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row1.forEach { job ->
                        JobCard(
                            job        = job,
                            isSelected = job == selected,
                            modifier   = Modifier.weight(1f).fillMaxHeight()
                        ) { selected = job }
                    }
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row2.forEach { job ->
                        JobCard(
                            job        = job,
                            isSelected = job == selected,
                            modifier   = Modifier.weight(1f).fillMaxHeight()
                        ) { selected = job }
                    }
                }
            }

            // 오른쪽: 선택 직업 상세
            JobDetailPanel(
                job      = selected,
                modifier = Modifier.weight(0.56f).fillMaxHeight()
            )
        }

        // 하단 확인 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelHeader)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(selected.themeColor())
                    .border(1.dp, selected.accentColor(), RoundedCornerShape(6.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onConfirm(selected) }
                    .padding(horizontal = 60.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selected.startLabel(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun JobCard(
    job: PlayerJob,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) job.accentColor() else SlotBorder
    val bgColor     = if (isSelected) job.themeColor()  else Color(0xFF1A1008)
    val textColor   = if (isSelected) job.accentColor() else TextMuted

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text       = job.koreanName().first().toString(),
                color      = textColor,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = job.koreanName(),
                color    = if (isSelected) Color.White else TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun JobDetailPanel(job: PlayerJob, modifier: Modifier = Modifier) {
    val stats = job.initialStats()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 직업명 + 태그라인
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = job.koreanName(),
                color      = job.accentColor(),
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = job.tagline(),
                color    = TextMuted,
                fontSize = 12.sp
            )
        }

        HorizontalDivider(color = BorderGold.copy(alpha = 0.3f))

        // 초기 스탯
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text       = "초기 스탯",
                color      = TextGold,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier                = Modifier.fillMaxWidth(),
                horizontalArrangement   = Arrangement.SpaceEvenly
            ) {
                JobStatChip("STR", stats.str,      job)
                JobStatChip("DEX", stats.dex,      job)
                JobStatChip("INT", stats.`int`,    job)
                JobStatChip("LUK", stats.luk,      job)
            }
        }

        HorizontalDivider(color = BorderGold.copy(alpha = 0.3f))

        // 성장 방향
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text       = "성장 방향",
                color      = TextGold,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text       = job.jobDescription(),
                color      = TextMuted,
                fontSize   = 12.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun JobStatChip(label: String, value: Int, job: PlayerJob) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 10.sp)
        Text(
            text       = value.toString(),
            color      = TextGold,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── 전직 팝업 ─────────────────────────────────────────────────────────────────
@Composable
private fun JobAdvancementDialog(onAdvance: (PlayerJob) -> Unit) {
    var selected by remember { mutableStateOf(PlayerJob.WARRIOR) }

    val row1 = listOf(PlayerJob.WARRIOR, PlayerJob.MAGE,  PlayerJob.ARCHER)
    val row2 = listOf(PlayerJob.THIEF,   PlayerJob.PIRATE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBg)
                .border(2.dp, BorderGold, RoundedCornerShape(12.dp))
        ) {
            // 헤더
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelHeader)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text       = "전직",
                    color      = TextGold,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = "레벨 3에 도달했습니다. 직업을 선택하세요.",
                    color    = TextMuted,
                    fontSize = 12.sp
                )
            }

            // 본문
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 왼쪽: 5개 직업 카드
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row1.forEach { job ->
                            JobCard(
                                job        = job,
                                isSelected = job == selected,
                                modifier   = Modifier.weight(1f).fillMaxHeight()
                            ) { selected = job }
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row2.forEach { job ->
                            JobCard(
                                job        = job,
                                isSelected = job == selected,
                                modifier   = Modifier.weight(1f).fillMaxHeight()
                            ) { selected = job }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                // 오른쪽: 선택 직업 상세
                JobDetailPanel(
                    job      = selected,
                    modifier = Modifier.weight(0.56f).fillMaxHeight()
                )
            }

            // 하단 확인 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelHeader)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(selected.themeColor())
                        .border(1.dp, selected.accentColor(), RoundedCornerShape(6.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onAdvance(selected) }
                        .padding(horizontal = 48.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = selected.advanceLabel(),
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── NPC 렌더링 ────────────────────────────────────────────────────────────────
private fun DrawScope.drawNpc(npc: Npc, cam: CameraState, bitmap: ImageBitmap?) {
    val c = cam.toScreenOffset(npc.worldX, npc.worldY, size.width, size.height)
    val imgSize = (size.height * 0.10f).toInt().coerceAtLeast(32)
    // 이미지
    if (bitmap != null) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset((c.x - imgSize / 2).toInt(), (c.y - imgSize).toInt()),
            dstSize = IntSize(imgSize, imgSize),
            filterQuality = androidx.compose.ui.graphics.FilterQuality.High
        )
    } else {
        drawCircle(Color(0xFFFFCC44), radius = imgSize / 2f, center = Offset(c.x, c.y - imgSize / 2f))
    }
    // 이름 텍스트
    drawContext.canvas.nativeCanvas.drawText(
        npc.name,
        c.x,
        c.y - imgSize - 4f,
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = size.height * 0.018f
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
    )
}

// ── 대화창 ────────────────────────────────────────────────────────────────────
@Composable
private fun DialogueWindow(
    session: DialogueSession,
    onNext: () -> Unit,
    onChoose: (Int) -> Unit,
    onClose: () -> Unit
) {
    val page = session.currentPage
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .background(Color(0xEE1A1008), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF8B6914), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // 화자 이름
            Text(
                text = page.speaker,
                color = Color(0xFFFFDF7E),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 대화 텍스트
            Text(
                text = page.text,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            // 버튼
            if (page.choices.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (session.isLastPage) {
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B6914))
                        ) { Text("닫기", color = Color.White, fontSize = 12.sp) }
                    } else {
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B6914))
                        ) { Text("다음 ▶", color = Color.White, fontSize = 12.sp) }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    page.choices.forEachIndexed { i, choice ->
                        Button(
                            onClick = { onChoose(i) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (i == 0) Color(0xFF8B6914) else Color(0xFF3A2A14)
                            )
                        ) { Text(choice, color = Color.White, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

// ── 퀘스트 트래커 ─────────────────────────────────────────────────────────────
@Composable
private fun QuestTrackerPanel(questState: QuestState, modifier: Modifier = Modifier) {
    val isReady = questState.status == QuestStatus.READY_TO_COMPLETE
    Column(
        modifier = modifier
            .background(Color(0xCC1A1008), RoundedCornerShape(6.dp))
            .border(1.dp, if (isReady) Color(0xFFFFDF7E) else Color(0xFF4A3010), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = if (isReady) "★ 완료 가능" else "▶ 진행 중",
            color = if (isReady) Color(0xFFFFDF7E) else Color(0xFF9A7D52),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "슬라임 소탕 작전",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        val fraction = (questState.slimeKillCount.toFloat() / questState.slimeKillGoal).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF3A2A14))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(if (isReady) Color(0xFFFFDF7E) else Color(0xFFCC8800))
            )
        }
        Text(
            text = "슬라임 처치  ${questState.slimeKillCount} / ${questState.slimeKillGoal}",
            color = Color(0xFF9A7D52),
            fontSize = 10.sp
        )
    }
}
