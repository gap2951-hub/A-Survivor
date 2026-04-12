package com.a_survivor.app.data

/** 런 간 영구 유지되는 메타 성장 데이터 */
data class MetaStats(
    val bonusDamage: Float    = 0f,
    val bonusMaxHp: Float     = 0f,
    val bonusMoveSpeed: Float = 0f,
    val currency: Int         = 0   // 런 후 누적되는 재화
)
