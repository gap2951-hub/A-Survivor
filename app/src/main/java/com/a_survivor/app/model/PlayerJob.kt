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
