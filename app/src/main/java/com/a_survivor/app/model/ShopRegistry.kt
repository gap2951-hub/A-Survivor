package com.a_survivor.app.model

object ShopRegistry {

    private val equipmentShopItems = listOf(
        ShopItem(
            id = 1, name = "초보자 검",
            description = "초보 모험가를 위한 기본 검.\n공격력 +5",
            buyPrice = 300, sellPrice = 90,
            itemType = ShopItemType.EQUIPMENT, itemId = "beginner_sword", stackable = false
        ),
        ShopItem(
            id = 2, name = "낡은 전사 상의",
            description = "낡고 헤진 전사용 상의.\nSTR +2, DEX +1, 물리방어력 +5",
            buyPrice = 500, sellPrice = 150,
            itemType = ShopItemType.EQUIPMENT, itemId = "old_warrior_top", stackable = false
        ),
        ShopItem(
            id = 3, name = "낡은 마법사 로브",
            description = "낡고 헤진 마법사용 로브.\nINT +2, LUK +1, 마법방어력 +5",
            buyPrice = 500, sellPrice = 150,
            itemType = ShopItemType.EQUIPMENT, itemId = "old_mage_robe", stackable = false
        ),
        ShopItem(
            id = 4, name = "낡은 가죽 신발",
            description = "낡고 헤진 가죽 신발.\nDEX +1, LUK +1, 회피율 +1",
            buyPrice = 400, sellPrice = 120,
            itemType = ShopItemType.EQUIPMENT, itemId = "old_leather_shoes", stackable = false
        )
    )

    private val consumableShopItems = listOf(
        ShopItem(
            id = 10, name = "빨간 포션",
            description = "HP 30 회복",
            buyPrice = 50, sellPrice = 15,
            itemType = ShopItemType.CONSUMABLE, itemId = "red_potion", stackable = true
        ),
        ShopItem(
            id = 11, name = "주황 포션",
            description = "HP 80 회복",
            buyPrice = 120, sellPrice = 36,
            itemType = ShopItemType.CONSUMABLE, itemId = "orange_potion", stackable = true
        ),
        ShopItem(
            id = 20, name = "장갑 공격력 주문서 100%",
            description = "장갑의 공격력을 1 올려준다.\n성공률 100%",
            buyPrice = 300, sellPrice = 90,
            itemType = ShopItemType.SCROLL, itemId = ScrollType.GLOVE_ATK_100.name, stackable = true
        ),
        ShopItem(
            id = 21, name = "장갑 공격력 주문서 60%",
            description = "장갑의 공격력을 2 올려준다.\n성공률 60%",
            buyPrice = 700, sellPrice = 210,
            itemType = ShopItemType.SCROLL, itemId = ScrollType.GLOVE_ATK_60.name, stackable = true
        ),
        ShopItem(
            id = 22, name = "장갑 공격력 주문서 10%",
            description = "장갑의 공격력을 3 올려준다.\n성공률 10%",
            buyPrice = 1500, sellPrice = 450,
            itemType = ShopItemType.SCROLL, itemId = ScrollType.GLOVE_ATK_10.name, stackable = true
        )
    )

    fun itemsFor(shopType: ShopType): List<ShopItem> = when (shopType) {
        ShopType.EQUIPMENT  -> equipmentShopItems
        ShopType.CONSUMABLE -> consumableShopItems
    }

    fun sellPriceForEquipment(name: String): Int =
        equipmentShopItems.find { it.name == name }?.sellPrice ?: 30

    fun sellPriceForScroll(scrollType: ScrollType): Int =
        consumableShopItems.find { it.itemType == ShopItemType.SCROLL && it.itemId == scrollType.name }?.sellPrice ?: 10

    fun sellPriceForConsumable(consumableType: ConsumableType): Int =
        consumableShopItems.find { it.itemId == ConsumableCatalog.itemId(consumableType) }?.sellPrice ?: 5

    fun createEquipment(itemId: String): Equipment? = when (itemId) {
        "beginner_sword" -> Equipment(
            name = "초보자 검", attackPower = 5,
            maxUpgradeCount = 0, remainingUpgradeCount = 0, failedUpgradeCount = 0, destroyed = false,
            description = "초보 모험가를 위한 기본 검."
        )
        "old_warrior_top" -> Equipment(
            name = "낡은 전사 상의", attackPower = 0,
            maxUpgradeCount = 0, remainingUpgradeCount = 0, failedUpgradeCount = 0, destroyed = false,
            strBonus = 2, dexBonus = 1, physicalDefense = 5,
            description = "낡고 헤진 전사용 상의."
        )
        "old_mage_robe" -> Equipment(
            name = "낡은 마법사 로브", attackPower = 0,
            maxUpgradeCount = 0, remainingUpgradeCount = 0, failedUpgradeCount = 0, destroyed = false,
            intBonus = 2, lukBonus = 1, magicDefense = 5,
            description = "낡고 헤진 마법사용 로브."
        )
        "old_leather_shoes" -> Equipment(
            name = "낡은 가죽 신발", attackPower = 0,
            maxUpgradeCount = 0, remainingUpgradeCount = 0, failedUpgradeCount = 0, destroyed = false,
            dexBonus = 1, lukBonus = 1, avoidability = 1,
            description = "낡고 헤진 가죽 신발."
        )
        else -> null
    }
}
