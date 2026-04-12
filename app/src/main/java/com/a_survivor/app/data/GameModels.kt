package com.a_survivor.app.data

import kotlin.math.sqrt

// ── 2D 벡터 ─────────────────────────────────────────────
data class Vector2(val x: Float = 0f, val y: Float = 0f) {
    companion object { val Zero = Vector2(0f, 0f) }
    operator fun plus(o: Vector2)  = Vector2(x + o.x, y + o.y)
    operator fun minus(o: Vector2) = Vector2(x - o.x, y - o.y)
    operator fun times(s: Float)   = Vector2(x * s,   y * s)
    fun length() = sqrt(x * x + y * y)
    fun normalized(): Vector2 {
        val l = length()
        return if (l > 0f) Vector2(x / l, y / l) else Zero
    }
    fun distanceTo(o: Vector2) = (this - o).length()
}

// ── 플레이어 상태 ─────────────────────────────────────────
data class PlayerData(
    val position: Vector2       = Vector2.Zero,
    val hp: Float               = 100f,
    val maxHp: Float            = 100f,
    val level: Int              = 1,
    val exp: Float              = 0f,
    val expToNextLevel: Float   = 100f,
    val moveSpeed: Float        = 220f,
    val attackDamage: Float     = 20f,
    val attacksPerSecond: Float = 1f,
    val attackRange: Float      = 220f,
    val attackCooldown: Float   = 0f  // 현재 쿨다운 타이머
)

// ── 적 상태 ───────────────────────────────────────────────
data class EnemyData(
    val id: Int,
    val position: Vector2,
    val hp: Float,
    val maxHp: Float,
    val moveSpeed: Float    = 80f,
    val contactDamage: Float = 10f,
    val expDrop: Float      = 20f,
    val damageCooldown: Float = 0f  // 연속 피해 방지용 쿨다운
)

// ── 투사체 ────────────────────────────────────────────────
data class Projectile(
    val id: Int,
    val position: Vector2,
    val velocity: Vector2,
    val damage: Float,
    val maxRange: Float,
    val distanceTraveled: Float = 0f
)

// ── 게임 단계 ─────────────────────────────────────────────
enum class GamePhase { PLAYING, UPGRADE_SELECTION, GAME_OVER }

// ── 전체 게임 상태 ────────────────────────────────────────
data class GameState(
    val phase: GamePhase            = GamePhase.PLAYING,
    val player: PlayerData          = PlayerData(),
    val enemies: List<EnemyData>    = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val score: Int                  = 0,
    val survivalTime: Float         = 0f,
    val upgradeChoices: List<Upgrade> = emptyList(),
    val screenWidth: Float          = 0f,
    val screenHeight: Float         = 0f
)
