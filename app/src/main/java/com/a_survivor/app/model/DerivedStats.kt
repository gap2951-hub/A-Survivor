package com.a_survivor.app.model

data class DerivedStats(
    val attackPower: Int = 0,
    val magicPower: Int = 0,
    val accuracy: Int = 0,
    val avoidability: Int = 0,
    val physicalDefense: Int = 0,
    val magicDefense: Int = 0,
    val criticalRate: Float = 0f,
    val moveSpeed: Float = 0f,
    val attackSpeed: Float = 0f,      // 장비 보너스 (ms 감소량)
    val attackIntervalMs: Long = 900L  // 실제 공격 주기 = 무기 기준값 - 장비 보너스
)
