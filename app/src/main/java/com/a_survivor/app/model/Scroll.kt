package com.a_survivor.app.model

enum class ScrollType {
    GLOVE_ATK_100,
    GLOVE_ATK_60,
    GLOVE_ATK_10,
    WHITE_SCROLL_1,
    WHITE_SCROLL_3
}

data class Scroll(
    val type: ScrollType,
    val name: String,
    val successRate: Int,
    val attackBonus: Int,
    val isWhiteScroll: Boolean
)

object ScrollCatalog {
    private val catalog: Map<ScrollType, Scroll> = mapOf(
        ScrollType.GLOVE_ATK_100  to Scroll(ScrollType.GLOVE_ATK_100,  "장갑 공격력 주문서 100%", 100, 1, false),
        ScrollType.GLOVE_ATK_60   to Scroll(ScrollType.GLOVE_ATK_60,   "장갑 공격력 주문서 60%",   60, 2, false),
        ScrollType.GLOVE_ATK_10   to Scroll(ScrollType.GLOVE_ATK_10,   "장갑 공격력 주문서 10%",   10, 3, false),
        ScrollType.WHITE_SCROLL_1 to Scroll(ScrollType.WHITE_SCROLL_1, "백의 주문서 1%",             1, 0, true),
        ScrollType.WHITE_SCROLL_3 to Scroll(ScrollType.WHITE_SCROLL_3, "백의 주문서 3%",             3, 0, true)
    )

    fun get(type: ScrollType): Scroll = catalog.getValue(type)
}
