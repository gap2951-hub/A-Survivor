package com.a_survivor.app.service

import android.content.Context
import com.a_survivor.app.model.ConsumableType
import com.a_survivor.app.model.MaterialType
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.viewmodel.InventorySlot
import com.a_survivor.app.viewmodel.UiState
import com.google.gson.Gson

class SaveService(context: Context) {

    private val prefs = context.getSharedPreferences("game_save", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(state: UiState) {
        val data = GameSaveData(
            playerLevel      = state.player.level,
            playerExp        = state.player.exp,
            playerHp         = state.player.hp,
            playerMaxHp      = state.player.maxHp,
            playerJob        = state.player.job.name,
            playerStr        = state.player.stats.str,
            playerDex        = state.player.stats.dex,
            playerInt        = state.player.stats.`int`,
            playerLuk        = state.player.stats.luk,
            playerStatPoints = state.player.availableStatPoint,
            playerPosX       = state.player.positionX,
            playerPosY       = state.player.positionY,
            money            = state.money,
            mapType          = state.world.mapType.name,
            questStatus      = state.questState.status.name,
            questKillCount   = state.questState.killCount,
            questKillGoal    = state.questState.killGoal,
            equipment        = state.equipment,
            hat              = state.hat,
            top              = state.top,
            shoes            = state.shoes,
            pants            = state.pants,
            weapon                   = state.weapon,
            tutorialStep             = state.questState.tutorialStep.name,
            tutorialTravelDistance   = state.questState.tutorialTravelDistance,
            mainQuestId              = state.questState.mainQuestId,
            mainQuestProgress        = state.questState.mainQuestProgress,
            mainQuestStatus          = state.questState.mainQuestStatus.name,
            inventory        = state.inventorySlots.map { slot ->
                when (slot) {
                    null -> SavedSlot("EMPTY")
                    is InventorySlot.ScrollItem     -> SavedSlot(
                        slotType  = "SCROLL",
                        scrollType = slot.type.name,
                        scrollQty  = slot.quantity
                    )
                    is InventorySlot.EquipItem      -> SavedSlot(
                        slotType  = "EQUIP",
                        equipment = slot.equipment
                    )
                    is InventorySlot.ConsumableItem -> SavedSlot(
                        slotType      = "CONSUMABLE",
                        consumableType = slot.type.name,
                        consumableQty  = slot.quantity
                    )
                    is InventorySlot.MaterialItem -> SavedSlot(
                        slotType    = "MATERIAL",
                        materialType = slot.type.name,
                        materialQty  = slot.quantity
                    )
                }
            }
        )
        prefs.edit().putString("save", gson.toJson(data)).apply()
    }

    fun load(): GameSaveData? {
        val json = prefs.getString("save", null) ?: return null
        return try {
            gson.fromJson(json, GameSaveData::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun hasSave(): Boolean = prefs.contains("save")

    fun deleteSave() = prefs.edit().remove("save").apply()

    fun toInventorySlot(slot: SavedSlot): InventorySlot? = when (slot.slotType) {
        "SCROLL" -> runCatching {
            InventorySlot.ScrollItem(ScrollType.valueOf(slot.scrollType!!), slot.scrollQty)
        }.getOrNull()
        "EQUIP" -> slot.equipment?.let { InventorySlot.EquipItem(it) }
        "CONSUMABLE" -> runCatching {
            InventorySlot.ConsumableItem(ConsumableType.valueOf(slot.consumableType!!), slot.consumableQty)
        }.getOrNull()
        "MATERIAL" -> runCatching {
            InventorySlot.MaterialItem(MaterialType.valueOf(slot.materialType!!), slot.materialQty)
        }.getOrNull()
        else -> null
    }
}
