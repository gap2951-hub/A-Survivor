package com.a_survivor.app.service

import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.PlayerStats
import com.a_survivor.app.model.Weapon
import com.a_survivor.app.model.attackIntervalMs

class DerivedStatsCalculator {

    fun calculate(
        job: PlayerJob,
        stats: PlayerStats,
        weapon: Weapon?,
        glove: Equipment?,
        hat: Equipment? = null,
        top: Equipment? = null,
        shoes: Equipment? = null,
    ): DerivedStats {
        val all = listOfNotNull(glove, hat, top, shoes)

        val totalSTR = stats.str + (weapon?.strBonus ?: 0) + all.sumOf { it.strBonus }
        val totalDEX = stats.dex + all.sumOf { it.dexBonus }
        val totalINT = stats.`int` + all.sumOf { it.intBonus }
        val totalLUK = stats.luk + all.sumOf { it.lukBonus }

        val equipAtk = (weapon?.attackPower ?: 0) + all.sumOf { it.attackPower }
        val equipMag = (weapon?.magicPower ?: 0) + all.sumOf { it.magicPower }

        val baseAtk: Int
        val baseMag: Int
        val baseAcc: Int
        val baseAvoid: Int

        when (job) {
            PlayerJob.WARRIOR -> {
                baseAtk   = equipAtk + (totalSTR * 0.5).toInt() + (totalDEX * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = (totalLUK * 0.5).toInt()
            }
            PlayerJob.MAGE -> {
                baseAtk   = 0
                baseMag   = equipMag + (totalINT * 0.5).toInt() + (totalLUK * 0.1).toInt()
                baseAcc   = 10 + totalLUK * 2 + totalDEX
                baseAvoid = totalLUK + (totalDEX * 0.2).toInt()
            }
            PlayerJob.ARCHER -> {
                baseAtk   = equipAtk + (totalDEX * 0.5).toInt() + (totalSTR * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = (totalLUK * 0.5).toInt()
            }
            PlayerJob.THIEF -> {
                baseAtk   = equipAtk + (totalLUK * 0.5).toInt() + (totalDEX * 0.1).toInt()
                baseMag   = 0
                baseAcc   = 10 + totalDEX * 2
                baseAvoid = totalLUK
            }
            PlayerJob.PIRATE -> {
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

        val equipAtkSpeedBonus = all.sumOf { it.attackSpeed.toDouble() }.toFloat()
        val baseInterval = weapon?.attackIntervalMs() ?: 900L
        val attackIntervalMs = (baseInterval - equipAtkSpeedBonus.toLong()).coerceAtLeast(300L)

        return DerivedStats(
            attackPower      = baseAtk,
            magicPower       = baseMag,
            accuracy         = baseAcc + all.sumOf { it.accuracy },
            avoidability     = baseAvoid + all.sumOf { it.avoidability },
            physicalDefense  = all.sumOf { it.physicalDefense },
            magicDefense     = all.sumOf { it.magicDefense },
            criticalRate     = all.sumOf { it.criticalRate.toDouble() }.toFloat(),
            moveSpeed        = all.sumOf { it.moveSpeed.toDouble() }.toFloat(),
            attackSpeed      = equipAtkSpeedBonus,
            attackIntervalMs = attackIntervalMs
        )
    }

    fun calculate(
        player: Player,
        glove: Equipment?,
        hat: Equipment? = null,
        top: Equipment? = null,
        shoes: Equipment? = null,
    ): DerivedStats = calculate(player.job, player.stats, player.weapon, glove, hat, top, shoes)
}
