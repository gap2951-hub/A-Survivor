package com.a_survivor.app.model

enum class MaterialType { SKELETON_BONE, BEEF }

data class MaterialInfo(
    val name: String,
    val sellPrice: Int
)

object MaterialCatalog {
    fun get(type: MaterialType): MaterialInfo = when (type) {
        MaterialType.SKELETON_BONE -> MaterialInfo("스켈레톤뼈", sellPrice = 20)
        MaterialType.BEEF          -> MaterialInfo("소고기",     sellPrice = 50)
    }
}
