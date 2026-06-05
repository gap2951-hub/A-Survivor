package com.a_survivor.app.service

import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.distanceTo
import com.a_survivor.app.model.slime
import kotlin.random.Random

class MonsterSpawner {

    /**
     * 월드 안에 슬라임을 랜덤 위치에 스폰합니다.
     * 각 몬스터는 이미 배치된 몬스터로부터 minDistance 이상 떨어진 위치에 놓입니다.
     * maxAttempts 안에 조건을 만족하는 위치를 못 찾으면 해당 몬스터는 건너뜁니다.
     */
    fun spawnSlimes(
        world: GameWorld,
        count: Int,
        minDistance: Float = 100f,
        margin: Float = 50f,
        random: Random = Random.Default
    ): List<Monster> {
        val placed = mutableListOf<Monster>()
        var nextId = 1

        repeat(count) {
            val monster = tryPlace(world, placed, minDistance, margin, nextId, random)
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
        minDistance: Float,
        margin: Float,
        id: Int,
        random: Random,
        maxAttempts: Int = 50
    ): Monster? {
        val xRange = margin..(world.width - margin)
        val yRange = margin..(world.height - margin)

        repeat(maxAttempts) {
            val x = random.nextFloat() * (xRange.endInclusive - xRange.start) + xRange.start
            val y = random.nextFloat() * (yRange.endInclusive - yRange.start) + yRange.start

            val tooClose = placed.any { it.distanceTo(x, y) < minDistance }
            if (!tooClose) return slime(id, x, y)
        }
        return null
    }
}
