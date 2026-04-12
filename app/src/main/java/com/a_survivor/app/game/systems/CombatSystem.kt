package com.a_survivor.app.game.systems

import com.a_survivor.app.data.EnemyData
import com.a_survivor.app.data.PlayerData
import com.a_survivor.app.data.Projectile
import com.a_survivor.app.data.Vector2
import kotlin.math.max

object CombatSystem {
    private var nextProjectileId = 0

    /**
     * 자동 공격: 사거리 내 가장 가까운 적을 향해 투사체 발사
     * 반환: (업데이트된 플레이어, 업데이트된 투사체 리스트)
     */
    fun updateAttack(
        player: PlayerData,
        enemies: List<EnemyData>,
        projectiles: List<Projectile>,
        deltaTime: Float
    ): Pair<PlayerData, List<Projectile>> {
        val newCooldown = max(0f, player.attackCooldown - deltaTime)

        // 쿨다운 중이거나 적이 없으면 스킵
        if (newCooldown > 0f || enemies.isEmpty()) {
            return player.copy(attackCooldown = newCooldown) to projectiles
        }

        // 사거리 내 가장 가까운 적 탐색
        val nearest = enemies
            .filter { it.position.distanceTo(player.position) <= player.attackRange }
            .minByOrNull  { it.position.distanceTo(player.position) }
            ?: return player.copy(attackCooldown = newCooldown) to projectiles

        // 투사체 생성
        val dir = (nearest.position - player.position).normalized()
        val projectile = Projectile(
            id                = nextProjectileId++,
            position          = player.position,
            velocity          = dir * 600f,
            damage            = player.attackDamage,
            maxRange          = player.attackRange
        )

        val cooldown = 1f / player.attacksPerSecond
        return player.copy(attackCooldown = cooldown) to (projectiles + projectile)
    }

    /**
     * 투사체 이동 및 적과의 충돌 처리
     * 반환: (살아있는 투사체, 피해를 입은 적 리스트)
     */
    fun updateProjectiles(
        projectiles: List<Projectile>,
        enemies: List<EnemyData>,
        deltaTime: Float
    ): Pair<List<Projectile>, List<EnemyData>> {
        val updatedEnemies   = enemies.toMutableList()
        val aliveProjectiles = mutableListOf<Projectile>()

        for (proj in projectiles) {
            val move    = proj.velocity * deltaTime
            val newPos  = proj.position + move
            val newDist = proj.distanceTraveled + move.length()

            if (newDist > proj.maxRange) continue  // 사거리 초과 → 소멸

            // 충돌 검사 (히트박스 반지름 28px)
            val hitIndex = updatedEnemies.indexOfFirst { e ->
                newPos.distanceTo(e.position) < 28f
            }

            if (hitIndex >= 0) {
                val e = updatedEnemies[hitIndex]
                updatedEnemies[hitIndex] = e.copy(hp = e.hp - proj.damage)
                // 투사체는 적에 맞으면 소멸
            } else {
                aliveProjectiles.add(proj.copy(position = newPos, distanceTraveled = newDist))
            }
        }

        return aliveProjectiles to updatedEnemies
    }

    /**
     * 적과의 접촉 피해 처리
     * 반환: (피해를 입은 플레이어, 쿨다운이 업데이트된 적 리스트)
     */
    fun checkContactDamage(
        player: PlayerData,
        enemies: List<EnemyData>,
        deltaTime: Float
    ): Pair<PlayerData, List<EnemyData>> {
        var currentHp = player.hp
        val updated = enemies.map { e ->
            val dist = e.position.distanceTo(player.position)
            when {
                dist < 35f && e.damageCooldown <= 0f -> {
                    currentHp -= e.contactDamage
                    e.copy(damageCooldown = 0.8f)  // 0.8초 피해 쿨다운
                }
                else -> e.copy(damageCooldown = max(0f, e.damageCooldown - deltaTime))
            }
        }
        return player.copy(hp = currentHp) to updated
    }

    fun reset() { nextProjectileId = 0 }
}
