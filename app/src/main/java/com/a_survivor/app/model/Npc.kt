package com.a_survivor.app.model

data class Npc(
    val id: Int,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val interactRange: Float = 14f,
    val role: NpcRole = NpcRole.QUEST,
    val imageResId: Int? = null  // null이면 npc_chuchu.png 폴백
)

object NpcRegistry {
    fun npcsFor(mapType: MapType): List<Npc> = when (mapType) {
        MapType.TOWN -> listOf(
            Npc(id = 1, name = "츄츄",  worldX = 450f, worldY = 260f, role = NpcRole.QUEST),
            Npc(id = 2, name = "브루스", worldX = 370f, worldY = 260f, role = NpcRole.EQUIPMENT_SHOP),
            Npc(id = 3, name = "피아",  worldX = 530f, worldY = 260f, role = NpcRole.CONSUMABLE_SHOP)
        )
        else -> emptyList()
    }
}
