package com.a_survivor.app.model

data class Npc(
    val id: Int,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val interactRange: Float = 14f
)

object NpcRegistry {
    fun npcsFor(mapType: MapType): List<Npc> = when (mapType) {
        MapType.TOWN -> listOf(Npc(id = 1, name = "츄츄", worldX = 450f, worldY = 260f))
        else -> emptyList()
    }
}
