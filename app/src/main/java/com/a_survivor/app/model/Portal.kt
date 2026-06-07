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
            Portal(850f, 286f, MapType.TOWN,    350f, 286f, "마을"),
            Portal(174f, 286f, MapType.FIELD_2, 800f, 286f, "중급 사냥터")
        )
        MapType.TOWN -> listOf(
            Portal(250f, 286f, MapType.BEGINNER_FIELD, 750f, 286f, "초보자 사냥터")
        )
        MapType.FIELD_2 -> listOf(
            Portal(850f, 286f, MapType.BEGINNER_FIELD, 300f, 286f, "초보자 사냥터"),
            Portal(174f, 286f, MapType.FIELD_3,        800f, 286f, "상급 사냥터")
        )
        MapType.FIELD_3 -> listOf(
            Portal(850f, 286f, MapType.FIELD_2, 300f, 286f, "중급 사냥터")
        )
    }
}
