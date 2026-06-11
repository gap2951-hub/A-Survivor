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
        // 스켈레톤 사냥터: 포탈을 우측 상단(880,80) / 좌측 상단(144,80)으로 배치
        MapType.BEGINNER_FIELD -> listOf(
            Portal(880f, 286f, MapType.TOWN,    350f, 286f, "마을"),
            Portal(144f, 286f, MapType.FIELD_2, 800f, 286f, "중급 사냥터")
        )
        MapType.TOWN -> listOf(
            Portal(250f, 286f, MapType.BEGINNER_FIELD,   860f, 286f, "초보자 사냥터"),
            Portal(750f, 286f, MapType.MINOTAUR_FIELD_1, 300f, 286f, "미노타우르스 사냥터")
        )
        MapType.FIELD_2 -> listOf(
            Portal(880f, 286f, MapType.BEGINNER_FIELD, 160f, 286f, "초보자 사냥터"),
            Portal(144f, 286f, MapType.FIELD_3,        800f, 286f, "상급 사냥터")
        )
        MapType.FIELD_3 -> listOf(
            Portal(880f, 286f, MapType.FIELD_2, 160f, 286f, "중급 사냥터")
        )
        MapType.MINOTAUR_FIELD_1 -> listOf(
            Portal(174f, 286f, MapType.TOWN,             750f, 286f, "마을"),
            Portal(850f, 286f, MapType.MINOTAUR_FIELD_2, 300f, 286f, "미노타우르스 사냥터 2")
        )
        MapType.MINOTAUR_FIELD_2 -> listOf(
            Portal(174f, 286f, MapType.MINOTAUR_FIELD_1, 800f, 286f, "미노타우르스 사냥터 1"),
            Portal(850f, 286f, MapType.MINOTAUR_FIELD_3, 300f, 286f, "미노타우르스 사냥터 3")
        )
        MapType.MINOTAUR_FIELD_3 -> listOf(
            Portal(174f, 286f, MapType.MINOTAUR_FIELD_2, 800f, 286f, "미노타우르스 사냥터 2")
        )
    }
}
