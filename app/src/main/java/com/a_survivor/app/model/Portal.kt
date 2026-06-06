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
            // 오른쪽 포탈 → 마을 (도착: 마을 왼쪽, 귀환 포탈 근처)
            Portal(850f, 286f, MapType.TOWN, 150f, 286f, "마을")
        )
        MapType.TOWN -> listOf(
            // 왼쪽 포탈 → 초보자 사냥터 (도착: 사냥터 포탈 근처)
            Portal(100f, 286f, MapType.BEGINNER_FIELD, 800f, 286f, "초보자 사냥터")
        )
    }
}
