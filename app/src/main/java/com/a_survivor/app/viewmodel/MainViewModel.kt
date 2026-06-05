package com.a_survivor.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a_survivor.app.model.DefaultWeapon
import com.a_survivor.app.model.DefaultWorld
import com.a_survivor.app.model.DropItem
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.model.StatType
import com.a_survivor.app.model.SlimeDropTable
import com.a_survivor.app.model.Weapon
import com.a_survivor.app.service.AutoAttackService
import com.a_survivor.app.service.DropService
import com.a_survivor.app.service.EnhancementService
import com.a_survivor.app.service.LevelService
import com.a_survivor.app.service.MonsterSpawner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryItem(val scrollType: ScrollType, val quantity: Int)

data class UiState(
    val equipment: Equipment?,
    val weapon: Weapon?,
    val inventory: List<InventoryItem>,
    val selectedScrollType: ScrollType? = null,
    val lastResult: EnhancementResult? = null,
    val player: Player = Player(),
    val world: GameWorld = DefaultWorld,
    val monsters: List<Monster> = emptyList()
)

class MainViewModel : ViewModel() {

    private val service           = EnhancementService()
    private val autoAttackService = AutoAttackService()
    private val levelService      = LevelService()
    private val dropService       = DropService()

    companion object {
        private const val MOVE_SPEED          = 3f
        private const val AUTO_ATTACK_INTERVAL = 1000L  // ms
    }

    init {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_ATTACK_INTERVAL)
                autoAttackTick()
            }
        }
    }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun createInitialState() = UiState(
        monsters = MonsterSpawner().spawnSlimes(world = DefaultWorld, count = 5),
        weapon = DefaultWeapon,
        equipment = Equipment(
            name = "노가다 목장갑",
            attackPower = 0,
            maxUpgradeCount = 5,
            remainingUpgradeCount = 5,
            failedUpgradeCount = 0,
            destroyed = false,
            description = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
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
                equipment = Equipment(
                    name = "노가다 목장갑",
                    attackPower = 0,
                    maxUpgradeCount = 5,
                    remainingUpgradeCount = 5,
                    failedUpgradeCount = 0,
                    destroyed = false,
                    description = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
                ),
                selectedScrollType = null,
                lastResult = null
            )
        }
    }

    private fun autoAttackTick() {
        _uiState.update { state ->
            val result = autoAttackService.tick(
                player    = state.player,
                equipment = state.equipment,
                monsters  = state.monsters
            )

            // 경험치 적용
            val gainedExp = result.killedMonsters.sumOf { it.expReward }
            val updatedPlayer = if (gainedExp > 0)
                levelService.applyExp(state.player, gainedExp)
            else
                state.player

            // 드랍 처리
            val drops = result.killedMonsters.flatMap {
                dropService.roll(SlimeDropTable.entries)
            }

            var newState = state.copy(
                monsters = result.updatedMonsters,
                player   = updatedPlayer
            )
            newState = applyDrops(newState, drops)
            newState
        }
    }

    private fun applyDrops(state: UiState, drops: List<DropItem>): UiState {
        var s = state
        for (drop in drops) {
            s = when (drop) {
                is DropItem.ScrollDrop -> s.copy(
                    inventory = s.inventory.map { item ->
                        if (item.scrollType == drop.scrollType)
                            item.copy(quantity = item.quantity + 1)
                        else item
                    }
                )
                is DropItem.EquipmentDrop -> {
                    // 장비 슬롯이 비어 있을 때만 장착
                    if (s.equipment == null) s.copy(equipment = drop.equipment) else s
                }
            }
        }
        return s
    }

    fun allocateStat(type: StatType) {
        _uiState.update { state ->
            val player = state.player
            if (player.availableStatPoint <= 0) return@update state
            val newStats = when (type) {
                StatType.STR -> player.stats.copy(str = player.stats.str + 1)
                StatType.DEX -> player.stats.copy(dex = player.stats.dex + 1)
                StatType.INT -> player.stats.copy(`int` = player.stats.`int` + 1)
                StatType.LUK -> player.stats.copy(luk = player.stats.luk + 1)
            }
            state.copy(
                player = player.copy(
                    stats              = newStats,
                    availableStatPoint = player.availableStatPoint - 1
                )
            )
        }
    }

    fun movePlayer(dirX: Float, dirY: Float) {
        _uiState.update { state ->
            val p = state.player
            val rawX = p.positionX + dirX * MOVE_SPEED
            val rawY = p.positionY + dirY * MOVE_SPEED
            val (clampedX, clampedY) = state.world.clampPosition(rawX, rawY)
            state.copy(player = p.copy(positionX = clampedX, positionY = clampedY))
        }
    }

    fun unequipWeapon() {
        _uiState.update { it.copy(weapon = null) }
    }

    fun resetWeapon() {
        _uiState.update { it.copy(weapon = DefaultWeapon) }
    }
}
