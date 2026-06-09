package com.a_survivor.app.model

import android.util.Log

object DropRegistry {

    private const val TAG = "DropRegistry"
    // monsterId → 아이템 드랍 목록 (돈 제외)
    private val itemDrops = mutableMapOf<String, MutableList<DropEntry>>()

    internal fun load(rows: List<Map<String, String>>) {
        itemDrops.clear()
        for (row in rows) {
            try {
                val monsterId = row["monsterId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "monsterId 누락, 스킵: $row"); continue
                }
                val itemId    = row["itemId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "itemId 누락, 스킵: $row"); continue
                }
                val dropRate  = row["dropRate"]?.toFloatOrNull() ?: run {
                    Log.w(TAG, "dropRate 파싱 실패, 스킵: $row"); continue
                }

                val dropItem: DropItem? = resolveItem(itemId)
                if (dropItem == null) {
                    Log.w(TAG, "알 수 없는 itemId '$itemId', 스킵")
                    continue
                }
                itemDrops.getOrPut(monsterId) { mutableListOf() }
                    .add(DropEntry(dropItem, dropRate))
            } catch (e: Exception) {
                Log.w(TAG, "행 파싱 실패, 스킵: $row — ${e.message}")
            }
        }
        Log.d(TAG, "드랍 엔트리 ${itemDrops.values.sumOf { it.size }}개 로드")
    }

    /**
     * monsterId에 해당하는 전체 드랍 목록을 반환합니다.
     * 돈 드랍(100%)은 MonsterRegistry에서 금액 범위를 읽어 자동 포함됩니다.
     * monsterId가 비어있으면 mapType을 통해 자동 조회합니다.
     */
    fun dropEntriesFor(monsterId: String, mapType: MapType? = null): List<DropEntry> {
        val effectiveId = monsterId.ifBlank {
            mapType?.let { MonsterRegistry.configForMap(it)?.monsterId } ?: return emptyList()
        }
        val cfg = MonsterRegistry.get(effectiveId)
        val moneyEntry = cfg?.let {
            val amount = (it.moneyMin..it.moneyMax).random()
            DropEntry(DropItem.MoneyDrop(amount), 1.0f)
        }
        val entries = itemDrops[effectiveId] ?: emptyList()
        return listOfNotNull(moneyEntry) + entries
    }

    private fun resolveItem(itemId: String): DropItem? {
        // 장비 itemId → EquipmentRegistry 조회
        EquipmentRegistry.get(itemId)?.let { return DropItem.EquipmentDrop(it) }
        // 주문서 → ScrollType enum 이름과 매핑
        runCatching { ScrollType.valueOf(itemId) }.getOrNull()
            ?.let { return DropItem.ScrollDrop(it) }
        // 재료 아이템 → MaterialType enum 이름과 매핑
        runCatching { MaterialType.valueOf(itemId) }.getOrNull()
            ?.let { return DropItem.MaterialDrop(it) }
        return null
    }
}
