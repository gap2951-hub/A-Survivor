package com.a_survivor.app.model

data class Monster(
    val id: Int,
    val name: String,

    val hp: Int,
    val maxHp: Int,

    val positionX: Float,
    val positionY: Float,

    val speed: Float,

    val expReward: Int,

    val state: MonsterState = MonsterState.IDLE,
    val lastAttackTime: Long = 0L
)

fun Monster.distanceTo(x: Float, y: Float): Float {
    val dx = positionX - x
    val dy = positionY - y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

fun slime(id: Int, positionX: Float = 0f, positionY: Float = 0f) = Monster(
    id = id,
    name = "슬라임",
    hp = 20,
    maxHp = 20,
    positionX = positionX,
    positionY = positionY,
    speed = 1f,
    expReward = 5
)
