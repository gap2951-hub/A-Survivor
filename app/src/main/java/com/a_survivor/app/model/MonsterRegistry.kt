package com.a_survivor.app.model

import android.util.Log

data class MonsterConfig(
    val monsterId: String,
    val name: String,
    val mapId: String,
    val variant: Int,
    val level: Int,
    val hp: Int,
    val exp: Int,
    val avoidability: Int,
    val accuracy: Int,
    val speed: Float,
    val count: Int,
    val moneyMin: Int,
    val moneyMax: Int
)

object MonsterRegistry {

    private const val TAG = "MonsterRegistry"
    private val byId  = mutableMapOf<String, MonsterConfig>()
    private val byMap = mutableMapOf<String, MonsterConfig>()

    internal fun load(rows: List<Map<String, String>>) {
        byId.clear()
        byMap.clear()
        for (row in rows) {
            try {
                val monsterId = row["monsterId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "monsterId 누락, 스킵: $row"); continue
                }
                val cfg = MonsterConfig(
                    monsterId    = monsterId,
                    name         = row["name"] ?: "몬스터",
                    mapId        = row["mapId"] ?: "",
                    variant      = row["variant"]?.toIntOrNull() ?: 1,
                    level        = row["level"]?.toIntOrNull() ?: 1,
                    hp           = row["hp"]?.toIntOrNull() ?: 10,
                    exp          = row["exp"]?.toIntOrNull() ?: 1,
                    avoidability = row["avoidability"]?.toIntOrNull() ?: 5,
                    accuracy     = row["accuracy"]?.toIntOrNull() ?: 15,
                    speed        = row["speed"]?.toFloatOrNull() ?: 1.0f,
                    count        = row["count"]?.toIntOrNull() ?: 5,
                    moneyMin     = row["moneyMin"]?.toIntOrNull() ?: 0,
                    moneyMax     = row["moneyMax"]?.toIntOrNull() ?: 0
                )
                byId[monsterId] = cfg
                if (cfg.mapId.isNotBlank()) byMap[cfg.mapId] = cfg
            } catch (e: Exception) {
                Log.w(TAG, "행 파싱 실패, 스킵: $row — ${e.message}")
            }
        }
        Log.d(TAG, "몬스터 ${byId.size}개 로드")
    }

    fun get(monsterId: String): MonsterConfig? = byId[monsterId]
    fun configForMap(mapType: MapType): MonsterConfig? = byMap[mapType.name]
    fun all(): Collection<MonsterConfig> = byId.values
}
