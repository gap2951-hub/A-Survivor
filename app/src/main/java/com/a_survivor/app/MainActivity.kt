package com.a_survivor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
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
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
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
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.uiState.collectAsState()
            MainScreen(
                state,
                vm::selectScroll,
                vm::useSelectedScroll,
                vm::unequipEquipment,
                vm::resetEquipment
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
    onReset: () -> Unit
) {
    val dragState         = remember { DragDropState() }
    var isInventoryOpen   by remember { mutableStateOf(false) }
    var equipSlotBounds   by remember { mutableStateOf<Rect?>(null) }
    var rootWindowOffset  by remember { mutableStateOf(Offset.Zero) }

    val isDragOver = dragState.isDragging &&
            state.equipment != null &&
            !state.equipment.destroyed &&
            equipSlotBounds?.contains(dragState.position) == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .onGloballyPositioned { rootWindowOffset = it.positionInWindow() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "메이플 강화 시뮬레이터",
                color = TextGold, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )

            EquipmentWindow(
                equipment = state.equipment,
                isDragOver = isDragOver,
                onSlotBounds = { equipSlotBounds = it },
                onUnequip = onUnequip,
                onReset = onReset
            )

            state.lastResult?.let { ResultPanel(it) }

            Button(
                onClick = { isInventoryOpen = !isInventoryOpen },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1F00))
            ) {
                Text(
                    text = if (isInventoryOpen) "인벤토리 닫기  ▲" else "인벤토리 열기  ▼",
                    color = TextGold, fontWeight = FontWeight.Bold
                )
            }

            if (isInventoryOpen) {
                InventoryWindow(
                    inventory = state.inventory,
                    dragState = dragState,
                    rootWindowOffset = rootWindowOffset,
                    onDragEnd = { scrollType, dropPos ->
                        if (equipSlotBounds?.contains(dropPos) == true &&
                            state.equipment != null && !state.equipment.destroyed) {
                            onScrollSelected(scrollType)
                            onEnhance()
                        }
                    }
                )
            }

            Spacer(Modifier.height(100.dp))
        }

        // 드래그 중인 아이템 고스트
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
    isDragOver: Boolean,
    onSlotBounds: (Rect) -> Unit,
    onUnequip: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("장비창")

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ① 머리 — 모자
            BodyRow { EmptySlot("모자", Modifier.size(SlotSize)) }

            // ② 얼굴 — 얼굴장식 / 눈장식 / 귀걸이
            BodyRow {
                EmptySlot("얼굴", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                EmptySlot("눈장식", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                EmptySlot("귀걸이", Modifier.size(SlotSize))
            }

            // ③ 목 — 목걸이
            BodyRow { EmptySlot("목걸이", Modifier.size(SlotSize)) }

            // ④ 어깨·상의·망토
            BodyRow {
                EmptySlot("어깨", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                EmptySlot("상의", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                EmptySlot("망토", Modifier.size(SlotSize))
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
                EmptySlot("하의", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                EmptySlot("무기", Modifier.size(SlotSize))
            }

            // ⑥ 신발·벨트
            BodyRow {
                EmptySlot("신발", Modifier.size(SlotSize))
                Spacer(Modifier.width(6.dp))
                EmptySlot("벨트", Modifier.size(SlotSize))
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
private fun WindowTitleBar(title: String) {
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
        Text(title, color = TextGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

// ── 아이템 정보 다이얼로그 ────────────────────────────────────────────────────
@Composable
private fun ItemInfoDialog(equipment: Equipment, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PanelBg)
                .border(1.dp, BorderGold, RoundedCornerShape(10.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 이름 + 이미지
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (equipment.destroyed) Color(0xFF3E0010) else Color(0xFF0A1200))
                        .border(1.dp, if (equipment.destroyed) ColorDestroyed else Color(0xFF446000), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (equipment.destroyed) {
                        Text("✕", color = ColorDestroyed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.nogada_glove),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = equipment.name,
                        color = if (equipment.destroyed) ColorDestroyed else TextGold,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (equipment.destroyed) "파괴됨" else "장착 중",
                        color = if (equipment.destroyed) ColorDestroyed else ColorSuccess,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = BorderGold.copy(alpha = 0.4f))

            // 스탯
            InfoStatRow("공격력",       "+${equipment.attackPower}",       DotWhite)
            InfoStatRow(
                "남은 강화 횟수",
                "${equipment.remainingUpgradeCount} / ${equipment.maxUpgradeCount}",
                TextGold
            )
            InfoStatRow(
                "실패 횟수",
                "${equipment.failedUpgradeCount}",
                if (equipment.failedUpgradeCount > 0) ColorFailure else TextGold
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1800))
            ) {
                Text("닫기", color = TextGold)
            }
        }
    }
}

@Composable
private fun InfoStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
fun ResultPanel(result: EnhancementResult) {
    val (bg, fg) = when (result) {
        is EnhancementResult.Success   -> Color(0xFF1B5E20) to ColorSuccess
        is EnhancementResult.Failure   -> Color(0xFF4E1210) to ColorFailure
        is EnhancementResult.Destroyed -> Color(0xFF3E0030) to ColorDestroyed
        is EnhancementResult.Error     -> Color(0xFF3E2400) to ColorError
    }
    Box(
        modifier = Modifier
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

// ── 인벤토리창 ────────────────────────────────────────────────────────────────
@Composable
fun InventoryWindow(
    inventory: List<InventoryItem>,
    dragState: DragDropState,
    rootWindowOffset: Offset,
    onDragEnd: (ScrollType, Offset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .border(1.dp, BorderGold, RoundedCornerShape(8.dp))
    ) {
        WindowTitleBar("인벤토리 - 주문서")

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
