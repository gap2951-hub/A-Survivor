package com.a_survivor.app.service

import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.Scroll
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
        if (equipment.remainingUpgradeCount <= 0) {
            return equipment to EnhancementResult.Error("업그레이드 가능 횟수가 부족합니다.")
        }
        val roll = Random.nextInt(1, 101)
        return if (roll <= scroll.successRate) {
            val updated = equipment.copy(
                attackPower = equipment.attackPower + scroll.attackBonus,
                remainingUpgradeCount = equipment.remainingUpgradeCount - 1
            )
            updated to EnhancementResult.Success(
                "${scroll.name} 성공!\n공격력 +${scroll.attackBonus} 증가"
            )
        } else {
            val updated = equipment.copy(
                remainingUpgradeCount = equipment.remainingUpgradeCount - 1,
                failedUpgradeCount = equipment.failedUpgradeCount + 1
            )
            updated to EnhancementResult.Failure(
                "${scroll.name} 실패!\n공격력은 증가하지 않았습니다."
            )
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
