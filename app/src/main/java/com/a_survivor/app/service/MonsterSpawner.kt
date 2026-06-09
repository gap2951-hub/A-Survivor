package com.a_survivor.app.service

import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.distanceTo
import com.a_survivor.app.model.skeletonWarrior
import kotlin.random.Random

class MonsterSpawner {

    fun spawnMonsters(
        world: GameWorld,
        count: Int,
        monsterId: String = "",
        variant: Int = 1,
        hp: Int = 30,
        expReward: Int = 10,
        avoidability: Int = 8,
        accuracy: Int = 18,
        speed: Float = 1.2f,
        isBlocked: ((Float, Float) -> Boolean)? = null,
        minDistance: Float = 100f,
        margin: Float = 200f,
        random: Random = Random.Default
    ): List<Monster> {
        val placed = mutableListOf<Monster>()
        var nextId = 1

        repeat(count) {
            val monster = tryPlace(
                world, placed, isBlocked, minDistance, margin, nextId,
                monsterId, variant, hp, expReward, avoidability, accuracy, speed, random
            )
            if (monster != null) {
                placed.add(monster)
                nextId++
            }
        }
        return placed
    }

    private fun tryPlace(
        world: GameWorld,
        placed: List<Monster>,
        isBlocked: ((Float, Float) -> Boolean)?,
        minDistance: Float,
        margin: Float,
        id: Int,
        monsterId: String,
        variant: Int,
        hp: Int,
        expReward: Int,
        avoidability: Int,
        accuracy: Int,
        speed: Float,
        random: Random,
        maxAttempts: Int = 100
    ): Monster? {
        val xRange = margin..(world.width - margin)
        val yRange = margin..(world.height - margin)

        repeat(maxAttempts) {
            val x = random.nextFloat() * (xRange.endInclusive - xRange.start) + xRange.start
            val y = random.nextFloat() * (yRange.endInclusive - yRange.start) + yRange.start

            val tooClose = placed.any { it.distanceTo(x, y) < minDistance }
            val blocked  = isBlocked?.invoke(x, y) == true
            if (!tooClose && !blocked) return skeletonWarrior(
                id, x, y, variant, hp, expReward, avoidability, accuracy, speed, monsterId
            )
        }
        return null
    }
}
