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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.a_survivor.app.model.CameraState
import com.a_survivor.app.model.DamageNumber
import com.a_survivor.app.model.DropItem
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.GroundItem
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.MonsterState
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.model.StatType
import com.a_survivor.app.model.Weapon
import com.a_survivor.app.service.AutoAttackService
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

// ── Activity ──────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
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
            MainScreen(
                state,
                vm::selectScroll,
                vm::useSelectedScroll,
                vm::unequipEquipment,
                vm::resetEquipment,
                vm::unequipWeapon,
                vm::resetWeapon,
                vm::movePlayer,
                vm::allocateStat
            )
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
    onAllocateStat: (StatType) -> Unit
) {
    val dragState        = remember { DragDropState() }
    var isEquipmentOpen  by remember { mutableStateOf(false) }
    var isInventoryOpen  by remember { mutableStateOf(false) }
    var isStatOpen       by remember { mutableStateOf(false) }
    var equipSlotBounds  by remember { mutableStateOf<Rect?>(null) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }

    var joystickDirX by remember { mutableStateOf(0f) }
    var joystickDirY by remember { mutableStateOf(0f) }

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
            damageNumbers = state.damageNumbers
        )

        // ② 상단 HUD
        GameHud(
            player = state.player,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // ③ 강화 결과 메시지
        state.lastResult?.let {
            ResultPanel(
                result = it,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            )
        }


        // ④ 장비창 오버레이
        if (isEquipmentOpen) {
            PanelOverlay(onDismiss = { isEquipmentOpen = false }) {
                EquipmentWindow(
                    equipment = state.equipment,
                    weapon = state.weapon,
                    isDragOver = isDragOver,
                    onSlotBounds = { equipSlotBounds = it },
                    onUnequip = onUnequip,
                    onReset = onReset,
                    onUnequipWeapon = onUnequipWeapon,
                    onResetWeapon = onResetWeapon,
                    onClose = { isEquipmentOpen = false }
                )
            }
        }

        // ⑤ 스탯창 오버레이
        if (isStatOpen) {
            PanelOverlay(onDismiss = { isStatOpen = false }) {
                StatWindow(
                    player    = state.player,
                    onAllocate = onAllocateStat,
                    onClose   = { isStatOpen = false }
                )
            }
        }

        // ⑥ 인벤토리 오버레이
        if (isInventoryOpen) {
            PanelOverlay(onDismiss = { isInventoryOpen = false }) {
                InventoryWindow(
                    inventory = state.inventory,
                    dragState = dragState,
                    rootWindowOffset = rootWindowOffset,
                    onClose = { isInventoryOpen = false },
                    onDragEnd = { scrollType, dropPos ->
                        if (isEquipmentOpen &&
                            equipSlotBounds?.contains(dropPos) == true &&
                            state.equipment != null && !state.equipment.destroyed) {
                            onScrollSelected(scrollType)
                            onEnhance()
                        }
                    }
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
    onClose: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("장비창", onClose = onClose)

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

private val SlotSize = 48.dp

@Composable
private fun BodyRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun WindowTitleBar(title: String, onClose: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelHeader)
            .padding(horizontal = 12.dp, vertical = 7.dp),
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapBitmap       = remember { loadBitmap(context, R.drawable.map_beginner, 2048) }
    val slimeBitmap     = remember { loadBitmap(context, R.drawable.slime, 256) }
    val scroll100Bitmap = remember { loadBitmap(context, R.drawable.scroll_100, 256) }
    val scroll60Bitmap  = remember { loadBitmap(context, R.drawable.scroll_60,  256) }
    val scroll10Bitmap  = remember { loadBitmap(context, R.drawable.scroll_10,  256) }
    val gloveBitmap     = remember { loadBitmap(context, R.drawable.nogada_glove, 256) }

    Canvas(modifier = modifier.fillMaxSize()) {
        // 화면에 맵이 꽉 차도록 줌 자동 계산 (빈 공간 없음)
        val zoom = maxOf(size.width / world.width, size.height / world.height)
        val cam = CameraState(zoom = zoom)
            .followPlayer(player.positionX, player.positionY)
            .clampToWorld(world, size.width, size.height)

        drawWorldBackground(cam, world, mapBitmap)
        groundItems.forEach { drawGroundItem(it, cam, scroll100Bitmap, scroll60Bitmap, scroll10Bitmap, gloveBitmap) }
        drawAttackRange(player, cam)
        monsters.forEach { drawMonster(it, cam, slimeBitmap) }
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
    val iconSize = (52f * cam.zoom).toInt().coerceAtLeast(12)

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
        textSize    = 14f * cam.zoom
        textAlign   = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(label, pos.x, pos.y + 18f * cam.zoom, labelPaint)
}

private fun DrawScope.drawAttackRange(player: Player, cam: CameraState) {
    val center = cam.toScreenOffset(player.positionX, player.positionY, size.width, size.height)
    val r = AutoAttackService.ATTACK_RANGE * cam.zoom
    drawCircle(Color.White.copy(alpha = 0.05f), radius = r, center = center)
    drawCircle(Color.White.copy(alpha = 0.12f), radius = r, center = center,
        style = Stroke(width = 1f))
}

private fun DrawScope.drawPlayer(player: Player, cam: CameraState) {
    val c = cam.toScreenOffset(player.positionX, player.positionY, size.width, size.height)
    val r = 25f * cam.zoom

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
    val imgSize = (96f * cam.zoom).toInt().coerceAtLeast(20)

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
    val barH = 4f * cam.zoom
    val barX = c.x - barW / 2f
    val barY = c.y - imgSize * 0.6f - 6f * cam.zoom
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
            textSize    = 16f * cam.zoom
            textAlign   = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
        drawContext.canvas.nativeCanvas.drawText(
            "!",
            c.x,
            barY - 4f * cam.zoom,
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
        color          = if (num.isPlayerDamage)
                             android.graphics.Color.parseColor("#FF4444")
                         else
                             android.graphics.Color.parseColor("#FFEE00")
        textSize       = (if (num.isPlayerDamage) 20f else 17f) * cam.zoom
        textAlign      = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias    = true
        this.alpha     = (alpha * 255).toInt()
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(num.value.toString(), pos.x, pos.y, paint)
}

// ── 상단 HUD ─────────────────────────────────────────────────────────────────
@Composable
private fun GameHud(player: Player, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Lv.${player.level}  ${player.job.koreanName()}",
            color = TextGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        HpBar(current = player.hp, max = player.maxHp)
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
    onAllocate: (StatType) -> Unit,
    onClose: (() -> Unit)? = null
) {
    val hasPoints = player.availableStatPoint > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("스탯창", onClose = onClose)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

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
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // 스탯 행
            StatAllocRow("STR", player.stats.str, hasPoints) { onAllocate(StatType.STR) }
            StatAllocRow("DEX", player.stats.dex, hasPoints) { onAllocate(StatType.DEX) }
            StatAllocRow("INT", player.stats.`int`, hasPoints) { onAllocate(StatType.INT) }
            StatAllocRow("LUK", player.stats.luk, hasPoints) { onAllocate(StatType.LUK) }
        }
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
    onClose: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("인벤토리 - 주문서", onClose = onClose)

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
