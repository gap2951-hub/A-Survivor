package com.a_survivor.app.model

import android.util.Log

object EquipmentRegistry {

    private const val TAG = "EquipmentRegistry"
    private val catalog = mutableMapOf<String, Equipment>()

    internal fun load(rows: List<Map<String, String>>) {
        catalog.clear()
        for (row in rows) {
            try {
                val itemId = row["itemId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "itemId 누락, 스킵: $row"); continue
                }
                val maxUpgrade = row["maxUpgradeCount"]?.toIntOrNull() ?: 0
                val equip = Equipment(
                    itemId                = itemId,
                    name                  = row["name"] ?: "",
                    slot                  = row["slot"] ?: "GLOVE",
                    requiredLevel         = row["requiredLevel"]?.toIntOrNull() ?: 1,
                    attackPower           = row["attackPower"]?.toIntOrNull() ?: 0,
                    maxUpgradeCount       = maxUpgrade,
                    remainingUpgradeCount = maxUpgrade,
                    failedUpgradeCount    = 0,
                    destroyed             = false,
                    strBonus              = row["strBonus"]?.toIntOrNull() ?: 0,
                    dexBonus              = row["dexBonus"]?.toIntOrNull() ?: 0,
                    intBonus              = row["intBonus"]?.toIntOrNull() ?: 0,
                    lukBonus              = row["lukBonus"]?.toIntOrNull() ?: 0,
                    physicalDefense       = row["physicalDefense"]?.toIntOrNull() ?: 0,
                    magicDefense          = row["magicDefense"]?.toIntOrNull() ?: 0,
                    accuracy              = row["accuracy"]?.toIntOrNull() ?: 0,
                    avoidability          = row["avoidability"]?.toIntOrNull() ?: 0,
                    buyPrice              = row["buyPrice"]?.toIntOrNull() ?: 0,
                    sellPrice             = row["sellPrice"]?.toIntOrNull() ?: 0,
                    description           = row["description"]?.replace("\\n", "\n") ?: ""
                )
                catalog[itemId] = equip
            } catch (e: Exception) {
                Log.w(TAG, "행 파싱 실패, 스킵: $row — ${e.message}")
            }
        }
        Log.d(TAG, "장비 ${catalog.size}개 로드")
    }

    fun get(itemId: String): Equipment? = catalog[itemId]
    fun all(): Collection<Equipment> = catalog.values
}
