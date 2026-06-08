package com.a_survivor.app.model

enum class ScrollType {
    // 장갑 공격력 주문서 (기존)
    GLOVE_ATK_100, GLOVE_ATK_60, GLOVE_ATK_10,
    // 백의 주문서 (기존)
    WHITE_SCROLL_1, WHITE_SCROLL_3,

    // 장갑 스탯 주문서
    GLOVE_STR_100, GLOVE_STR_60, GLOVE_STR_10,
    GLOVE_DEX_100, GLOVE_DEX_60, GLOVE_DEX_10,
    GLOVE_INT_100, GLOVE_INT_60, GLOVE_INT_10,
    GLOVE_LUK_100, GLOVE_LUK_60, GLOVE_LUK_10,

    // 상의 스탯 주문서
    TOP_STR_100, TOP_STR_60, TOP_STR_10,
    TOP_DEX_100, TOP_DEX_60, TOP_DEX_10,
    TOP_INT_100, TOP_INT_60, TOP_INT_10,
    TOP_LUK_100, TOP_LUK_60, TOP_LUK_10,

    // 모자 스탯 주문서
    HAT_STR_100, HAT_STR_60, HAT_STR_10,
    HAT_DEX_100, HAT_DEX_60, HAT_DEX_10,
    HAT_INT_100, HAT_INT_60, HAT_INT_10,
    HAT_LUK_100, HAT_LUK_60, HAT_LUK_10,

    // 신발 스탯 주문서
    SHOES_STR_100, SHOES_STR_60, SHOES_STR_10,
    SHOES_DEX_100, SHOES_DEX_60, SHOES_DEX_10,
    SHOES_INT_100, SHOES_INT_60, SHOES_INT_10,
    SHOES_LUK_100, SHOES_LUK_60, SHOES_LUK_10,

    // 한손검 공격력 주문서
    SWORD_ATK_100, SWORD_ATK_60, SWORD_ATK_10,
    // 표창 공격력 주문서
    STAR_ATK_100, STAR_ATK_60, STAR_ATK_10,
    // 완드 마력 주문서
    WAND_MAG_100, WAND_MAG_60, WAND_MAG_10,
    // 스태프 마력 주문서
    STAFF_MAG_100, STAFF_MAG_60, STAFF_MAG_10
}

data class Scroll(
    val type: ScrollType,
    val name: String,
    val successRate: Int,
    val attackBonus: Int = 0,
    val magicBonus: Int = 0,
    val strBonus: Int = 0,
    val dexBonus: Int = 0,
    val intBonus: Int = 0,
    val lukBonus: Int = 0,
    val isWhiteScroll: Boolean = false,
    val targetSlot: String = ""
)

object ScrollCatalog {
    private val catalog: Map<ScrollType, Scroll> = mapOf(
        // 장갑 공격력 주문서 (기존)
        ScrollType.GLOVE_ATK_100 to Scroll(ScrollType.GLOVE_ATK_100, "장갑 공격력 주문서 100%", 100, attackBonus = 1, targetSlot = "GLOVE"),
        ScrollType.GLOVE_ATK_60  to Scroll(ScrollType.GLOVE_ATK_60,  "장갑 공격력 주문서 60%",   60, attackBonus = 2, targetSlot = "GLOVE"),
        ScrollType.GLOVE_ATK_10  to Scroll(ScrollType.GLOVE_ATK_10,  "장갑 공격력 주문서 10%",   10, attackBonus = 3, targetSlot = "GLOVE"),
        // 백의 주문서
        ScrollType.WHITE_SCROLL_1 to Scroll(ScrollType.WHITE_SCROLL_1, "백의 주문서 1%",  1, isWhiteScroll = true),
        ScrollType.WHITE_SCROLL_3 to Scroll(ScrollType.WHITE_SCROLL_3, "백의 주문서 3%",  3, isWhiteScroll = true),

        // 장갑 스탯 주문서
        ScrollType.GLOVE_STR_100 to Scroll(ScrollType.GLOVE_STR_100, "장갑 힘 주문서 100%", 100, strBonus = 1, targetSlot = "GLOVE"),
        ScrollType.GLOVE_STR_60  to Scroll(ScrollType.GLOVE_STR_60,  "장갑 힘 주문서 60%",   60, strBonus = 2, targetSlot = "GLOVE"),
        ScrollType.GLOVE_STR_10  to Scroll(ScrollType.GLOVE_STR_10,  "장갑 힘 주문서 10%",   10, strBonus = 3, targetSlot = "GLOVE"),
        ScrollType.GLOVE_DEX_100 to Scroll(ScrollType.GLOVE_DEX_100, "장갑 민첩 주문서 100%", 100, dexBonus = 1, targetSlot = "GLOVE"),
        ScrollType.GLOVE_DEX_60  to Scroll(ScrollType.GLOVE_DEX_60,  "장갑 민첩 주문서 60%",   60, dexBonus = 2, targetSlot = "GLOVE"),
        ScrollType.GLOVE_DEX_10  to Scroll(ScrollType.GLOVE_DEX_10,  "장갑 민첩 주문서 10%",   10, dexBonus = 3, targetSlot = "GLOVE"),
        ScrollType.GLOVE_INT_100 to Scroll(ScrollType.GLOVE_INT_100, "장갑 지력 주문서 100%", 100, intBonus = 1, targetSlot = "GLOVE"),
        ScrollType.GLOVE_INT_60  to Scroll(ScrollType.GLOVE_INT_60,  "장갑 지력 주문서 60%",   60, intBonus = 2, targetSlot = "GLOVE"),
        ScrollType.GLOVE_INT_10  to Scroll(ScrollType.GLOVE_INT_10,  "장갑 지력 주문서 10%",   10, intBonus = 3, targetSlot = "GLOVE"),
        ScrollType.GLOVE_LUK_100 to Scroll(ScrollType.GLOVE_LUK_100, "장갑 행운 주문서 100%", 100, lukBonus = 1, targetSlot = "GLOVE"),
        ScrollType.GLOVE_LUK_60  to Scroll(ScrollType.GLOVE_LUK_60,  "장갑 행운 주문서 60%",   60, lukBonus = 2, targetSlot = "GLOVE"),
        ScrollType.GLOVE_LUK_10  to Scroll(ScrollType.GLOVE_LUK_10,  "장갑 행운 주문서 10%",   10, lukBonus = 3, targetSlot = "GLOVE"),

        // 상의 스탯 주문서
        ScrollType.TOP_STR_100 to Scroll(ScrollType.TOP_STR_100, "상의 힘 주문서 100%", 100, strBonus = 1, targetSlot = "TOP"),
        ScrollType.TOP_STR_60  to Scroll(ScrollType.TOP_STR_60,  "상의 힘 주문서 60%",   60, strBonus = 2, targetSlot = "TOP"),
        ScrollType.TOP_STR_10  to Scroll(ScrollType.TOP_STR_10,  "상의 힘 주문서 10%",   10, strBonus = 3, targetSlot = "TOP"),
        ScrollType.TOP_DEX_100 to Scroll(ScrollType.TOP_DEX_100, "상의 민첩 주문서 100%", 100, dexBonus = 1, targetSlot = "TOP"),
        ScrollType.TOP_DEX_60  to Scroll(ScrollType.TOP_DEX_60,  "상의 민첩 주문서 60%",   60, dexBonus = 2, targetSlot = "TOP"),
        ScrollType.TOP_DEX_10  to Scroll(ScrollType.TOP_DEX_10,  "상의 민첩 주문서 10%",   10, dexBonus = 3, targetSlot = "TOP"),
        ScrollType.TOP_INT_100 to Scroll(ScrollType.TOP_INT_100, "상의 지력 주문서 100%", 100, intBonus = 1, targetSlot = "TOP"),
        ScrollType.TOP_INT_60  to Scroll(ScrollType.TOP_INT_60,  "상의 지력 주문서 60%",   60, intBonus = 2, targetSlot = "TOP"),
        ScrollType.TOP_INT_10  to Scroll(ScrollType.TOP_INT_10,  "상의 지력 주문서 10%",   10, intBonus = 3, targetSlot = "TOP"),
        ScrollType.TOP_LUK_100 to Scroll(ScrollType.TOP_LUK_100, "상의 행운 주문서 100%", 100, lukBonus = 1, targetSlot = "TOP"),
        ScrollType.TOP_LUK_60  to Scroll(ScrollType.TOP_LUK_60,  "상의 행운 주문서 60%",   60, lukBonus = 2, targetSlot = "TOP"),
        ScrollType.TOP_LUK_10  to Scroll(ScrollType.TOP_LUK_10,  "상의 행운 주문서 10%",   10, lukBonus = 3, targetSlot = "TOP"),

        // 모자 스탯 주문서
        ScrollType.HAT_STR_100 to Scroll(ScrollType.HAT_STR_100, "모자 힘 주문서 100%", 100, strBonus = 1, targetSlot = "HAT"),
        ScrollType.HAT_STR_60  to Scroll(ScrollType.HAT_STR_60,  "모자 힘 주문서 60%",   60, strBonus = 2, targetSlot = "HAT"),
        ScrollType.HAT_STR_10  to Scroll(ScrollType.HAT_STR_10,  "모자 힘 주문서 10%",   10, strBonus = 3, targetSlot = "HAT"),
        ScrollType.HAT_DEX_100 to Scroll(ScrollType.HAT_DEX_100, "모자 민첩 주문서 100%", 100, dexBonus = 1, targetSlot = "HAT"),
        ScrollType.HAT_DEX_60  to Scroll(ScrollType.HAT_DEX_60,  "모자 민첩 주문서 60%",   60, dexBonus = 2, targetSlot = "HAT"),
        ScrollType.HAT_DEX_10  to Scroll(ScrollType.HAT_DEX_10,  "모자 민첩 주문서 10%",   10, dexBonus = 3, targetSlot = "HAT"),
        ScrollType.HAT_INT_100 to Scroll(ScrollType.HAT_INT_100, "모자 지력 주문서 100%", 100, intBonus = 1, targetSlot = "HAT"),
        ScrollType.HAT_INT_60  to Scroll(ScrollType.HAT_INT_60,  "모자 지력 주문서 60%",   60, intBonus = 2, targetSlot = "HAT"),
        ScrollType.HAT_INT_10  to Scroll(ScrollType.HAT_INT_10,  "모자 지력 주문서 10%",   10, intBonus = 3, targetSlot = "HAT"),
        ScrollType.HAT_LUK_100 to Scroll(ScrollType.HAT_LUK_100, "모자 행운 주문서 100%", 100, lukBonus = 1, targetSlot = "HAT"),
        ScrollType.HAT_LUK_60  to Scroll(ScrollType.HAT_LUK_60,  "모자 행운 주문서 60%",   60, lukBonus = 2, targetSlot = "HAT"),
        ScrollType.HAT_LUK_10  to Scroll(ScrollType.HAT_LUK_10,  "모자 행운 주문서 10%",   10, lukBonus = 3, targetSlot = "HAT"),

        // 신발 스탯 주문서
        ScrollType.SHOES_STR_100 to Scroll(ScrollType.SHOES_STR_100, "신발 힘 주문서 100%", 100, strBonus = 1, targetSlot = "SHOES"),
        ScrollType.SHOES_STR_60  to Scroll(ScrollType.SHOES_STR_60,  "신발 힘 주문서 60%",   60, strBonus = 2, targetSlot = "SHOES"),
        ScrollType.SHOES_STR_10  to Scroll(ScrollType.SHOES_STR_10,  "신발 힘 주문서 10%",   10, strBonus = 3, targetSlot = "SHOES"),
        ScrollType.SHOES_DEX_100 to Scroll(ScrollType.SHOES_DEX_100, "신발 민첩 주문서 100%", 100, dexBonus = 1, targetSlot = "SHOES"),
        ScrollType.SHOES_DEX_60  to Scroll(ScrollType.SHOES_DEX_60,  "신발 민첩 주문서 60%",   60, dexBonus = 2, targetSlot = "SHOES"),
        ScrollType.SHOES_DEX_10  to Scroll(ScrollType.SHOES_DEX_10,  "신발 민첩 주문서 10%",   10, dexBonus = 3, targetSlot = "SHOES"),
        ScrollType.SHOES_INT_100 to Scroll(ScrollType.SHOES_INT_100, "신발 지력 주문서 100%", 100, intBonus = 1, targetSlot = "SHOES"),
        ScrollType.SHOES_INT_60  to Scroll(ScrollType.SHOES_INT_60,  "신발 지력 주문서 60%",   60, intBonus = 2, targetSlot = "SHOES"),
        ScrollType.SHOES_INT_10  to Scroll(ScrollType.SHOES_INT_10,  "신발 지력 주문서 10%",   10, intBonus = 3, targetSlot = "SHOES"),
        ScrollType.SHOES_LUK_100 to Scroll(ScrollType.SHOES_LUK_100, "신발 행운 주문서 100%", 100, lukBonus = 1, targetSlot = "SHOES"),
        ScrollType.SHOES_LUK_60  to Scroll(ScrollType.SHOES_LUK_60,  "신발 행운 주문서 60%",   60, lukBonus = 2, targetSlot = "SHOES"),
        ScrollType.SHOES_LUK_10  to Scroll(ScrollType.SHOES_LUK_10,  "신발 행운 주문서 10%",   10, lukBonus = 3, targetSlot = "SHOES"),

        // 한손검 공격력 주문서
        ScrollType.SWORD_ATK_100 to Scroll(ScrollType.SWORD_ATK_100, "한손검 공격력 주문서 100%", 100, attackBonus = 3, targetSlot = "WEAPON"),
        ScrollType.SWORD_ATK_60  to Scroll(ScrollType.SWORD_ATK_60,  "한손검 공격력 주문서 60%",   60, attackBonus = 5, targetSlot = "WEAPON"),
        ScrollType.SWORD_ATK_10  to Scroll(ScrollType.SWORD_ATK_10,  "한손검 공격력 주문서 10%",   10, attackBonus = 7, targetSlot = "WEAPON"),
        // 표창 공격력 주문서
        ScrollType.STAR_ATK_100 to Scroll(ScrollType.STAR_ATK_100, "표창 공격력 주문서 100%", 100, attackBonus = 3, targetSlot = "WEAPON"),
        ScrollType.STAR_ATK_60  to Scroll(ScrollType.STAR_ATK_60,  "표창 공격력 주문서 60%",   60, attackBonus = 5, targetSlot = "WEAPON"),
        ScrollType.STAR_ATK_10  to Scroll(ScrollType.STAR_ATK_10,  "표창 공격력 주문서 10%",   10, attackBonus = 7, targetSlot = "WEAPON"),
        // 완드 마력 주문서
        ScrollType.WAND_MAG_100 to Scroll(ScrollType.WAND_MAG_100, "완드 마력 주문서 100%", 100, magicBonus = 3, targetSlot = "WEAPON"),
        ScrollType.WAND_MAG_60  to Scroll(ScrollType.WAND_MAG_60,  "완드 마력 주문서 60%",   60, magicBonus = 5, targetSlot = "WEAPON"),
        ScrollType.WAND_MAG_10  to Scroll(ScrollType.WAND_MAG_10,  "완드 마력 주문서 10%",   10, magicBonus = 7, targetSlot = "WEAPON"),
        // 스태프 마력 주문서
        ScrollType.STAFF_MAG_100 to Scroll(ScrollType.STAFF_MAG_100, "스태프 마력 주문서 100%", 100, magicBonus = 3, targetSlot = "WEAPON"),
        ScrollType.STAFF_MAG_60  to Scroll(ScrollType.STAFF_MAG_60,  "스태프 마력 주문서 60%",   60, magicBonus = 5, targetSlot = "WEAPON"),
        ScrollType.STAFF_MAG_10  to Scroll(ScrollType.STAFF_MAG_10,  "스태프 마력 주문서 10%",   10, magicBonus = 7, targetSlot = "WEAPON"),
    )

    fun get(type: ScrollType): Scroll = catalog.getValue(type)
}
