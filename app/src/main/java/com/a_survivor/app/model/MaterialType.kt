package com.a_survivor.app.model

enum class MaterialType {
    SKELETON_BONE, BEEF,            // 레거시 (구 세이브 호환)
    SKELETON_BONE_1, SKELETON_BONE_2, SKELETON_BONE_3,
    MINOTAUR_BEEF_1, MINOTAUR_BEEF_2, MINOTAUR_BEEF_3
}

// bone | beef — 동일 아이콘 재사용
fun MaterialType.iconKey(): String = if (name.contains("BONE")) "bone" else "beef"

data class MaterialInfo(
    val name: String,
    val description: String = "",
    val sellPrice: Int
)

object MaterialCatalog {
    fun get(type: MaterialType): MaterialInfo = when (type) {
        MaterialType.SKELETON_BONE   -> MaterialInfo("스켈레톤뼈",  sellPrice = 20)
        MaterialType.BEEF            -> MaterialInfo("소고기",      sellPrice = 50)
        MaterialType.SKELETON_BONE_1 -> MaterialInfo("스켈레톤1의 뼈",
            "약한 스켈레톤에게서 얻은 뼈 조각.\n미약한 마력이 남아 있다.", sellPrice = 20)
        MaterialType.SKELETON_BONE_2 -> MaterialInfo("스켈레톤2의 뼈",
            "강한 스켈레톤에게서 얻은 뼈.\n표면에 검은 마력이 스며들어 있다.", sellPrice = 40)
        MaterialType.SKELETON_BONE_3 -> MaterialInfo("스켈레톤3의 뼈",
            "정예 스켈레톤에게서 얻은 뼈.\n강한 마력 반응이 느껴진다.", sellPrice = 80)
        MaterialType.MINOTAUR_BEEF_1 -> MaterialInfo("질긴 소고기",
            "거친 초원에서 자란 미노타우르스의 고기.\n식용 가능하지만 질기다.", sellPrice = 50)
        MaterialType.MINOTAUR_BEEF_2 -> MaterialInfo("신선한 소고기",
            "상태가 좋은 미노타우르스의 고기.\n마을 주민들이 선호한다.", sellPrice = 100)
        MaterialType.MINOTAUR_BEEF_3 -> MaterialInfo("최상급 소고기",
            "강력한 미노타우르스에게서 얻은 고기.\n고급 요리 재료로 사용된다.", sellPrice = 200)
    }
}
