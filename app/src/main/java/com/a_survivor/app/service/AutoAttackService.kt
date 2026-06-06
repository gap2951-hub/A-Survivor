package com.a_survivor.app.service

import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.distanceTo
import kotlin.random.Random

class AutoAttackService {
    companion object {
        const val ATTACK_RANGE = 60f
    }

    fun tick(
        player: Player,
        equipment: Equipment?,
        monsters: List<Monster>,
        derivedStats: DerivedStats
    ): AutoAttackResult {
        val target = findTarget(player, monsters)
            ?: return AutoAttackResult(monsters, targetId = null, damage = 0)

        // 명중 판정: accuracy / (accuracy + monster.avoidability)
        val acc      = derivedStats.accuracy
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

        val damage = maxOf(derivedStats.attackPower.coerceAtLeast(derivedStats.magicPower), 1)

        val killed = mutableListOf<Monster>()
        val updatedMonsters = monsters.mapNotNull { monster ->
            if (monster.id == target.id) {
                val newHp = monster.hp - damage
                if (newHp <= 0) { killed.add(monster); null }
                else monster.copy(hp = newHp)
            } else monster
        }

        return AutoAttackResult(
            updatedMonsters = updatedMonsters,
            targetId        = target.id,
            damage          = damage,
            killedMonsters  = killed
        )
    }

    private fun findTarget(player: Player, monsters: List<Monster>): Monster? =
        monsters
            .filter { it.distanceTo(player.positionX, player.positionY) <= ATTACK_RANGE }
            .minByOrNull { it.distanceTo(player.positionX, player.positionY) }
}

data class AutoAttackResult(
    val updatedMonsters: List<Monster>,
    val targetId: Int?,
    val damage: Int,
    val killedMonsters: List<Monster> = emptyList(),
    val isMiss: Boolean = false
)
