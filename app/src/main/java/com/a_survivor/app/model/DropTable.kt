package com.a_survivor.app.model

sealed class DropItem {
    data class ScrollDrop(val scrollType: ScrollType) : DropItem()
    data class EquipmentDrop(val equipment: Equipment) : DropItem()
    data class MoneyDrop(val amount: Int) : DropItem()
}

data class DropEntry(
    val item: DropItem,
    val probability: Float  // 0.0 ~ 1.0, 각 항목은 독립 판정
)

private val gloveEntry = DropEntry(
    item = DropItem.EquipmentDrop(
        Equipment(
            name                  = "노가다 목장갑",
            attackPower           = 0,
            maxUpgradeCount       = 5,
            remainingUpgradeCount = 5,
            failedUpgradeCount    = 0,
            destroyed             = false,
            description           = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
        )
    ),
    probability = 0.05f
)

private val scrollEntries = listOf(
    DropEntry(DropItem.ScrollDrop(ScrollType.GLOVE_ATK_100),  0.20f),
    DropEntry(DropItem.ScrollDrop(ScrollType.GLOVE_ATK_60),   0.10f),
    DropEntry(DropItem.ScrollDrop(ScrollType.GLOVE_ATK_10),   0.03f),
    DropEntry(DropItem.ScrollDrop(ScrollType.WHITE_SCROLL_1), 0.01f)
)

fun dropEntriesFor(mapType: MapType): List<DropEntry> {
    val moneyRange = when (mapType) {
        MapType.BEGINNER_FIELD -> 10..15
        MapType.FIELD_2        -> 15..20
        MapType.FIELD_3        -> 25..35
        else                   -> return emptyList()
    }
    return listOf(DropEntry(DropItem.MoneyDrop(moneyRange.random()), 1.0f)) +
            gloveEntry + scrollEntries
}
