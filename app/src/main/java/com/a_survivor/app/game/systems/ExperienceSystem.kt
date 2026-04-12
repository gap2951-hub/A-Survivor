package com.a_survivor.app.game.systems

import com.a_survivor.app.data.PlayerData

object ExperienceSystem {

    /**
     * 경험치 추가 및 레벨업 판정
     * 반환: (업데이트된 플레이어, 레벨업 여부)
     */
    fun addExp(player: PlayerData, amount: Float): Pair<PlayerData, Boolean> {
        val newExp = player.exp + amount
        return if (newExp >= player.expToNextLevel) {
            val overflow          = newExp - player.expToNextLevel
            val nextRequirement   = player.expToNextLevel * 1.5f
            player.copy(
                level           = player.level + 1,
                exp             = overflow,
                expToNextLevel  = nextRequirement
            ) to true
        } else {
            player.copy(exp = newExp) to false
        }
    }
}
