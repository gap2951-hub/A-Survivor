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
            Portal(850f, 286f, MapType.TOWN, 800f, 286f, "마을")
        )
        MapType.TOWN -> listOf(
            Portal(100f, 286f, MapType.BEGINNER_FIELD, 100f, 286f, "초보자 사냥터")
        )
    }
}
