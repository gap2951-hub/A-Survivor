package com.a_survivor.app.model

sealed class DropItem {
    data class ScrollDrop(val scrollType: ScrollType) : DropItem()
    data class EquipmentDrop(val equipment: Equipment) : DropItem()
    data class MoneyDrop(val amount: Int) : DropItem()
    data class MaterialDrop(val materialType: MaterialType) : DropItem()
}

data class DropEntry(
    val item: DropItem,
    val probability: Float  // 0.0 ~ 1.0, 각 항목은 독립 판정
)
