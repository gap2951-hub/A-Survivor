package com.a_survivor.app.service

import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.PlayerStats

/**
 * 장비로부터 공격/마력 수치를 집계하는 인터페이스.
 * 향후 무기·마력 장갑 등이 추가될 때 구현체만 교체하면 됩니다.
 */
interface EquipmentStatProvider {
    val weaponAttackPower: Int
    val gloveAttackPower: Int
    val weaponMagicPower: Int
    val gloveMagicPower: Int
}

class CombatStatCalculator {

    /**
     * 직업과 스탯, 장비 수치를 받아 최종 공격력(또는 마력)을 반환합니다.
     * 마법사는 magicPower, 나머지 직업은 attackPower 필드로 결과가 담깁니다.
     */
    fun calculate(
        job: PlayerJob,
        stats: PlayerStats,
        equipment: EquipmentStatProvider
    ): CombatStats = when (job) {
        PlayerJob.WARRIOR -> CombatStats(
            attackPower = equipment.weaponAttackPower + equipment.gloveAttackPower + (stats.str * 0.5).toInt()
        )
        PlayerJob.ARCHER -> CombatStats(
            attackPower = equipment.weaponAttackPower + equipment.gloveAttackPower + (stats.dex * 0.5).toInt()
        )
        PlayerJob.THIEF -> CombatStats(
            attackPower = equipment.weaponAttackPower + equipment.gloveAttackPower + (stats.luk * 0.5).toInt()
        )
        PlayerJob.MAGE -> CombatStats(
            magicPower = equipment.weaponMagicPower + equipment.gloveMagicPower + (stats.`int` * 0.5).toInt(),
            // 마력 장갑이 없는 현재는 공격력 장갑 수치를 임시로 합산
            attackPower = equipment.gloveAttackPower
        )
        PlayerJob.BEGINNER -> CombatStats(
            attackPower = equipment.weaponAttackPower + equipment.gloveAttackPower + (stats.str * 0.5).toInt()
        )
        PlayerJob.PIRATE -> CombatStats(
            attackPower = equipment.weaponAttackPower + equipment.gloveAttackPower + (stats.dex * 0.5).toInt()
        )
    }
}

data class CombatStats(
    val attackPower: Int = 0,
    val magicPower: Int = 0
)
