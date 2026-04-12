package com.a_survivor.app.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.a_survivor.app.data.*
import com.a_survivor.app.game.systems.*
import com.a_survivor.app.persistence.MetaProgression
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val meta = MetaProgression(application)
    private var gameLoop: Job? = null

    // D-pad 입력 (-1f ~ 1f)
    var inputX = 0f
    var inputY = 0f

    // ── 게임 시작 ─────────────────────────────────────────
    fun startGame(screenWidth: Float, screenHeight: Float) {
        val metaStats   = meta.load()
        val basePlayer  = PlayerData(position = Vector2(screenWidth / 2f, screenHeight / 2f))
        val player      = meta.applyToPlayer(basePlayer, metaStats)

        EnemySpawner.reset()
        CombatSystem.reset()

        _state.value = GameState(
            player       = player,
            screenWidth  = screenWidth,
            screenHeight = screenHeight
        )
        startLoop()
    }

    // ── 게임 루프 (~60fps) ────────────────────────────────
    private fun startLoop() {
        gameLoop?.cancel()
        gameLoop = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (true) {
                val now   = System.currentTimeMillis()
                val delta = ((now - lastTime) / 1000f).coerceAtMost(0.05f)
                lastTime  = now

                if (_state.value.phase == GamePhase.PLAYING) {
                    _state.value = tick(_state.value, delta)
                }
                delay(16L)
            }
        }
    }

    // ── 한 프레임 처리 ────────────────────────────────────
    private fun tick(s: GameState, dt: Float): GameState {
        var player      = s.player
        var enemies     = s.enemies
        var projectiles = s.projectiles

        // 1. 플레이어 이동
        player = movePlayer(player, dt, s.screenWidth, s.screenHeight)

        // 2. 적 → 플레이어 방향으로 이동
        enemies = enemies.map { e ->
            val dir = (player.position - e.position).normalized()
            e.copy(position = e.position + dir * e.moveSpeed * dt)
        }

        // 3. 새 적 스폰
        enemies = enemies + EnemySpawner.update(dt, s.survivalTime, player.position, s.screenWidth, s.screenHeight)

        // 4. 자동 공격 (투사체 발사)
        val (attackedPlayer, newProjectiles) = CombatSystem.updateAttack(player, enemies, projectiles, dt)
        player      = attackedPlayer
        projectiles = newProjectiles

        // 5. 투사체 이동 및 충돌
        val (aliveProj, damagedEnemies) = CombatSystem.updateProjectiles(projectiles, enemies, dt)
        projectiles = aliveProj
        enemies     = damagedEnemies

        // 6. 접촉 피해
        val (damagedPlayer, updatedEnemies) = CombatSystem.checkContactDamage(player, enemies, dt)
        player  = damagedPlayer
        enemies = updatedEnemies

        // 7. 죽은 적 정리 + 경험치 획득
        val dead     = enemies.filter { it.hp <= 0f }
        val totalExp = dead.sumOf { it.expDrop.toDouble() }.toFloat()
        enemies      = enemies.filter { it.hp > 0f }

        var leveledUp = false
        if (totalExp > 0f) {
            val (expPlayer, didLevel) = ExperienceSystem.addExp(player, totalExp)
            player    = expPlayer
            leveledUp = didLevel
        }

        // 8. 게임 오버 체크
        if (player.hp <= 0f) {
            val score     = (s.survivalTime * 10).toInt()
            val metaStats = meta.load()
            meta.save(metaStats.copy(currency = metaStats.currency + score / 10))
            return s.copy(phase = GamePhase.GAME_OVER, player = player.copy(hp = 0f), score = score)
        }

        // 9. 레벨업 → 업그레이드 선택 화면
        if (leveledUp) {
            return s.copy(
                phase          = GamePhase.UPGRADE_SELECTION,
                player         = player,
                enemies        = enemies,
                projectiles    = projectiles,
                survivalTime   = s.survivalTime + dt,
                score          = s.score + dead.size * 10,
                upgradeChoices = UpgradeSystem.getRandomChoices()
            )
        }

        return s.copy(
            player       = player,
            enemies      = enemies,
            projectiles  = projectiles,
            survivalTime = s.survivalTime + dt,
            score        = s.score + dead.size * 10
        )
    }

    // ── D-pad 입력 기반 플레이어 이동 ─────────────────────
    private fun movePlayer(p: PlayerData, dt: Float, w: Float, h: Float): PlayerData {
        val newX = (p.position.x + inputX * p.moveSpeed * dt).coerceIn(30f, w - 30f)
        val newY = (p.position.y + inputY * p.moveSpeed * dt).coerceIn(30f, h - 30f)
        return p.copy(position = Vector2(newX, newY))
    }

    // ── 업그레이드 선택 ───────────────────────────────────
    fun selectUpgrade(upgrade: com.a_survivor.app.data.Upgrade) {
        val s = _state.value
        _state.value = s.copy(
            phase          = GamePhase.PLAYING,
            player         = upgrade.effect(s.player),
            upgradeChoices = emptyList()
        )
    }

    // ── 재시작 ────────────────────────────────────────────
    fun restartGame() {
        startGame(_state.value.screenWidth, _state.value.screenHeight)
    }

    override fun onCleared() {
        gameLoop?.cancel()
        super.onCleared()
    }
}
