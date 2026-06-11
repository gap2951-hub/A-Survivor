package com.a_survivor.app.service

import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Weapon

data class GameSaveData(
    val playerLevel: Int = 1,
    val playerExp: Int = 0,
    val playerHp: Int = 100,
    val playerMaxHp: Int = 100,
    val playerJob: String = "BEGINNER",
    val playerStr: Int = 10,
    val playerDex: Int = 10,
    val playerInt: Int = 10,
    val playerLuk: Int = 10,
    val playerStatPoints: Int = 0,
    val playerPosX: Float = 512f,
    val playerPosY: Float = 286f,
    val money: Int = 0,
    val mapType: String = "BEGINNER_FIELD",
    val questStatus: String = "NOT_STARTED",
    val questKillCount: Int = 0,
    val questKillGoal: Int = 5,
    val equipment: Equipment? = null,
    val hat: Equipment? = null,
    val top: Equipment? = null,
    val shoes: Equipment? = null,
    val weapon: Weapon? = null,
    val inventory: List<SavedSlot> = emptyList()
)

data class SavedSlot(
    val slotType: String = "EMPTY",
    val scrollType: String? = null,
    val scrollQty: Int = 0,
    val equipment: Equipment? = null,
    val consumableType: String? = null,
    val consumableQty: Int = 0,
    val materialType: String? = null,
    val materialQty: Int = 0
)
