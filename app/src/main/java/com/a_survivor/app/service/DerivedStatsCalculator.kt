package com.a_survivor.app.service

import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.PlayerStats
import com.a_survivor.app.model.Weapon

class DerivedStatsCalculator {

    fun calculate(
        job: PlayerJob,
        stats: PlayerStats,
        weapon: Weapon?,
        equipment: Equipment?
    ): DerivedStats {
        // 1단계: 장비 기본 스탯 보정 합산
        val totalSTR = stats.str + (weapon?.strBonus ?: 0) + (equipment?.strBonus ?: 0)
        val totalDEX = stats.dex + (equipment?.dexBonus ?: 0)
        val totalINT = stats.`int` + (equipment?.intBonus ?: 0)
        val totalLUK = stats.luk + (equipment?.lukBonus ?: 0)

        // 2단계: 장비 공격력/마력 합산
        val equipAtk = (weapon?.attackPower ?: 0) + (equipment?.attackPower ?: 0)
        val equipMag = (weapon?.magicPower ?: 0) + (equipment?.magicPower ?: 0)

        // 3단계: 직업별 스탯 → 전투 능력치 (기본값)
        val baseAtk: Int
        val baseMag: Int
        val baseAcc: Int
        val baseAvoid: Int

        when (job) {
            PlayerJob.WARRIOR -> {
                // STR 주효과, DEX 보조
                baseAtk   = equipAtk + (totalSTR * 0.5).toInt() + (totalDEX * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = (totalLUK * 0.5).toInt()
            }
            PlayerJob.MAGE -> {
                // INT 주효과, LUK 보조
                baseAtk   = 0
                baseMag   = equipMag + (totalINT * 0.5).toInt() + (totalLUK * 0.1).toInt()
                baseAcc   = 10 + totalLUK * 2 + totalDEX
                baseAvoid = totalLUK + (totalDEX * 0.2).toInt()
            }
            PlayerJob.ARCHER -> {
                // DEX 주효과, STR 보조
                baseAtk   = equipAtk + (totalDEX * 0.5).toInt() + (totalSTR * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = (totalLUK * 0.5).toInt()
            }
            PlayerJob.THIEF -> {
                // LUK 주효과, DEX 보조
                baseAtk   = equipAtk + (totalLUK * 0.5).toInt() + (totalDEX * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = totalLUK
            }
            PlayerJob.PIRATE -> {
                // DEX 주효과, STR 보조
                baseAtk   = equipAtk + (totalDEX * 0.5).toInt() + (totalSTR * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = (totalLUK * 0.5).toInt()
            }
            PlayerJob.BEGINNER -> {
                baseAtk   = equipAtk + (totalSTR * 0.5).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX + totalLUK
                baseAvoid = (totalLUK * 0.3).toInt()
            }
        }

        // 4단계: 장비 전용 능력치 합산
        return DerivedStats(
            attackPower     = baseAtk,
            magicPower      = baseMag,
            accuracy        = baseAcc + (equipment?.accuracy ?: 0),
            avoidability    = baseAvoid + (equipment?.avoidability ?: 0),
            physicalDefense = equipment?.physicalDefense ?: 0,
            magicDefense    = equipment?.magicDefense ?: 0,
            criticalRate    = equipment?.criticalRate ?: 0f,
            moveSpeed       = equipment?.moveSpeed ?: 0f,
            attackSpeed     = equipment?.attackSpeed ?: 0f
        )
    }

    fun calculate(player: Player, equipment: Equipment?): DerivedStats =
        calculate(player.job, player.stats, player.weapon, equipment)
}
