package com.a_survivor.app.model

import android.util.Log

data class QuestData(
    val questId: String,
    val name: String,
    val npcId: Int,
    val targetMonsterId: String,
    val targetCount: Int,
    val rewardExp: Int,
    val rewardMoney: Int,
    val rewardItemId: String
)

object QuestRegistry {

    private const val TAG = "QuestRegistry"
    private val catalog = mutableListOf<QuestData>()

    internal fun load(rows: List<Map<String, String>>) {
        catalog.clear()
        for (row in rows) {
            try {
                val questId = row["questId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "questId 누락, 스킵: $row"); continue
                }
                catalog.add(QuestData(
                    questId         = questId,
                    name            = row["name"] ?: "",
                    npcId           = row["npcId"]?.toIntOrNull() ?: 0,
                    targetMonsterId = row["targetMonsterId"] ?: "",
                    targetCount     = row["targetCount"]?.toIntOrNull() ?: 1,
                    rewardExp       = row["rewardExp"]?.toIntOrNull() ?: 0,
                    rewardMoney     = row["rewardMoney"]?.toIntOrNull() ?: 0,
                    rewardItemId    = row["rewardItemId"] ?: ""
                ))
            } catch (e: Exception) {
                Log.w(TAG, "행 파싱 실패, 스킵: $row — ${e.message}")
            }
        }
        Log.d(TAG, "퀘스트 ${catalog.size}개 로드")
    }

    fun get(questId: String): QuestData? = catalog.find { it.questId == questId }
    fun questForNpc(npcId: Int): QuestData? = catalog.find { it.npcId == npcId }
    fun all(): List<QuestData> = catalog.toList()
}
