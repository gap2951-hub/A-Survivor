package com.a_survivor.app.game.systems

import com.a_survivor.app.data.Upgrade
import com.a_survivor.app.data.PlayerData
import kotlin.math.min

object UpgradeSystem {

    val allUpgrades: List<Upgrade> = listOf(
        Upgrade(
            id = "atk_speed",
            name = "공격 속도 ↑",
            description = "공격 속도 +25%"
        ) { p -> p.copy(attacksPerSecond = p.attacksPerSecond * 1.25f) },

        Upgrade(
            id = "damage",
            name = "공격력 ↑",
            description = "공격력 +20"
        ) { p -> p.copy(attackDamage = p.attackDamage + 20f) },

        Upgrade(
            id = "move_speed",
            name = "이동 속도 ↑",
            description = "이동 속도 +15%"
        ) { p -> p.copy(moveSpeed = p.moveSpeed * 1.15f) },

        Upgrade(
            id = "max_hp",
            name = "최대 체력 ↑",
            description = "최대 체력 +50, 체력 회복"
        ) { p ->
            val newMax = p.maxHp + 50f
            p.copy(maxHp = newMax, hp = min(p.hp + 50f, newMax))
        },

        Upgrade(
            id = "atk_range",
            name = "공격 범위 ↑",
            description = "공격 범위 +60"
        ) { p -> p.copy(attackRange = p.attackRange + 60f) },

        Upgrade(
            id = "heal",
            name = "긴급 회복",
            description = "현재 체력 30% 회복"
        ) { p -> p.copy(hp = min(p.hp + p.maxHp * 0.3f, p.maxHp)) }
    )

    fun getRandomChoices(count: Int = 3): List<Upgrade> =
        allUpgrades.shuffled().take(count)
}
