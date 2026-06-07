package com.a_survivor.app.model

data class ShopItem(
    val id: Int,
    val name: String,
    val description: String,
    val buyPrice: Int,
    val sellPrice: Int,
    val itemType: ShopItemType,
    val itemId: String,
    val stackable: Boolean
)
