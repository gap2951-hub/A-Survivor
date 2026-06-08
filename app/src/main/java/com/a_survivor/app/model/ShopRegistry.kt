package com.a_survivor.app.model

import android.util.Log

object ShopRegistry {

    private const val TAG = "ShopRegistry"
    private val shopItems = mutableMapOf<ShopType, MutableList<ShopItem>>()
    private var nextId = 1

    internal fun load(rows: List<Map<String, String>>) {
        shopItems.clear()
        nextId = 1
        for (row in rows) {
            try {
                val shopType = runCatching { ShopType.valueOf(row["shopType"] ?: "") }.getOrNull() ?: run {
                    Log.w(TAG, "shopType 파싱 실패, 스킵: $row"); continue
                }
                val itemId   = row["itemId"]?.takeIf { it.isNotBlank() } ?: run {
                    Log.w(TAG, "itemId 누락, 스킵: $row"); continue
                }
                val buyPrice  = row["buyPrice"]?.toIntOrNull() ?: 0
                val sellPrice = row["sellPrice"]?.toIntOrNull() ?: 0

                val (name, desc, itemType, stackable) = resolveItemMeta(shopType, itemId) ?: run {
                    Log.w(TAG, "알 수 없는 itemId '$itemId', 스킵"); continue
                }

                shopItems.getOrPut(shopType) { mutableListOf() }.add(
                    ShopItem(
                        id        = nextId++,
                        name      = name,
                        description = desc,
                        buyPrice  = buyPrice,
                        sellPrice = sellPrice,
                        itemType  = itemType,
                        itemId    = itemId,
                        stackable = stackable
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "행 파싱 실패, 스킵: $row — ${e.message}")
            }
        }
        Log.d(TAG, "상점 아이템 ${shopItems.values.sumOf { it.size }}개 로드")
    }

    private data class ItemMeta(val name: String, val desc: String, val itemType: ShopItemType, val stackable: Boolean)

    private fun resolveItemMeta(shopType: ShopType, itemId: String): ItemMeta? {
        return when (shopType) {
            ShopType.EQUIPMENT -> {
                val equip = EquipmentRegistry.get(itemId) ?: return null
                ItemMeta(equip.name, equip.description, ShopItemType.EQUIPMENT, false)
            }
            ShopType.CONSUMABLE -> {
                // 소비 아이템: 포션 or 주문서
                val scroll = runCatching { ScrollType.valueOf(itemId) }.getOrNull()
                if (scroll != null) {
                    val s = ScrollCatalog.get(scroll)
                    ItemMeta(s.name, scrollDesc(s), ShopItemType.SCROLL, true)
                } else {
                    val consumable = ConsumableCatalog.fromItemId(itemId) ?: return null
                    val info = ConsumableCatalog.get(consumable)
                    ItemMeta(info.name, info.description, ShopItemType.CONSUMABLE, true)
                }
            }
        }
    }

    private fun scrollDesc(s: Scroll): String = buildString {
        if (s.isWhiteScroll) {
            append("업그레이드 실패 횟수 복구.\n성공률 ${s.successRate}%")
        } else {
            val slotKo = when (s.targetSlot) {
                "GLOVE" -> "장갑"; "TOP" -> "상의"; "HAT" -> "모자"
                "SHOES" -> "신발"; "WEAPON" -> "무기"; else -> s.targetSlot
            }
            if (slotKo.isNotBlank()) append("[$slotKo 전용] ")
            val effects = buildList {
                if (s.attackBonus > 0) add("공격력 +${s.attackBonus}")
                if (s.magicBonus  > 0) add("마력 +${s.magicBonus}")
                if (s.strBonus    > 0) add("힘 +${s.strBonus}")
                if (s.dexBonus    > 0) add("민첩 +${s.dexBonus}")
                if (s.intBonus    > 0) add("지력 +${s.intBonus}")
                if (s.lukBonus    > 0) add("행운 +${s.lukBonus}")
            }
            append(effects.joinToString(", "))
            append("\n성공률 ${s.successRate}%")
        }
    }

    fun itemsFor(shopType: ShopType): List<ShopItem> = shopItems[shopType] ?: emptyList()

    /** ShopType.CONSUMABLE 목록에서 SCROLL 타입도 반환 (기존 API 유지) */
    fun scrollItemsIn(shopType: ShopType): List<ShopItem> =
        itemsFor(shopType).filter { it.itemType == ShopItemType.SCROLL }

    fun sellPriceForEquipment(nameOrId: String): Int {
        // itemId 우선 조회, 없으면 name으로 폴백
        EquipmentRegistry.get(nameOrId)?.sellPrice?.takeIf { it > 0 }?.let { return it }
        return shopItems.values.flatten()
            .filter { it.itemType == ShopItemType.EQUIPMENT }
            .find { it.name == nameOrId || it.itemId == nameOrId }
            ?.sellPrice ?: 30
    }

    fun sellPriceForScroll(scrollType: ScrollType): Int =
        shopItems[ShopType.CONSUMABLE]
            ?.find { it.itemType == ShopItemType.SCROLL && it.itemId == scrollType.name }
            ?.sellPrice ?: 10

    fun sellPriceForConsumable(consumableType: ConsumableType): Int {
        val itemId = ConsumableCatalog.itemId(consumableType)
        return shopItems[ShopType.CONSUMABLE]
            ?.find { it.itemType == ShopItemType.CONSUMABLE && it.itemId == itemId }
            ?.sellPrice ?: 5
    }

    fun createEquipment(itemId: String): Equipment? = EquipmentRegistry.get(itemId)
}
