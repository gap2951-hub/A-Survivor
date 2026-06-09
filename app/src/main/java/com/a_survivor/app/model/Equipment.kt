package com.a_survivor.app.model

data class Equipment(
    val name: String,
    val attackPower: Int,
    val maxUpgradeCount: Int,
    val remainingUpgradeCount: Int,
    val failedUpgradeCount: Int,
    val destroyed: Boolean,
    val availableJobs: List<PlayerJob> = PlayerJob.values().toList(),
    val description: String = "",
    // 기본 스탯 보정
    val strBonus: Int = 0,
    val dexBonus: Int = 0,
    val intBonus: Int = 0,
    val lukBonus: Int = 0,
    // 전투 능력치 보정 (장비 전용)
    val magicPower: Int = 0,
    val accuracy: Int = 0,
    val avoidability: Int = 0,
    val physicalDefense: Int = 0,
    val magicDefense: Int = 0,
    val criticalRate: Float = 0f,
    val moveSpeed: Float = 0f,
    val attackSpeed: Float = 0f,
    // 데이터 테이블 식별자 및 상점 정보 (기본값: 이전 저장 데이터 호환)
    val itemId: String = "",
    val slot: String = "GLOVE",
    val requiredLevel: Int = 1,
    val buyPrice: Int = 0,
    val sellPrice: Int = 0
)
