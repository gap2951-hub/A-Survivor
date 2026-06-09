package com.a_survivor.app.service

import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.Projectile
import com.a_survivor.app.model.Skill
import com.a_survivor.app.model.SkillEffect
import com.a_survivor.app.model.SkillEffectType
import com.a_survivor.app.model.SkillType
import com.a_survivor.app.model.projectileSpeed
import com.a_survivor.app.model.projectileType
import kotlin.math.sqrt

data class SkillHit(val monsterId: Int, val damage: Int)

data class SkillResult(
    val hits: List<SkillHit>,
    val newProjectiles: List<Projectile>,
    val effect: SkillEffect?
)

class SkillService {

    fun execute(
        skill: Skill,
        player: Player,
        monsters: List<Monster>,
        derivedStats: DerivedStats,
        nextProjectileId: Int,
        nextEffectId: Int
    ): SkillResult {
        val now = System.currentTimeMillis()
        return when (skill.type) {
            SkillType.MELEE_BURST -> meleeBurst(skill, player, monsters, derivedStats, nextEffectId, now)
            SkillType.AOE         -> aoe(skill, player, monsters, derivedStats, nextEffectId, now)
            SkillType.MULTI_SHOT  -> multiShot(skill, player, monsters, derivedStats, nextProjectileId, nextEffectId, now)
        }
    }

    private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx; val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    private fun meleeBurst(
        skill: Skill, player: Player, monsters: List<Monster>,
        derivedStats: DerivedStats, nextEffectId: Int, now: Long
    ): SkillResult {
        val damage = (derivedStats.attackPower * skill.damageMultiplier).toInt().coerceAtLeast(1)
        val hits = monsters
            .filter { it.hp > 0 && dist(it.positionX, it.positionY, player.positionX, player.positionY) <= skill.range }
            .map { SkillHit(it.id, damage) }
        if (hits.isEmpty()) return SkillResult(emptyList(), emptyList(), null)
        val effect = SkillEffect(nextEffectId, SkillEffectType.SLASH_BURST, player.positionX, player.positionY, skill.range, now)
        return SkillResult(hits, emptyList(), effect)
    }

    private fun aoe(
        skill: Skill, player: Player, monsters: List<Monster>,
        derivedStats: DerivedStats, nextEffectId: Int, now: Long
    ): SkillResult {
        val target = monsters
            .filter { it.hp > 0 && dist(it.positionX, it.positionY, player.positionX, player.positionY) <= skill.range }
            .minByOrNull { dist(it.positionX, it.positionY, player.positionX, player.positionY) }
            ?: return SkillResult(emptyList(), emptyList(), null)
        val base = if (derivedStats.magicPower > 0) derivedStats.magicPower else derivedStats.attackPower
        val damage = (base * skill.damageMultiplier).toInt().coerceAtLeast(1)
        val hits = monsters
            .filter { it.hp > 0 && dist(it.positionX, it.positionY, target.positionX, target.positionY) <= skill.aoeRadius }
            .map { SkillHit(it.id, damage) }
        val effect = SkillEffect(nextEffectId, SkillEffectType.EXPLOSION, target.positionX, target.positionY, skill.aoeRadius, now)
        return SkillResult(hits, emptyList(), effect)
    }

    private fun multiShot(
        skill: Skill, player: Player, monsters: List<Monster>,
        derivedStats: DerivedStats, nextProjectileId: Int, nextEffectId: Int, now: Long
    ): SkillResult {
        val damage = (derivedStats.attackPower * skill.damageMultiplier).toInt().coerceAtLeast(1)
        val projType = player.job.projectileType() ?: return SkillResult(emptyList(), emptyList(), null)
        val speed = player.job.projectileSpeed()
        val targets = monsters
            .filter { it.hp > 0 && dist(it.positionX, it.positionY, player.positionX, player.positionY) <= skill.range }
            .sortedBy { dist(it.positionX, it.positionY, player.positionX, player.positionY) }
            .take(skill.shotCount)
        if (targets.isEmpty()) return SkillResult(emptyList(), emptyList(), null)
        val projectiles = targets.mapIndexed { i, t ->
            Projectile(
                id                = nextProjectileId + i,
                type              = projType,
                positionX         = player.positionX,
                positionY         = player.positionY,
                targetX           = t.positionX,
                targetY           = t.positionY,
                speed             = speed,
                damage            = damage,
                maxTravelDistance = skill.range * 1.5f,
                targetMonsterId   = t.id
            )
        }
        val effect = SkillEffect(nextEffectId, SkillEffectType.SLASH_BURST, player.positionX, player.positionY, 35f, now)
        return SkillResult(emptyList(), projectiles, effect)
    }
}
