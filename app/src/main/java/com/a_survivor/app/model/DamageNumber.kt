package com.a_survivor.app.model

data class DamageNumber(
    val id: Int,
    val value: Int,
    val worldX: Float,
    val worldY: Float,
    val createdAt: Long,
    val isPlayerDamage: Boolean,
    val isMiss: Boolean = false
)
