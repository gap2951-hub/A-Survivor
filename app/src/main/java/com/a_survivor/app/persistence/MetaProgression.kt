package com.a_survivor.app.persistence

import android.content.Context
import com.a_survivor.app.data.MetaStats
import com.a_survivor.app.data.PlayerData
import kotlin.math.min

/** SharedPreferences 기반 영구 메타 성장 저장/로드 */
class MetaProgression(context: Context) {
    private val prefs = context.getSharedPreferences("meta_progression", Context.MODE_PRIVATE)

    fun load(): MetaStats = MetaStats(
        bonusDamage    = prefs.getFloat("bonus_damage",     0f),
        bonusMaxHp     = prefs.getFloat("bonus_max_hp",     0f),
        bonusMoveSpeed = prefs.getFloat("bonus_move_speed", 0f),
        currency       = prefs.getInt("currency",           0)
    )

    fun save(stats: MetaStats) {
        prefs.edit()
            .putFloat("bonus_damage",     stats.bonusDamage)
            .putFloat("bonus_max_hp",     stats.bonusMaxHp)
            .putFloat("bonus_move_speed", stats.bonusMoveSpeed)
            .putInt("currency",           stats.currency)
            .apply()
    }

    /** 런 시작 시 메타 보너스를 플레이어 기본 스탯에 적용 */
    fun applyToPlayer(player: PlayerData, meta: MetaStats): PlayerData {
        val newMaxHp = player.maxHp + meta.bonusMaxHp
        return player.copy(
            attackDamage = player.attackDamage + meta.bonusDamage,
            maxHp        = newMaxHp,
            hp           = newMaxHp,
            moveSpeed    = player.moveSpeed + meta.bonusMoveSpeed
        )
    }
}
