package com.a_survivor.app.model

data class Player(
    val level: Int = 1,
    val exp: Int = 0,

    val hp: Int = 100,
    val maxHp: Int = 100,

    val job: PlayerJob = PlayerJob.WARRIOR,
    val stats: PlayerStats = PlayerJob.WARRIOR.initialStats(),
    val availableStatPoint: Int = 0,
    val weapon: Weapon = DefaultWeapon,

    val positionX: Float = 742f,
    val positionY: Float = 346f
)
