package com.a_survivor.app.service

import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.Projectile
import kotlin.math.sqrt

class ProjectileService {
    companion object {
        const val COLLISION_RADIUS = 16f
    }

    fun tick(projectiles: List<Projectile>, monsters: List<Monster>): ProjectileTickResult {
        val hitEvents = mutableListOf<ProjectileHitEvent>()
        val remaining = mutableListOf<Projectile>()

        for (proj in projectiles) {
            val dx   = proj.targetX - proj.positionX
            val dy   = proj.targetY - proj.positionY
            val dist = sqrt(dx * dx + dy * dy)

            val newX: Float
            val newY: Float
            val moved: Float
            if (dist <= proj.speed) {
                newX  = proj.targetX
                newY  = proj.targetY
                moved = dist
            } else {
                val r = proj.speed / dist
                newX  = proj.positionX + dx * r
                newY  = proj.positionY + dy * r
                moved = proj.speed
            }

            val newTraveled = proj.traveledDistance + moved
            if (newTraveled >= proj.maxTravelDistance) continue

            val hit = monsters.firstOrNull { m ->
                val mdx = m.positionX - newX
                val mdy = m.positionY - newY
                sqrt(mdx * mdx + mdy * mdy) <= COLLISION_RADIUS
            }

            if (hit != null) {
                hitEvents.add(ProjectileHitEvent(hit.id, proj.damage))
            } else {
                remaining.add(proj.copy(positionX = newX, positionY = newY, traveledDistance = newTraveled))
            }
        }

        return ProjectileTickResult(remaining, hitEvents)
    }
}

data class ProjectileTickResult(
    val updatedProjectiles: List<Projectile>,
    val hitEvents: List<ProjectileHitEvent>
)

data class ProjectileHitEvent(val monsterId: Int, val damage: Int)
