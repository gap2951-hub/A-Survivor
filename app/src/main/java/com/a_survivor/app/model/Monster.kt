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

    val avoidability: Int = 5,
    val accuracy: Int = 15,

    val state: MonsterState = MonsterState.IDLE,
    val lastAttackTime: Long = 0L,
    val facingLeft: Boolean = false,
    val variant: Int = 1   // 1·2·3 — 맵별 색상 구분
)

fun Monster.distanceTo(x: Float, y: Float): Float {
    val dx = positionX - x
    val dy = positionY - y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

fun skeletonWarrior(
    id: Int,
    positionX: Float = 0f,
    positionY: Float = 0f,
    variant: Int = 1,
    hp: Int = 30,
    expReward: Int = 10,
    avoidability: Int = 8,
    accuracy: Int = 18,
    speed: Float = 1.2f
) = Monster(
    id          = id,
    name        = "스켈레톤 워리어",
    hp          = hp,
    maxHp       = hp,
    positionX   = positionX,
    positionY   = positionY,
    speed       = speed,
    expReward   = expReward,
    avoidability = avoidability,
    accuracy    = accuracy,
    variant     = variant
)
