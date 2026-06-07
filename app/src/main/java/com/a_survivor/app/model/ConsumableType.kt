package com.a_survivor.app.model

enum class ConsumableType {
    RED_POTION,
    ORANGE_POTION
}

data class ConsumableInfo(
    val type: ConsumableType,
    val name: String,
    val healAmount: Int,
    val description: String
)

object ConsumableCatalog {
    private val catalog: Map<ConsumableType, ConsumableInfo> = mapOf(
        ConsumableType.RED_POTION    to ConsumableInfo(ConsumableType.RED_POTION,    "빨간 포션", 30, "HP 30 회복"),
        ConsumableType.ORANGE_POTION to ConsumableInfo(ConsumableType.ORANGE_POTION, "주황 포션", 80, "HP 80 회복")
    )

    fun get(type: ConsumableType): ConsumableInfo = catalog.getValue(type)

    fun itemId(type: ConsumableType): String = when (type) {
        ConsumableType.RED_POTION    -> "red_potion"
        ConsumableType.ORANGE_POTION -> "orange_potion"
    }

    fun fromItemId(itemId: String): ConsumableType? = when (itemId) {
        "red_potion"    -> ConsumableType.RED_POTION
        "orange_potion" -> ConsumableType.ORANGE_POTION
        else            -> null
    }
}
