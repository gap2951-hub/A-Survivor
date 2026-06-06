package com.a_survivor.app.model

data class Projectile(
    val id: Int,
    val type: ProjectileType,
    val positionX: Float,
    val positionY: Float,
    val targetX: Float,
    val targetY: Float,
    val speed: Float,
    val damage: Int,
    val traveledDistance: Float = 0f,
    val maxTravelDistance: Float = 300f
)
