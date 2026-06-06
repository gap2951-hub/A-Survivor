package com.a_survivor.app.service

import com.a_survivor.app.model.AttackType
import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.Projectile
import com.a_survivor.app.model.attackType
import com.a_survivor.app.model.distanceTo
import com.a_survivor.app.model.projectileSpeed
import com.a_survivor.app.model.projectileType
import kotlin.random.Random

class AutoAttackService {

    fun tick(
        player: Player,
        equipment: Equipment?,
        monsters: List<Monster>,
        derivedStats: DerivedStats,
        attackRange: Float,
        nextProjectileId: Int
    ): AutoAttackResult {
        val target = findTarget(player, monsters, attackRange)
            ?: return AutoAttackResult(monsters, targetId = null, damage = 0)

        // 명중 판정: accuracy / (accuracy + monster.avoidability)
        val acc       = derivedStats.accuracy
        val hitChance = acc.toFloat() / (acc + target.avoidability).coerceAtLeast(1)
        val hit       = Random.nextFloat() < hitChance

        if (!hit) {
            return AutoAttackResult(
                updatedMonsters = monsters,
                targetId        = target.id,
                damage          = 0,
                isMiss          = true
            )
        }

        return if (player.job.attackType() == AttackType.PROJECTILE) {
            val damage = if (player.job == PlayerJob.MAGE)
                derivedStats.magicPower.coerceAtLeast(1)
            else
                derivedStats.attackPower.coerceAtLeast(1)

            AutoAttackResult(
                updatedMonsters = monsters,
                targetId        = target.id,
                damage          = 0,
                newProjectile   = Projectile(
                    id                = nextProjectileId,
                    type              = player.job.projectileType(),
                    positionX         = player.positionX,
                    positionY         = player.positionY,
                    targetX           = target.positionX,
                    targetY           = target.positionY,
                    speed             = player.job.projectileSpeed(),
                    damage            = damage,
                    maxTravelDistance = attackRange * 1.5f
                )
            )
        } else {
            val damage      = derivedStats.attackPower.coerceAtLeast(1)
            val killed      = mutableListOf<Monster>()
            val updatedMonsters = monsters.mapNotNull { monster ->
                if (monster.id == target.id) {
                    val newHp = monster.hp - damage
                    if (newHp <= 0) { killed.add(monster); null }
                    else monster.copy(hp = newHp)
                } else monster
            }
            AutoAttackResult(
                updatedMonsters = updatedMonsters,
                targetId        = target.id,
                damage          = damage,
                killedMonsters  = killed
            )
        }
    }

    private fun findTarget(player: Player, monsters: List<Monster>, attackRange: Float): Monster? =
        monsters
            .filter { it.distanceTo(player.positionX, player.positionY) <= attackRange }
            .minByOrNull { it.distanceTo(player.positionX, player.positionY) }
}

data class AutoAttackResult(
    val updatedMonsters: List<Monster>,
    val targetId: Int?,
    val damage: Int,
    val killedMonsters: List<Monster> = emptyList(),
    val isMiss: Boolean = false,
    val newProjectile: Projectile? = null
)
