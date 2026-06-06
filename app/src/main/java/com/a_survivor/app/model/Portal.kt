package com.a_survivor.app.model

data class Portal(
    val worldX: Float,
    val worldY: Float,
    val targetMap: MapType,
    val targetX: Float,
    val targetY: Float,
    val label: String
)

object PortalRegistry {
    fun portalsFor(mapType: MapType): List<Portal> = when (mapType) {
        MapType.BEGINNER_FIELD -> listOf(
            // 오른쪽 포탈 → 마을 (도착: x=350, 포탈x=250에서 100f 거리)
            Portal(850f, 286f, MapType.TOWN, 350f, 286f, "마을")
        )
        MapType.TOWN -> listOf(
            // 포탈 x=250 (장애물 없는 개활지) → 사냥터 도착 x=750
            Portal(250f, 286f, MapType.BEGINNER_FIELD, 750f, 286f, "초보자 사냥터")
        )
    }
}
