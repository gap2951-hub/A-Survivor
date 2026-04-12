package com.a_survivor.app.game.systems

import com.a_survivor.app.data.EnemyData
import com.a_survivor.app.data.Vector2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object EnemySpawner {
    private var nextId = 0
    private var spawnTimer = 0f

    // 생존 시간에 따라 스폰 간격 단축 (최소 0.4초)
    private fun spawnInterval(t: Float) = max(0.4f, 2.0f - t / 60f)

    // 생존 시간에 따라 적 능력치 강화
    private fun enemyStats(t: Float): Pair<Float, Float> {
        val scale = 1f + t / 60f
        return Pair(50f * scale, min(70f + t / 20f, 160f))
    }

    fun update(
        deltaTime: Float,
        survivalTime: Float,
        playerPos: Vector2,
        screenW: Float,
        screenH: Float
    ): List<EnemyData> {
        spawnTimer += deltaTime
        if (spawnTimer < spawnInterval(survivalTime)) return emptyList()
        spawnTimer = 0f

        // 시간이 지날수록 한 번에 더 많이 스폰 (최대 4마리)
        val count = min(1 + (survivalTime / 30f).toInt(), 4)
        val (hp, speed) = enemyStats(survivalTime)

        return List(count) {
            EnemyData(
                id       = nextId++,
                position = spawnPosition(playerPos, screenW, screenH),
                hp       = hp,
                maxHp    = hp,
                moveSpeed = speed
            )
        }
    }

    /** 플레이어 주변 화면 밖에서 무작위 방향으로 스폰 */
    private fun spawnPosition(center: Vector2, w: Float, h: Float): Vector2 {
        val angle  = Random.nextFloat() * 2f * Math.PI.toFloat()
        val radius = max(w, h) * 0.65f
        return Vector2(
            (center.x + cos(angle) * radius).coerceIn(0f, w),
            (center.y + sin(angle) * radius).coerceIn(0f, h)
        )
    }

    fun reset() {
        nextId = 0
        spawnTimer = 0f
    }
}
