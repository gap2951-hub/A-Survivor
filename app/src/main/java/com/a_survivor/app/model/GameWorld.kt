package com.a_survivor.app.model

enum class MapType { BEGINNER_FIELD, TOWN, FIELD_2, FIELD_3 }

data class GameWorld(
    val width: Float = 1024f,
    val height: Float = 572f,
    val mapType: MapType = MapType.BEGINNER_FIELD
) {
    /** 주어진 좌표가 월드 범위 안에 있는지 확인 */
    fun contains(x: Float, y: Float): Boolean =
        x in 0f..width && y in 0f..height

    /** 플레이어 좌표를 월드 경계 안으로 제한 */
    fun clampPosition(x: Float, y: Float): Pair<Float, Float> =
        x.coerceIn(0f, width) to y.coerceIn(0f, height)
}

val DefaultWorld = GameWorld()
val TownWorld    = GameWorld(mapType = MapType.TOWN)
val Field2World  = GameWorld(mapType = MapType.FIELD_2)
val Field3World  = GameWorld(mapType = MapType.FIELD_3)
