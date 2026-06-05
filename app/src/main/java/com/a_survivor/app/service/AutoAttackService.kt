package com.a_survivor.app.service

import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.distanceTo

class AutoAttackService(
    private val calculator: CombatStatCalculator = CombatStatCalculator()
) {
    companion object {
        const val ATTACK_RANGE = 120f
    }

    /**
     * 1틱마다 호출. 범위 내 가장 가까운 몬스터를 공격하고 결과를 반환합니다.
     */
    fun tick(
        player: Player,
        equipment: Equipment?,
        monsters: List<Monster>
    ): AutoAttackResult {
        val target = findTarget(player, monsters)
            ?: return AutoAttackResult(monsters, targetId = null, damage = 0)

        val damage = calculateDamage(player, equipment)

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

    private fun calculateDamage(player: Player, equipment: Equipment?): Int {
        val provider = object : EquipmentStatProvider {
            override val weaponAttackPower = player.weapon.attackPower
            override val gloveAttackPower  = equipment?.attackPower ?: 0
            override val weaponMagicPower  = player.weapon.magicPower
            override val gloveMagicPower   = 0
        }
        val stats = calculator.calculate(player.job, player.stats, provider)
        return maxOf(stats.attackPower, stats.magicPower, 1)
    }
}

data class AutoAttackResult(
    val updatedMonsters: List<Monster>,
    val targetId: Int?,
    val damage: Int,
    val killedMonsters: List<Monster> = emptyList()
)
