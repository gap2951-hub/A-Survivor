package com.a_survivor.app.viewmodel

import androidx.lifecycle.ViewModel
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.service.EnhancementService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InventoryItem(val scrollType: ScrollType, val quantity: Int)

data class UiState(
    val equipment: Equipment?,
    val inventory: List<InventoryItem>,
    val selectedScrollType: ScrollType? = null,
    val lastResult: EnhancementResult? = null
)

class MainViewModel : ViewModel() {

    private val service = EnhancementService()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun createInitialState() = UiState(
        equipment = Equipment(
            name = "노가다 목장갑",
            attackPower = 0,
            maxUpgradeCount = 5,
            remainingUpgradeCount = 5,
            failedUpgradeCount = 0,
            destroyed = false
        ),
        inventory = listOf(
            InventoryItem(ScrollType.GLOVE_ATK_100, 10),
            InventoryItem(ScrollType.GLOVE_ATK_60, 10),
            InventoryItem(ScrollType.GLOVE_ATK_10, 10),
            InventoryItem(ScrollType.WHITE_SCROLL_1, 10),
            InventoryItem(ScrollType.WHITE_SCROLL_3, 10)
        )
    )

    fun selectScroll(scrollType: ScrollType) {
        _uiState.update { it.copy(selectedScrollType = scrollType, lastResult = null) }
    }

    fun useSelectedScroll() {
        val state = _uiState.value
        val equipment = state.equipment ?: run {
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("장비가 장착되어 있지 않습니다.")) }
            return
        }
        val scrollType = state.selectedScrollType ?: return
        val scroll = ScrollCatalog.get(scrollType)

        val item = state.inventory.find { it.scrollType == scrollType }
        if (item == null || item.quantity <= 0) {
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("주문서 수량이 부족합니다.")) }
            return
        }

        val (newEquipment, result) = service.applyScroll(equipment, scroll)
        val newInventory = state.inventory.map {
            if (it.scrollType == scrollType) it.copy(quantity = it.quantity - 1) else it
        }

        _uiState.update {
            it.copy(
                equipment = newEquipment,
                inventory = newInventory,
                lastResult = result
            )
        }
    }

    fun unequipEquipment() {
        _uiState.update { it.copy(equipment = null, selectedScrollType = null, lastResult = null) }
    }

    fun resetEquipment() {
        _uiState.update {
            it.copy(
                equipment = Equipment("노가다 목장갑", 0, 5, 5, 0, false),
                selectedScrollType = null,
                lastResult = null
            )
        }
    }
}
