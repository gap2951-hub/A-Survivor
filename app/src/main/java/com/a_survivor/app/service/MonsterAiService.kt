package com.a_survivor.app.service

import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.MonsterState
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.distanceTo
import kotlin.math.sqrt

class MonsterAiService {

    companion object {
        const val MOVE_SPEED      = 1.2f
        const val ATTACK_RANGE    = 35f
        const val ATTACK_DAMAGE   = 5
        const val ATTACK_INTERVAL = 1000L
        const val DEAGGRO_RANGE   = 500f
    }

    fun tick(
        monsters: List<Monster>,
        player: Player,
        currentTime: Long,
        world: GameWorld,
        isBlocked: (Float, Float) -> Boolean
    ): MonsterAiResult {
        var totalPlayerDamage = 0

        val updated = monsters.map { m ->
            if (m.state == MonsterState.IDLE) return@map m

            val dist = m.distanceTo(player.positionX, player.positionY)

            when {
                // 어그로 해제
                dist > DEAGGRO_RANGE -> m.copy(state = MonsterState.IDLE)

                // 공격 범위 내 → 공격 시도
                dist <= ATTACK_RANGE -> {
                    val canAttack = currentTime - m.lastAttackTime >= ATTACK_INTERVAL
                    if (canAttack) totalPlayerDamage += ATTACK_DAMAGE
                    m.copy(
                        state          = MonsterState.ATTACKING,
                        lastAttackTime = if (canAttack) currentTime else m.lastAttackTime
                    )
                }

                // 추적 이동 (벽 슬라이딩 적용)
                else -> {
                    val dx = player.positionX - m.positionX
                    val dy = player.positionY - m.positionY
                    val len = sqrt(dx * dx + dy * dy)
                    val dirX = dx / len
                    val dirY = dy / len

                    val rawX = (m.positionX + dirX * MOVE_SPEED).coerceIn(0f, world.width)
                    val rawY = (m.positionY + dirY * MOVE_SPEED).coerceIn(0f, world.height)

                    val newX = if (!isBlocked(rawX, m.positionY)) rawX else m.positionX
                    val newY = if (!isBlocked(newX, rawY))        rawY else m.positionY

                    m.copy(positionX = newX, positionY = newY, state = MonsterState.AGGRO)
                }
            }
        }

        return MonsterAiResult(updated, totalPlayerDamage)
    }
}

data class MonsterAiResult(
    val updatedMonsters: List<Monster>,
    val playerDamage: Int
)
