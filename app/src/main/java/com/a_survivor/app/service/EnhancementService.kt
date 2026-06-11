package com.a_survivor.app.service

import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Scroll
import com.a_survivor.app.model.Weapon
import kotlin.random.Random

class EnhancementService {

    fun applyScroll(equipment: Equipment, scroll: Scroll): Pair<Equipment, EnhancementResult> {
        if (equipment.destroyed) {
            return equipment to EnhancementResult.Error("장비가 파괴된 상태입니다.")
        }
        return if (scroll.isWhiteScroll) applyWhiteScroll(equipment, scroll)
        else applyNormalScroll(equipment, scroll)
    }

    private fun applyNormalScroll(
        equipment: Equipment,
        scroll: Scroll
    ): Pair<Equipment, EnhancementResult> {
        if (scroll.targetSlot.isNotEmpty() && equipment.slot != scroll.targetSlot) {
            val slotKo = slotName(scroll.targetSlot)
            return equipment to EnhancementResult.Error("이 주문서는 $slotKo 전용입니다.")
        }
        if (equipment.remainingUpgradeCount <= 0) {
            return equipment to EnhancementResult.Error("업그레이드 가능 횟수가 부족합니다.")
        }
        val roll = Random.nextInt(1, 101)
        return if (roll <= scroll.successRate) {
            val updated = equipment.copy(
                attackPower           = equipment.attackPower + scroll.attackBonus,
                magicPower            = equipment.magicPower  + scroll.magicBonus,
                strBonus              = equipment.strBonus    + scroll.strBonus,
                dexBonus              = equipment.dexBonus    + scroll.dexBonus,
                intBonus              = equipment.intBonus    + scroll.intBonus,
                lukBonus              = equipment.lukBonus    + scroll.lukBonus,
                remainingUpgradeCount = equipment.remainingUpgradeCount - 1
            )
            updated to EnhancementResult.Success("${scroll.name} 성공!\n${effectDesc(scroll)}")
        } else {
            val updated = equipment.copy(
                remainingUpgradeCount = equipment.remainingUpgradeCount - 1,
                failedUpgradeCount    = equipment.failedUpgradeCount + 1
            )
            updated to EnhancementResult.Failure("${scroll.name} 실패!")
        }
    }

    private fun effectDesc(scroll: Scroll): String = buildString {
        if (scroll.attackBonus > 0) appendLine("공격력 +${scroll.attackBonus}")
        if (scroll.magicBonus  > 0) appendLine("마력 +${scroll.magicBonus}")
        if (scroll.strBonus    > 0) appendLine("힘 +${scroll.strBonus}")
        if (scroll.dexBonus    > 0) appendLine("민첩 +${scroll.dexBonus}")
        if (scroll.intBonus    > 0) appendLine("지력 +${scroll.intBonus}")
        if (scroll.lukBonus    > 0) appendLine("행운 +${scroll.lukBonus}")
    }.trimEnd()

    private fun slotName(slot: String) = when (slot) {
        "GLOVE"  -> "장갑"
        "TOP"    -> "상의"
        "HAT"    -> "모자"
        "SHOES"  -> "신발"
        "WEAPON" -> "무기"
        else     -> slot
    }

    fun applyScrollToWeapon(weapon: Weapon, scroll: Scroll): Pair<Weapon, EnhancementResult> {
        if (weapon.destroyed) return weapon to EnhancementResult.Error("무기가 파괴된 상태입니다.")
        if (scroll.targetSlot.isNotEmpty() && scroll.targetSlot != "WEAPON")
            return weapon to EnhancementResult.Error("이 주문서는 무기 전용이 아닙니다.")
        if (weapon.remainingUpgradeCount <= 0)
            return weapon to EnhancementResult.Error("업그레이드 가능 횟수가 부족합니다.")
        val roll = Random.nextInt(1, 101)
        return if (roll <= scroll.successRate) {
            val updated = weapon.copy(
                attackPower           = weapon.attackPower + scroll.attackBonus,
                magicPower            = weapon.magicPower  + scroll.magicBonus,
                strBonus              = weapon.strBonus    + scroll.strBonus,
                remainingUpgradeCount = weapon.remainingUpgradeCount - 1
            )
            updated to EnhancementResult.Success("${scroll.name} 성공!\n${effectDesc(scroll)}")
        } else {
            val updated = weapon.copy(
                remainingUpgradeCount = weapon.remainingUpgradeCount - 1,
                failedUpgradeCount    = weapon.failedUpgradeCount + 1
            )
            updated to EnhancementResult.Failure("${scroll.name} 실패!")
        }
    }

    private fun applyWhiteScroll(
        equipment: Equipment,
        scroll: Scroll
    ): Pair<Equipment, EnhancementResult> {
        if (equipment.failedUpgradeCount <= 0) {
            return equipment to EnhancementResult.Error("복구할 실패 횟수가 없습니다.")
        }
        val roll = Random.nextInt(1, 101)
        return if (roll <= scroll.successRate) {
            val updated = equipment.copy(
                failedUpgradeCount = equipment.failedUpgradeCount - 1,
                remainingUpgradeCount = equipment.remainingUpgradeCount + 1
            )
            updated to EnhancementResult.Success("${scroll.name} 성공!\n실패 횟수 1회 복구")
        } else {
            val updated = equipment.copy(destroyed = true)
            updated to EnhancementResult.Destroyed("${scroll.name} 실패!\n장비가 파괴되었습니다.")
        }
    }
}
