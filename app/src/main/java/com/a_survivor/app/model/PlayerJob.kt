package com.a_survivor.app.model

enum class PlayerJob {
    BEGINNER,
    WARRIOR,
    MAGE,
    ARCHER,
    THIEF,
    PIRATE;

    fun initialStats(): PlayerStats = when (this) {
        BEGINNER -> PlayerStats(str = 10, dex = 10, `int` = 10, luk = 10)
        WARRIOR  -> PlayerStats(str = 20, dex = 5,  `int` = 4,  luk = 4)
        ARCHER   -> PlayerStats(str = 5,  dex = 20, `int` = 4,  luk = 4)
        THIEF    -> PlayerStats(str = 5,  dex = 10, `int` = 4,  luk = 15)
        MAGE     -> PlayerStats(str = 4,  dex = 4,  `int` = 20, luk = 5)
        PIRATE   -> PlayerStats(str = 10, dex = 16, `int` = 4,  luk = 6)
    }
}

fun PlayerJob.attackType(): AttackType = when (this) {
    PlayerJob.BEGINNER, PlayerJob.WARRIOR -> AttackType.MELEE
    else -> AttackType.PROJECTILE
}

fun PlayerJob.attackRange(): Float = when (this) {
    PlayerJob.BEGINNER -> 60f
    PlayerJob.WARRIOR  -> 60f
    PlayerJob.MAGE     -> 170f
    PlayerJob.ARCHER   -> 220f
    PlayerJob.THIEF    -> 180f
    PlayerJob.PIRATE   -> 190f
}

fun PlayerJob.projectileType(): ProjectileType = when (this) {
    PlayerJob.MAGE   -> ProjectileType.ENERGY_BOLT
    PlayerJob.ARCHER -> ProjectileType.ARROW
    PlayerJob.THIEF  -> ProjectileType.THROWING_STAR
    PlayerJob.PIRATE -> ProjectileType.BULLET
    else             -> ProjectileType.ENERGY_BOLT
}

fun PlayerJob.projectileSpeed(): Float = when (this) {
    PlayerJob.MAGE   -> 6f
    PlayerJob.ARCHER -> 9f
    PlayerJob.THIEF  -> 11f
    PlayerJob.PIRATE -> 13f
    else             -> 8f
}
