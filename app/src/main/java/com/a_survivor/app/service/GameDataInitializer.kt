package com.a_survivor.app.service

import android.content.Context
import android.util.Log
import com.a_survivor.app.model.DropRegistry
import com.a_survivor.app.model.EquipmentRegistry
import com.a_survivor.app.model.MonsterRegistry
import com.a_survivor.app.model.NpcRegistry
import com.a_survivor.app.model.QuestRegistry
import com.a_survivor.app.model.ShopRegistry

object GameDataInitializer {

    private const val TAG = "GameDataInitializer"
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return

        // 로드 순서 중요: DropRegistry·ShopRegistry 는 EquipmentRegistry 의존
        val equipRows   = DataLoader.loadCsv(context, "equipment.csv")
        EquipmentRegistry.load(equipRows)

        val monsterRows = DataLoader.loadCsv(context, "monster.csv")
        MonsterRegistry.load(monsterRows)

        val dropRows    = DataLoader.loadCsv(context, "drop.csv")
        DropRegistry.load(dropRows)

        val npcRows     = DataLoader.loadCsv(context, "npc.csv")
        NpcRegistry.load(npcRows)

        val shopRows    = DataLoader.loadCsv(context, "shop.csv")
        ShopRegistry.load(shopRows)

        val questRows   = DataLoader.loadCsv(context, "quest.csv")
        QuestRegistry.load(questRows)

        initialized = true
        Log.i(TAG, "게임 데이터 초기화 완료")
    }

    /** 에디터 저장 후 호출 — 전체 데이터를 다시 로드 */
    fun reload(context: Context) {
        initialized = false
        initialize(context)
    }
}
