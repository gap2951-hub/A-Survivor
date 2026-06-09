package com.a_survivor.app.model

import android.util.Log

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

    private const val TAG = "NpcRegistry"
    private val npcsByMap = mutableMapOf<String, MutableList<Npc>>()

    internal fun load(rows: List<Map<String, String>>) {
        npcsByMap.clear()
        for (row in rows) {
            try {
                val npcId = row["npcId"]?.toIntOrNull() ?: run {
                    Log.w(TAG, "npcId 파싱 실패, 스킵: $row"); continue
                }
                val mapId = row["mapId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "mapId 누락, 스킵: $row"); continue
                }
                val role = runCatching { NpcRole.valueOf(row["role"] ?: "") }.getOrNull() ?: run {
                    Log.w(TAG, "role 파싱 실패, 스킵: $row"); continue
                }
                val npc = Npc(
                    id    = npcId,
                    name  = row["name"] ?: "",
                    worldX = row["x"]?.toFloatOrNull() ?: 0f,
                    worldY = row["y"]?.toFloatOrNull() ?: 0f,
                    role  = role
                )
                npcsByMap.getOrPut(mapId) { mutableListOf() }.add(npc)
            } catch (e: Exception) {
                Log.w(TAG, "행 파싱 실패, 스킵: $row — ${e.message}")
            }
        }
        Log.d(TAG, "NPC ${npcsByMap.values.sumOf { it.size }}개 로드")
    }

    fun npcsFor(mapType: MapType): List<Npc> = npcsByMap[mapType.name] ?: emptyList()
}
