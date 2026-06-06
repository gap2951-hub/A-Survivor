package com.a_survivor.app.model

data class GameWorld(
    val width: Float = 1600f,
    val height: Float = 1200f
) {
    /** 주어진 좌표가 월드 범위 안에 있는지 확인 */
    fun contains(x: Float, y: Float): Boolean =
        x in 0f..width && y in 0f..height

    /** 플레이어 좌표를 월드 경계 안으로 제한 */
    fun clampPosition(x: Float, y: Float): Pair<Float, Float> =
        x.coerceIn(0f, width) to y.coerceIn(0f, height)
}

val DefaultWorld = GameWorld()
