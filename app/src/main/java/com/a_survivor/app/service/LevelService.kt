package com.a_survivor.app.service

import com.a_survivor.app.model.Player

class LevelService {

    /** 해당 레벨에서 레벨업에 필요한 경험치 */
    fun requiredExp(level: Int): Int = level * 20

    /**
     * 획득 경험치를 적용하고 조건 충족 시 레벨업을 반복 처리합니다.
     * 레벨업 1회당 SP +5.
     */
    fun applyExp(player: Player, gainedExp: Int): Player {
        var p = player.copy(exp = player.exp + gainedExp)
        while (p.exp >= requiredExp(p.level)) {
            p = p.copy(
                exp                = p.exp - requiredExp(p.level),
                level              = p.level + 1,
                availableStatPoint = p.availableStatPoint + 5,
                hp                 = p.maxHp
            )
        }
        return p
    }
}
