package com.a_survivor.app.model

data class Equipment(
    val name: String,
    val attackPower: Int,
    val maxUpgradeCount: Int,
    val remainingUpgradeCount: Int,
    val failedUpgradeCount: Int,
    val destroyed: Boolean,
    val availableJobs: List<PlayerJob> = PlayerJob.values().toList(),
    val description: String = ""
)
