package com.a_survivor.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.a_survivor.app.R
import com.a_survivor.app.model.ConsumableCatalog
import com.a_survivor.app.model.ConsumableType
import com.a_survivor.app.model.GameMessage
import com.a_survivor.app.model.MessageType
import com.a_survivor.app.model.DamageNumber
import com.a_survivor.app.model.DefaultWeapon
import com.a_survivor.app.model.DefaultWorld
import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.DialoguePage
import com.a_survivor.app.model.DialogueSession
import com.a_survivor.app.model.DropItem
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.GroundItem
import com.a_survivor.app.model.MapType
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.MonsterState
import com.a_survivor.app.model.Npc
import com.a_survivor.app.model.NpcRegistry
import com.a_survivor.app.model.NpcRole
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.PlayerStats
import com.a_survivor.app.model.Portal
import com.a_survivor.app.model.PortalRegistry
import com.a_survivor.app.model.Projectile
import com.a_survivor.app.model.QuestState
import com.a_survivor.app.model.QuestStatus
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.model.ShopInfo
import com.a_survivor.app.model.ShopItemType
import com.a_survivor.app.model.ShopRegistry
import com.a_survivor.app.model.ShopType
import com.a_survivor.app.model.StatType
import com.a_survivor.app.model.DropRegistry
import com.a_survivor.app.model.EquipmentRegistry
import com.a_survivor.app.model.MonsterRegistry
import com.a_survivor.app.model.QuestRegistry
import com.a_survivor.app.model.Skill
import com.a_survivor.app.model.SkillEffect
import com.a_survivor.app.model.SkillRegistry
import com.a_survivor.app.model.Weapon
import com.a_survivor.app.model.attackRange
import com.a_survivor.app.service.AutoAttackService
import com.a_survivor.app.service.SkillService
import com.a_survivor.app.service.DerivedStatsCalculator
import com.a_survivor.app.service.DropService
import com.a_survivor.app.service.EnhancementService
import com.a_survivor.app.service.LevelService
import com.a_survivor.app.service.MonsterAiService
import com.a_survivor.app.service.MonsterSpawner
import com.a_survivor.app.service.GameDataInitializer
import com.a_survivor.app.service.ProjectileService
import com.a_survivor.app.service.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class InventorySlot {
    data class ScrollItem(val type: ScrollType, val quantity: Int) : InventorySlot()
    data class EquipItem(val equipment: Equipment) : InventorySlot()
    data class ConsumableItem(val type: ConsumableType, val quantity: Int) : InventorySlot()
}

data class PendingRespawn(val monsterId: Int, val diedAt: Long)

data class PendingPlayerAttack(
    val targetId: Int,
    val damage: Int,
    val isMiss: Boolean,
    val applyAt: Long
)

data class UiState(
    val equipment: Equipment?,
    val weapon: Weapon?,
    val money: Int = 0,
    val inventorySlots: List<InventorySlot?> = List(32) { null },
    val selectedScrollType: ScrollType? = null,
    val lastResult: EnhancementResult? = null,
    val player: Player = Player(),
    val world: GameWorld = DefaultWorld,
    val monsters: List<Monster> = emptyList(),
    val groundItems: List<GroundItem> = emptyList(),
    val pendingRespawns: List<PendingRespawn> = emptyList(),
    val damageNumbers: List<DamageNumber> = emptyList(),
    val portals: List<Portal> = PortalRegistry.portalsFor(MapType.BEGINNER_FIELD),
    val lastTeleportAt: Long = 0L,
    val derivedStats: DerivedStats = DerivedStats(),
    val jobAdvancementPending: Boolean = false,
    val projectiles: List<Projectile> = emptyList(),
    val npcs: List<Npc> = emptyList(),
    val questState: QuestState = QuestState(),
    val activeDialogue: DialogueSession? = null,
    val playerAttackAnimStart: Long = 0L,
    val playerHurtAnimStart: Long = 0L,
    val pendingPlayerAttack: PendingPlayerAttack? = null,
    val playerDeathTime: Long = 0L,
    val activeShop: ShopInfo? = null,
    val messages: List<GameMessage> = emptyList(),
    val quickSlots: List<ConsumableType?> = List(3) { null },
    val skillCooldownUntil: Map<String, Long> = emptyMap(),
    val skillEffects: List<SkillEffect> = emptyList(),
    val autoAttackEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val service                = EnhancementService()
    private val autoAttackService      = AutoAttackService()
    private val monsterAiService       = MonsterAiService()
    private val levelService           = LevelService()
    private val dropService            = DropService()
    private val derivedStatsCalculator = DerivedStatsCalculator()
    private val projectileService      = ProjectileService()
    private val skillService           = SkillService()
    private val saveService            = com.a_survivor.app.service.SaveService(application.applicationContext)

    private var nextGroundItemId    = 0
    private var nextProjectileId    = 0
    private var nextEffectId        = 0
    private var nextMessageId       = 0L

    private fun addMessage(state: UiState, text: String, type: MessageType): UiState {
        val newMsg = GameMessage(id = nextMessageId++, text = text, type = type)
        viewModelScope.launch {
            delay(2000L)
            _uiState.update { s -> s.copy(messages = s.messages.filter { it.id != newMsg.id }) }
        }
        return state.copy(messages = (state.messages + newMsg).takeLast(5))
    }

    private val collisionBitmap: Bitmap? by lazy {
        val opts = BitmapFactory.Options().apply {
            inSampleSize      = 4
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(application.resources, R.drawable.map_beginner, opts)
    }
    private val townCollisionBitmap: Bitmap? by lazy {
        val opts = BitmapFactory.Options().apply {
            inSampleSize      = 4
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(application.resources, R.drawable.map_town, opts)
    }

    companion object {
        private const val MOVE_SPEED            = 2f
        private const val AUTO_ATTACK_CHECK_INTERVAL = 100L  // 공격 주기 체크 빈도 (실제 간격은 derivedStats.attackIntervalMs)
        private const val AI_TICK_INTERVAL      = 16L
        private const val RESPAWN_DELAY         = 5000L
        private const val RESPAWN_CHECK_INTERVAL = 1000L
        private const val DAMAGE_NUMBER_DURATION = 800L
        private const val PICKUP_RANGE          = 30f
        private const val PICKUP_DELAY          = 2000L
        private const val COLLISION_RADIUS      = 10f
        private const val LUMINANCE_THRESHOLD   = 80f
        private const val PORTAL_RANGE          = 30f
        private const val PORTAL_COOLDOWN       = 2000L
        private const val ATTACK_ANIM_DURATION  = 300L
    }

    init {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_ATTACK_CHECK_INTERVAL)
                autoAttackTick()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(AI_TICK_INTERVAL)
                monsterAiTick()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(AI_TICK_INTERVAL)
                projectileTick()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(RESPAWN_CHECK_INTERVAL)
                respawnTick()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(AI_TICK_INTERVAL)
                pendingAttackTick()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(AI_TICK_INTERVAL)
                skillEffectTick()
            }
        }
    }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var nextMonsterId      = (_uiState.value.monsters.maxOfOrNull { it.id } ?: 0) + 1
    private var nextDamageNumberId = 0

    // 저장된 데이터 로드 + 자동 저장
    init {
        // CSV 기반 게임 데이터 초기화 (최초 1회)
        GameDataInitializer.initialize(application)

        val initial = saveService.load()?.let { restoreState(it) } ?: createInitialState()
        _uiState.value = initial
        nextMonsterId = (initial.monsters.maxOfOrNull { it.id } ?: 0) + 1
        viewModelScope.launch {
            while (true) {
                delay(10_000L)
                saveService.save(_uiState.value)
            }
        }
    }

    fun saveNow() = saveService.save(_uiState.value)
    fun hasSave(): Boolean = saveService.hasSave()

    override fun onCleared() {
        super.onCleared()
        saveService.save(_uiState.value)
    }

    private fun restoreState(data: com.a_survivor.app.service.GameSaveData): UiState {
        val job       = runCatching { PlayerJob.valueOf(data.playerJob) }.getOrDefault(PlayerJob.BEGINNER)
        val mapType   = runCatching { MapType.valueOf(data.mapType) }.getOrDefault(MapType.BEGINNER_FIELD)
        val questStat = runCatching { QuestStatus.valueOf(data.questStatus) }.getOrDefault(QuestStatus.NOT_STARTED)

        val player = Player(
            level              = data.playerLevel,
            exp                = data.playerExp,
            hp                 = data.playerHp.coerceAtLeast(1),
            maxHp              = data.playerMaxHp,
            job                = job,
            stats              = PlayerStats(str = data.playerStr, dex = data.playerDex, `int` = data.playerInt, luk = data.playerLuk),
            availableStatPoint = data.playerStatPoints,
            weapon             = data.weapon ?: DefaultWeapon,
            positionX          = data.playerPosX,
            positionY          = data.playerPosY
        )

        val inventory = data.inventory
            .map { saveService.toInventorySlot(it) }
            .let { slots -> (slots + List(32) { null }).take(32) }

        val world = GameWorld(mapType = mapType)
        val base = UiState(
            player         = player,
            equipment      = data.equipment,
            weapon         = data.weapon,
            money          = data.money,
            inventorySlots = inventory,
            world          = world,
            monsters       = spawnSkeletons(world, 5),
            portals        = PortalRegistry.portalsFor(mapType),
            npcs           = NpcRegistry.npcsFor(mapType),
            questState     = QuestState(
                status    = questStat,
                killCount = data.questKillCount,
                killGoal  = data.questKillGoal
            )
        )
        return computeDerived(base)
    }

    fun startGame(job: PlayerJob = PlayerJob.BEGINNER) {
        val newState = createInitialState(job)
        _uiState.value = newState
        saveService.save(newState)
    }

    fun advanceJob(job: PlayerJob) {
        _uiState.update { state ->
            val beginnerBase   = PlayerJob.BEGINNER.initialStats()
            val currentStats   = state.player.stats
            val investedPoints = (currentStats.str + currentStats.dex + currentStats.`int` + currentStats.luk) -
                (beginnerBase.str + beginnerBase.dex + beginnerBase.`int` + beginnerBase.luk)

            val updatedPlayer = state.player.copy(
                job                = job,
                stats              = job.initialStats(),
                availableStatPoint = state.player.availableStatPoint + investedPoints.coerceAtLeast(0)
            )
            computeDerived(state.copy(
                player                = updatedPlayer,
                jobAdvancementPending = false
            ))
        }
    }

    private fun computeDerived(state: UiState): UiState =
        state.copy(derivedStats = derivedStatsCalculator.calculate(state.player, state.equipment))

    private fun createInitialState(job: PlayerJob = PlayerJob.BEGINNER): UiState {
        val initialPlayer = Player(job = job, stats = job.initialStats())
        val initialEquip  = EquipmentRegistry.get("NOGADA_GLOVE") ?: Equipment(
            name = "노가다 목장갑",
            attackPower = 0,
            maxUpgradeCount = 5,
            remainingUpgradeCount = 5,
            failedUpgradeCount = 0,
            destroyed = false,
            description = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
        )
        val questData = QuestRegistry.questForNpc(1)
        val base = UiState(
            monsters   = spawnSkeletons(DefaultWorld, 5),
            portals    = PortalRegistry.portalsFor(MapType.BEGINNER_FIELD),
            weapon     = DefaultWeapon,
            equipment  = initialEquip,
            player     = initialPlayer,
            questState = QuestState(killGoal = questData?.targetCount ?: 5)
        )
        return computeDerived(base)
    }

    // ── 충돌 판정 ──────────────────────────────────────────────────────────────

    private fun isBlocked(worldX: Float, worldY: Float, world: GameWorld): Boolean {
        val r = COLLISION_RADIUS
        return listOf(
            worldX        to worldY,
            worldX - r    to worldY,
            worldX + r    to worldY,
            worldX        to worldY - r,
            worldX        to worldY + r
        ).any { (x, y) ->
            isPixelBlocked(
                x.coerceIn(0f, world.width),
                y.coerceIn(0f, world.height),
                world
            )
        }
    }

    private fun isPixelBlocked(worldX: Float, worldY: Float, world: GameWorld): Boolean {
        val bmp = when (world.mapType) {
            MapType.TOWN -> townCollisionBitmap
            else         -> collisionBitmap
        } ?: return false
        val px = ((worldX / world.width)  * bmp.width ).toInt().coerceIn(0, bmp.width  - 1)
        val py = ((worldY / world.height) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val pixel = bmp.getPixel(px, py)
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return luminance < LUMINANCE_THRESHOLD
    }

    // ── 이동 ──────────────────────────────────────────────────────────────────

    fun movePlayer(dirX: Float, dirY: Float) {
        var teleportedMap: MapType? = null
        var pickedUp = false
        _uiState.update { state ->
            val p     = state.player
            val world = state.world

            val rawX = p.positionX + dirX * MOVE_SPEED
            val rawY = p.positionY + dirY * MOVE_SPEED
            val (clampedX, clampedY) = world.clampPosition(rawX, rawY)

            val newX = if (!isBlocked(clampedX, p.positionY, world)) clampedX else p.positionX
            val newY = if (!isBlocked(newX,     clampedY,    world)) clampedY else p.positionY

            val isAttacking = System.currentTimeMillis() - state.playerAttackAnimStart < ATTACK_ANIM_DURATION
            val newFacingLeft = if (isAttacking) p.facingLeft
                                else if (dirX < 0f) true
                                else if (dirX > 0f) false
                                else p.facingLeft
            val moved = state.copy(player = p.copy(positionX = newX, positionY = newY, facingLeft = newFacingLeft))
            val afterPickup = checkPickup(moved)
            if (afterPickup.groundItems.size < moved.groundItems.size) pickedUp = true

            val now = System.currentTimeMillis()
            if (now - afterPickup.lastTeleportAt < PORTAL_COOLDOWN) return@update afterPickup

            val nearPortal = afterPickup.portals.firstOrNull { portal ->
                val dx = newX - portal.worldX
                val dy = newY - portal.worldY
                sqrt(dx * dx + dy * dy) <= PORTAL_RANGE
            }
            if (nearPortal != null) {
                teleportedMap = nearPortal.targetMap
                teleportState(afterPickup, nearPortal, now)
            } else afterPickup
        }
        if (pickedUp) SoundManager.playSfx(SoundManager.Sfx.ITEM_PICKUP)
        teleportedMap?.let { map ->
            SoundManager.playSfx(SoundManager.Sfx.PORTAL)
            SoundManager.switchBgm(SoundManager.bgmForMap(map))
        }
    }

    private fun spawnSkeletons(world: GameWorld, count: Int): List<Monster> {
        val cfg = MonsterRegistry.configForMap(world.mapType) ?: return emptyList()
        return MonsterSpawner().spawnMonsters(
            world        = world,
            count        = count,
            monsterId    = cfg.monsterId,
            variant      = cfg.variant,
            hp           = cfg.hp,
            expReward    = cfg.exp,
            avoidability = cfg.avoidability,
            accuracy     = cfg.accuracy,
            speed        = cfg.speed,
            isBlocked    = { x, y -> isBlocked(x, y, world) }
        )
    }

    private fun teleportState(state: UiState, portal: Portal, now: Long): UiState {
        val targetWorld = GameWorld(mapType = portal.targetMap)
        val newMonsters = spawnSkeletons(targetWorld, 5)

        return state.copy(
            player          = state.player.copy(positionX = portal.targetX, positionY = portal.targetY),
            world           = targetWorld,
            monsters        = newMonsters,
            groundItems     = emptyList(),
            pendingRespawns = emptyList(),
            portals         = PortalRegistry.portalsFor(portal.targetMap),
            lastTeleportAt  = now,
            projectiles     = emptyList(),
            npcs            = NpcRegistry.npcsFor(portal.targetMap)
        )
    }

    // ── 자동 공격 틱 ───────────────────────────────────────────────────────────

    private fun autoAttackTick() {
        if (!_uiState.value.autoAttackEnabled) return
        executeAttack()
    }

    fun manualAttack() = executeAttack()

    fun toggleAutoAttack() {
        _uiState.update { it.copy(autoAttackEnabled = !it.autoAttackEnabled) }
    }

    private fun executeAttack() {
        var attacked = false
        _uiState.update { state ->
            if (state.player.hp <= 0) return@update state
            if (System.currentTimeMillis() - state.playerAttackAnimStart < state.derivedStats.attackIntervalMs) return@update state
            val lockedIds = state.projectiles.map { it.targetMonsterId }.toSet()
            val result = autoAttackService.tick(
                player           = state.player,
                equipment        = state.equipment,
                monsters         = state.monsters,
                derivedStats     = state.derivedStats,
                attackRange      = state.player.job.attackRange(),
                nextProjectileId = nextProjectileId,
                lockedMonsterIds = lockedIds
            )

            val now = System.currentTimeMillis()

            if (result.targetId == null) return@update state
            attacked = true

            val target = state.monsters.find { it.id == result.targetId }
            val newFacingLeft = target != null && target.positionX < state.player.positionX

            if (result.newProjectile != null) {
                nextProjectileId++
                return@update state.copy(
                    player               = state.player.copy(facingLeft = newFacingLeft),
                    projectiles          = state.projectiles + result.newProjectile,
                    playerAttackAnimStart = now
                )
            }

            if (state.pendingPlayerAttack != null) return@update state

            state.copy(
                player                = state.player.copy(facingLeft = newFacingLeft),
                playerAttackAnimStart = now,
                pendingPlayerAttack   = PendingPlayerAttack(
                    targetId = result.targetId,
                    damage   = result.damage,
                    isMiss   = result.isMiss,
                    applyAt  = now + ATTACK_ANIM_DURATION
                ),
                monsters = state.monsters.map { m ->
                    if (m.id == result.targetId && m.state == MonsterState.IDLE)
                        m.copy(state = MonsterState.AGGRO) else m
                }
            )
        }
        if (attacked) SoundManager.playSfx(SoundManager.Sfx.ATTACK)
    }

    // ── 대기 중인 공격 데미지 적용 ─────────────────────────────────────────────

    private fun pendingAttackTick() {
        var sfxToPlay: SoundManager.Sfx? = null
        _uiState.update { state ->
            val pending = state.pendingPlayerAttack ?: return@update state
            val now = System.currentTimeMillis()
            if (now < pending.applyAt) return@update state

            val baseState = state.copy(pendingPlayerAttack = null)

            if (pending.isMiss) {
                val targetMonster = state.monsters.find { it.id == pending.targetId }
                val missNum = targetMonster?.let {
                    DamageNumber(nextDamageNumberId++, 0,
                        it.positionX, it.positionY, now, isPlayerDamage = false, isMiss = true)
                }
                return@update baseState.copy(
                    damageNumbers = baseState.damageNumbers + listOfNotNull(missNum)
                )
            }

            val targetMonster = state.monsters.find { it.id == pending.targetId }
                ?: return@update baseState
            if (targetMonster.hp <= 0) return@update baseState

            val newHp         = targetMonster.hp - pending.damage
            val killed        = newHp <= 0
            sfxToPlay = if (killed) SoundManager.Sfx.MONSTER_DIE else SoundManager.Sfx.MONSTER_HIT
            val updatedMonsters = if (killed) {
                state.monsters.filter { it.id != pending.targetId }
            } else {
                state.monsters.map { if (it.id == pending.targetId) it.copy(hp = newHp) else it }
            }

            val killedList    = if (killed) listOf(targetMonster) else emptyList()
            val gainedExp     = killedList.sumOf { it.expReward }
            val updatedPlayer = if (gainedExp > 0) levelService.applyExp(state.player, gainedExp) else state.player
            if (updatedPlayer.level > state.player.level) sfxToPlay = SoundManager.Sfx.LEVEL_UP

            val newGroundItems = mutableListOf<GroundItem>()
            killedList.forEach { monster ->
                val drops = dropService.roll(
                    DropRegistry.dropEntriesFor(monster.monsterId, state.world.mapType)
                )
                drops.forEachIndexed { index, drop ->
                    val angle  = index * (Math.PI * 2.0 / drops.size.coerceAtLeast(1)).toFloat()
                    val spread = if (drops.size > 1) 20f else 0f
                    newGroundItems.add(GroundItem(
                        id = nextGroundItemId++,
                        positionX = monster.positionX + cos(angle) * spread,
                        positionY = monster.positionY + sin(angle) * spread,
                        dropItem  = drop, droppedAt = now
                    ))
                }
            }

            val attackDmgNum = if (pending.damage > 0) DamageNumber(
                nextDamageNumberId++, pending.damage,
                targetMonster.positionX, targetMonster.positionY, now, isPlayerDamage = false
            ) else null

            val advancePending = state.jobAdvancementPending || (
                gainedExp > 0 && updatedPlayer.job == PlayerJob.BEGINNER &&
                updatedPlayer.level >= 3 && state.player.level < 3
            )

            val questKillAdd   = if (state.questState.status == QuestStatus.IN_PROGRESS) killedList.size else 0
            val newKillCount   = (state.questState.killCount + questKillAdd)
            val newQuestStatus = if (state.questState.status == QuestStatus.IN_PROGRESS &&
                newKillCount >= state.questState.killGoal) QuestStatus.READY_TO_COMPLETE
                else state.questState.status
            val newQuestState  = state.questState.copy(
                killCount = newKillCount.coerceAtMost(state.questState.killGoal),
                status    = newQuestStatus
            )

            val finalState = baseState.copy(
                monsters              = updatedMonsters,
                player                = updatedPlayer,
                groundItems           = baseState.groundItems + newGroundItems,
                pendingRespawns       = baseState.pendingRespawns + killedList.map { PendingRespawn(it.id, now) },
                damageNumbers         = baseState.damageNumbers + listOfNotNull(attackDmgNum),
                jobAdvancementPending = advancePending,
                questState            = newQuestState
            )
            val msgsState = if (gainedExp > 0) addMessage(finalState, "+${gainedExp} EXP", MessageType.EXP) else finalState
            if (gainedExp > 0) computeDerived(msgsState) else msgsState
        }
        sfxToPlay?.let { SoundManager.playSfx(it) }
    }

    // ── 스킬 사용 ──────────────────────────────────────────────────────────────

    fun useSkill() {
        var sfxToPlay: SoundManager.Sfx? = null
        _uiState.update { state ->
            if (state.player.hp <= 0) return@update state
            val skill = SkillRegistry.skillFor(state.player.job)
            val now = System.currentTimeMillis()
            if (now < (state.skillCooldownUntil[skill.id] ?: 0L)) return@update state

            val result = skillService.execute(
                skill, state.player, state.monsters, state.derivedStats,
                nextProjectileId, nextEffectId
            )
            if (result.hits.isEmpty() && result.newProjectiles.isEmpty()) return@update state

            sfxToPlay = SoundManager.Sfx.ATTACK
            if (result.effect != null) nextEffectId++

            val newCooldowns = state.skillCooldownUntil + (skill.id to now + skill.cooldownMs)
            val newEffects   = state.skillEffects + listOfNotNull(result.effect)

            val firstTargetX = result.newProjectiles.firstOrNull()?.targetX
                ?: state.monsters.find { it.id == result.hits.firstOrNull()?.monsterId }?.positionX
            val newFacingLeft = firstTargetX?.let { it < state.player.positionX } ?: state.player.facingLeft

            // MULTI_SHOT: 투사체만 생성, 데미지는 projectileTick이 처리
            if (result.newProjectiles.isNotEmpty()) {
                nextProjectileId += result.newProjectiles.size
                return@update state.copy(
                    skillCooldownUntil   = newCooldowns,
                    skillEffects         = newEffects,
                    projectiles          = state.projectiles + result.newProjectiles,
                    playerAttackAnimStart = now,
                    player               = state.player.copy(facingLeft = newFacingLeft)
                )
            }

            // MELEE_BURST / AOE: 즉시 데미지 처리
            val hitMap = result.hits.groupBy { it.monsterId }
            val killed = mutableListOf<Monster>()
            val updatedMonsters = state.monsters.mapNotNull { m ->
                val totalDmg = hitMap[m.id]?.sumOf { it.damage } ?: 0
                if (totalDmg == 0) return@mapNotNull m
                val newHp = m.hp - totalDmg
                if (newHp <= 0) { killed.add(m); null } else m.copy(hp = newHp)
            }

            val gainedExp     = killed.sumOf { it.expReward }
            val updatedPlayer = (if (gainedExp > 0) levelService.applyExp(state.player, gainedExp) else state.player)
                .copy(facingLeft = newFacingLeft)
            if (updatedPlayer.level > state.player.level) sfxToPlay = SoundManager.Sfx.LEVEL_UP

            val newGroundItems = mutableListOf<GroundItem>()
            killed.forEach { monster ->
                val drops = dropService.roll(DropRegistry.dropEntriesFor(monster.monsterId, state.world.mapType))
                drops.forEachIndexed { i, drop ->
                    val angle  = i * (Math.PI * 2.0 / drops.size.coerceAtLeast(1)).toFloat()
                    val spread = if (drops.size > 1) 20f else 0f
                    newGroundItems.add(GroundItem(nextGroundItemId++,
                        monster.positionX + cos(angle) * spread,
                        monster.positionY + sin(angle) * spread,
                        drop, now))
                }
            }

            val dmgNums = result.hits.mapNotNull { hit ->
                val m = state.monsters.find { it.id == hit.monsterId } ?: return@mapNotNull null
                DamageNumber(nextDamageNumberId++, hit.damage, m.positionX, m.positionY, now, false)
            }

            val questKillAdd   = if (state.questState.status == QuestStatus.IN_PROGRESS) killed.size else 0
            val newKillCount   = (state.questState.killCount + questKillAdd).coerceAtMost(state.questState.killGoal)
            val newQuestStatus = if (state.questState.status == QuestStatus.IN_PROGRESS && newKillCount >= state.questState.killGoal)
                QuestStatus.READY_TO_COMPLETE else state.questState.status

            val advancePending = state.jobAdvancementPending || (
                gainedExp > 0 && updatedPlayer.job == PlayerJob.BEGINNER &&
                updatedPlayer.level >= 3 && state.player.level < 3
            )

            val base = state.copy(
                skillCooldownUntil    = newCooldowns,
                skillEffects          = newEffects,
                monsters              = updatedMonsters,
                player                = updatedPlayer,
                groundItems           = state.groundItems + newGroundItems,
                pendingRespawns       = state.pendingRespawns + killed.map { PendingRespawn(it.id, now) },
                damageNumbers         = state.damageNumbers + dmgNums,
                questState            = state.questState.copy(killCount = newKillCount, status = newQuestStatus),
                jobAdvancementPending = advancePending,
                playerAttackAnimStart = now
            )
            val msgsState = if (gainedExp > 0) addMessage(base, "+${gainedExp} EXP", MessageType.EXP) else base
            if (gainedExp > 0) computeDerived(msgsState) else msgsState
        }
        sfxToPlay?.let { SoundManager.playSfx(it) }
    }

    // ── 스킬 이펙트 정리 ──────────────────────────────────────────────────────

    private fun skillEffectTick() {
        _uiState.update { state ->
            if (state.skillEffects.isEmpty()) return@update state
            val now = System.currentTimeMillis()
            val remaining = state.skillEffects.filter { !it.isExpired(now) }
            if (remaining.size == state.skillEffects.size) return@update state
            state.copy(skillEffects = remaining)
        }
    }

    // ── 투사체 틱 ──────────────────────────────────────────────────────────────

    private fun projectileTick() {
        _uiState.update { state ->
            if (state.projectiles.isEmpty()) return@update state

            val now        = System.currentTimeMillis()
            val projResult = projectileService.tick(state.projectiles, state.monsters)
            if (projResult.hitEvents.isEmpty()) {
                return@update state.copy(projectiles = projResult.updatedProjectiles)
            }

            val damageByMonster = projResult.hitEvents.groupBy { it.monsterId }
            val killed          = mutableListOf<Monster>()
            val hitIds          = projResult.hitEvents.map { it.monsterId }.toSet()

            val updatedMonsters = state.monsters.mapNotNull { monster ->
                val totalDamage = damageByMonster[monster.id]?.sumOf { it.damage } ?: 0
                if (totalDamage == 0) return@mapNotNull monster
                val newHp = monster.hp - totalDamage
                if (newHp <= 0) { killed.add(monster); null }
                else monster.copy(hp = newHp)
            }.map { m ->
                if (m.id in hitIds && m.state == MonsterState.IDLE) m.copy(state = MonsterState.AGGRO) else m
            }

            val newDmgNums = projResult.hitEvents.mapNotNull { event ->
                val monster = state.monsters.find { it.id == event.monsterId } ?: return@mapNotNull null
                DamageNumber(
                    nextDamageNumberId++, event.damage,
                    monster.positionX, monster.positionY, now,
                    isPlayerDamage = false
                )
            }

            val gainedExp     = killed.sumOf { it.expReward }
            val updatedPlayer = if (gainedExp > 0) levelService.applyExp(state.player, gainedExp) else state.player

            val newGroundItems = mutableListOf<GroundItem>()
            killed.forEach { monster ->
                val drops = dropService.roll(
                    DropRegistry.dropEntriesFor(monster.monsterId, state.world.mapType)
                )
                drops.forEachIndexed { index, drop ->
                    val angle  = index * (Math.PI * 2.0 / drops.size.coerceAtLeast(1)).toFloat()
                    val spread = if (drops.size > 1) 20f else 0f
                    newGroundItems.add(GroundItem(
                        id        = nextGroundItemId++,
                        positionX = monster.positionX + cos(angle) * spread,
                        positionY = monster.positionY + sin(angle) * spread,
                        dropItem  = drop,
                        droppedAt = now
                    ))
                }
            }

            val newPending = killed.map { PendingRespawn(it.id, now) }

            val advancePending = state.jobAdvancementPending || (
                gainedExp > 0 &&
                updatedPlayer.job == PlayerJob.BEGINNER &&
                updatedPlayer.level >= 3 &&
                state.player.level < 3
            )

            val questKillAddProj = if (state.questState.status == QuestStatus.IN_PROGRESS) killed.size else 0
            val newKillCountProj = (state.questState.killCount + questKillAddProj)
            val newQuestStatusProj = if (state.questState.status == QuestStatus.IN_PROGRESS && newKillCountProj >= state.questState.killGoal)
                QuestStatus.READY_TO_COMPLETE else state.questState.status
            val newQuestStateProj = state.questState.copy(
                killCount = newKillCountProj.coerceAtMost(state.questState.killGoal),
                status = newQuestStatusProj
            )

            val newState = state.copy(
                projectiles           = projResult.updatedProjectiles,
                monsters              = updatedMonsters,
                player                = updatedPlayer,
                groundItems           = state.groundItems + newGroundItems,
                pendingRespawns       = state.pendingRespawns + newPending,
                damageNumbers         = state.damageNumbers + newDmgNums,
                jobAdvancementPending = advancePending,
                questState            = newQuestStateProj
            )
            val msgsState = if (gainedExp > 0) addMessage(newState, "+${gainedExp} EXP", MessageType.EXP) else newState
            if (gainedExp > 0) computeDerived(msgsState) else msgsState
        }
    }

    // ── 몬스터 리스폰 틱 ───────────────────────────────────────────────────────

    private fun respawnTick() {
        _uiState.update { state ->
            val now = System.currentTimeMillis()
            val (ready, waiting) = state.pendingRespawns.partition { now - it.diedAt >= RESPAWN_DELAY }
            if (ready.isEmpty()) return@update state

            val newMonsters = ready.mapNotNull {
                spawnSkeletons(state.world, 1).firstOrNull()?.copy(id = nextMonsterId++)
            }
            state.copy(monsters = state.monsters + newMonsters, pendingRespawns = waiting)
        }
    }

    private fun monsterAiTick() {
        var playerHit = false
        var pickedUp = false
        _uiState.update { state ->
            val now      = System.currentTimeMillis()
            val aiResult = monsterAiService.tick(
                monsters           = state.monsters,
                player             = state.player,
                currentTime        = now,
                world              = state.world,
                isBlocked          = { x, y -> isBlocked(x, y, state.world) },
                playerAvoidability = state.derivedStats.avoidability
            )
            val newHp = (state.player.hp - aiResult.playerDamage).coerceAtLeast(0)

            val playerDmgNum = when {
                aiResult.playerDamage > 0 -> DamageNumber(
                    nextDamageNumberId++, aiResult.playerDamage,
                    state.player.positionX, state.player.positionY, now,
                    isPlayerDamage = true
                )
                aiResult.playerDodged -> DamageNumber(
                    nextDamageNumberId++, 0,
                    state.player.positionX, state.player.positionY, now,
                    isPlayerDamage = true, isMiss = true
                )
                else -> null
            }

            val activeDmgNums = state.damageNumbers.filter { now - it.createdAt < DAMAGE_NUMBER_DURATION }

            val wasAlive    = state.player.hp > 0
            val justDied    = wasAlive && newHp == 0
            val newDeathTime  = if (justDied) now else state.playerDeathTime
            val newHurtStart  = if (aiResult.playerDamage > 0 && !justDied) now else state.playerHurtAnimStart
            if (aiResult.playerDamage > 0) playerHit = true

            val newState = state.copy(
                monsters            = aiResult.updatedMonsters,
                player              = state.player.copy(hp = newHp),
                damageNumbers       = activeDmgNums + listOfNotNull(playerDmgNum),
                playerHurtAnimStart = newHurtStart,
                playerDeathTime     = newDeathTime
            )
            val finalState = checkPickup(newState)
            if (finalState.groundItems.size < newState.groundItems.size) pickedUp = true
            finalState
        }
        if (playerHit) SoundManager.playSfx(SoundManager.Sfx.PLAYER_HIT)
        if (pickedUp) SoundManager.playSfx(SoundManager.Sfx.ITEM_PICKUP)
    }

    private fun checkPickup(state: UiState): UiState {
        val now = System.currentTimeMillis()
        val px  = state.player.positionX
        val py  = state.player.positionY
        val (toPickup, remaining) = state.groundItems.partition { item ->
            val dx = item.positionX - px
            val dy = item.positionY - py
            sqrt(dx * dx + dy * dy) <= PICKUP_RANGE && now - item.droppedAt >= PICKUP_DELAY
        }
        if (toPickup.isEmpty()) return state
        return applyDrops(state.copy(groundItems = remaining), toPickup.map { it.dropItem })
    }

    // ── 인벤토리 슬롯 헬퍼 ────────────────────────────────────────────────────

    private fun addScrollToSlots(slots: List<InventorySlot?>, type: ScrollType): List<InventorySlot?> {
        val m = slots.toMutableList()
        val idx = m.indexOfFirst { it is InventorySlot.ScrollItem && it.type == type }
        if (idx >= 0) {
            m[idx] = (m[idx] as InventorySlot.ScrollItem).let { it.copy(quantity = it.quantity + 1) }
        } else {
            val empty = m.indexOfFirst { it == null }
            if (empty >= 0) m[empty] = InventorySlot.ScrollItem(type, 1)
        }
        return m
    }

    private fun addEquipToSlots(slots: List<InventorySlot?>, equipment: Equipment): List<InventorySlot?> {
        val m = slots.toMutableList()
        val empty = m.indexOfFirst { it == null }
        if (empty >= 0) m[empty] = InventorySlot.EquipItem(equipment)
        return m
    }

    private fun addConsumableToSlots(slots: List<InventorySlot?>, type: ConsumableType): List<InventorySlot?> {
        val m = slots.toMutableList()
        val idx = m.indexOfFirst { it is InventorySlot.ConsumableItem && it.type == type }
        if (idx >= 0) {
            m[idx] = (m[idx] as InventorySlot.ConsumableItem).let { it.copy(quantity = it.quantity + 1) }
        } else {
            val empty = m.indexOfFirst { it == null }
            if (empty >= 0) m[empty] = InventorySlot.ConsumableItem(type, 1)
        }
        return m
    }

    private fun applyDrops(state: UiState, drops: List<DropItem>): UiState {
        var s = state
        for (drop in drops) {
            s = when (drop) {
                is DropItem.MoneyDrop -> addMessage(
                    s.copy(money = s.money + drop.amount),
                    "+${drop.amount}원", MessageType.MONEY
                )
                is DropItem.ScrollDrop -> addMessage(
                    s.copy(inventorySlots = addScrollToSlots(s.inventorySlots, drop.scrollType)),
                    "${ScrollCatalog.get(drop.scrollType).name} 획득", MessageType.ITEM
                )
                is DropItem.EquipmentDrop -> {
                    val withSlots = s.copy(inventorySlots = addEquipToSlots(s.inventorySlots, drop.equipment))
                    val withEquip = if (withSlots.equipment == null) withSlots.copy(equipment = drop.equipment) else withSlots
                    addMessage(withEquip, "${drop.equipment.name} 획득", MessageType.ITEM)
                }
            }
        }
        return s
    }

    // ── 강화 시스템 ────────────────────────────────────────────────────────────

    fun clearLastResult() {
        _uiState.update { it.copy(lastResult = null) }
    }

    fun selectScroll(scrollType: ScrollType) {
        _uiState.update { it.copy(selectedScrollType = scrollType, lastResult = null) }
    }

    fun useSelectedScroll() {
        val state     = _uiState.value
        val equipment = state.equipment ?: run {
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("장비가 장착되어 있지 않습니다.")) }
            return
        }
        val scrollType = state.selectedScrollType ?: return
        val scroll     = ScrollCatalog.get(scrollType)

        val slotIdx = state.inventorySlots.indexOfFirst { it is InventorySlot.ScrollItem && it.type == scrollType }
        val slot = state.inventorySlots.getOrNull(slotIdx) as? InventorySlot.ScrollItem
        if (slot == null || slot.quantity <= 0) {
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("주문서 수량이 부족합니다.")) }
            return
        }

        val (newEquipment, result) = service.applyScroll(equipment, scroll)
        _uiState.update { s ->
            val newSlots = s.inventorySlots.toMutableList()
            newSlots[slotIdx] = if (slot.quantity > 1) slot.copy(quantity = slot.quantity - 1) else null
            computeDerived(s.copy(equipment = newEquipment, inventorySlots = newSlots, lastResult = result))
        }
        when (result) {
            is EnhancementResult.Success   -> SoundManager.playSfx(SoundManager.Sfx.SCROLL_SUCCESS)
            is EnhancementResult.Failure,
            is EnhancementResult.Destroyed -> SoundManager.playSfx(SoundManager.Sfx.SCROLL_FAIL)
            else -> {}
        }
    }

    fun unequipEquipment() {
        _uiState.update { s ->
            if (s.equipment == null) return@update s
            val newSlots = addEquipToSlots(s.inventorySlots, s.equipment)
            computeDerived(s.copy(equipment = null, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
        }
    }

    fun equipFromInventory(equipment: Equipment) {
        _uiState.update { s ->
            val slotIdx = s.inventorySlots.indexOfFirst { it is InventorySlot.EquipItem && it.equipment == equipment }
            if (slotIdx < 0) return@update s
            val newSlots = s.inventorySlots.toMutableList()
            newSlots[slotIdx] = if (s.equipment != null) InventorySlot.EquipItem(s.equipment) else null
            computeDerived(s.copy(equipment = equipment, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
        }
    }

    fun resetEquipment() {
        _uiState.update { s ->
            val glove = EquipmentRegistry.get("NOGADA_GLOVE") ?: Equipment(
                name = "노가다 목장갑",
                attackPower = 0,
                maxUpgradeCount = 5,
                remainingUpgradeCount = 5,
                failedUpgradeCount = 0,
                destroyed = false,
                description = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
            )
            computeDerived(s.copy(
                equipment = glove,
                selectedScrollType = null,
                lastResult = null
            ))
        }
    }

    fun allocateStat(type: StatType) {
        _uiState.update { state ->
            val player = state.player
            if (player.availableStatPoint <= 0) return@update state
            val newStats = when (type) {
                StatType.STR -> player.stats.copy(str = player.stats.str + 1)
                StatType.DEX -> player.stats.copy(dex = player.stats.dex + 1)
                StatType.INT -> player.stats.copy(`int` = player.stats.`int` + 1)
                StatType.LUK -> player.stats.copy(luk = player.stats.luk + 1)
            }
            computeDerived(state.copy(
                player = player.copy(stats = newStats, availableStatPoint = player.availableStatPoint - 1)
            ))
        }
    }

    fun unequipWeapon() { _uiState.update { computeDerived(it.copy(weapon = null)) } }
    fun resetWeapon()   { _uiState.update { computeDerived(it.copy(weapon = DefaultWeapon)) } }

    // ── NPC 대화 / 퀘스트 ─────────────────────────────────────────────────────

    fun startDialogue(npcId: Int) {
        _uiState.update { state ->
            val npc = state.npcs.find { it.id == npcId } ?: return@update state
            val pages = when (npc.role) {
                NpcRole.QUEST -> buildDialoguePages(state.questState)
                NpcRole.EQUIPMENT_SHOP -> listOf(
                    DialoguePage(
                        speaker = "브루스",
                        text = "어서 오게 모험가.\n장비를 사거나 필요 없는 물건을 정리하고 싶다면 나를 찾아오게.",
                        choices = listOf("장비 상점 열기", "나가기")
                    )
                )
                NpcRole.CONSUMABLE_SHOP -> listOf(
                    DialoguePage(
                        speaker = "피아",
                        text = "전투가 길어질수록 물약은 꼭 필요해요.\n필요 없는 물건도 제가 사드릴게요.",
                        choices = listOf("소비 상점 열기", "나가기")
                    )
                )
            }
            state.copy(activeDialogue = DialogueSession(pages = pages, npcId = npcId))
        }
    }

    fun nextDialoguePage() {
        _uiState.update { state ->
            val dlg = state.activeDialogue ?: return@update state
            if (dlg.currentPage.choices.isNotEmpty()) return@update state
            if (dlg.isLastPage) state.copy(activeDialogue = null)
            else state.copy(activeDialogue = dlg.copy(currentIndex = dlg.currentIndex + 1))
        }
    }

    fun chooseDialogueOption(index: Int) {
        _uiState.update { state ->
            val dlg = state.activeDialogue ?: return@update state
            val npc = state.npcs.find { it.id == dlg.npcId }

            when (npc?.role) {
                NpcRole.EQUIPMENT_SHOP -> {
                    if (index == 0) {
                        state.copy(
                            activeDialogue = null,
                            activeShop = ShopInfo(ShopType.EQUIPMENT, "브루스")
                        )
                    } else {
                        state.copy(activeDialogue = null)
                    }
                }
                NpcRole.CONSUMABLE_SHOP -> {
                    if (index == 0) {
                        state.copy(
                            activeDialogue = null,
                            activeShop = ShopInfo(ShopType.CONSUMABLE, "피아")
                        )
                    } else {
                        state.copy(activeDialogue = null)
                    }
                }
                else -> {
                    // 퀘스트 NPC (츄츄)
                    val quest = state.questState
                    when {
                        quest.status == QuestStatus.NOT_STARTED -> if (index == 0) {
                            val newPages = listOf(
                                DialoguePage("츄츄", "정말 감사합니다!\n마을 밖 초보자 사냥터에 있는 스켈레톤 워리어들을 조금만 정리해 주세요."),
                                DialoguePage("츄츄", "스켈레톤 워리어 5마리를 처치하면 다시 저를 찾아와 주세요.")
                            )
                            state.copy(
                                questState = quest.copy(status = QuestStatus.IN_PROGRESS),
                                activeDialogue = DialogueSession(pages = newPages, npcId = dlg.npcId)
                            )
                        } else {
                            val newPages = listOf(
                                DialoguePage("츄츄", "그렇군요...\n혹시 마음이 바뀌면 다시 말을 걸어주세요.")
                            )
                            state.copy(activeDialogue = DialogueSession(pages = newPages, npcId = dlg.npcId))
                        }
                        quest.status == QuestStatus.READY_TO_COMPLETE -> {
                            val questData  = dlg.npcId?.let { QuestRegistry.questForNpc(it) }
                            val rewardExp  = questData?.rewardExp ?: 50
                            val rewardItem = questData?.rewardItemId?.let {
                                runCatching { ScrollType.valueOf(it) }.getOrNull()
                            } ?: ScrollType.GLOVE_ATK_100
                            val rewarded  = levelService.applyExp(state.player, rewardExp)
                            val newSlots  = addScrollToSlots(state.inventorySlots, rewardItem)
                            computeDerived(state.copy(
                                player         = rewarded,
                                inventorySlots = newSlots,
                                questState     = quest.copy(status = QuestStatus.COMPLETED),
                                activeDialogue = null
                            ))
                        }
                        else -> state.copy(activeDialogue = null)
                    }
                }
            }
        }
    }

    fun closeDialogue() {
        _uiState.update { it.copy(activeDialogue = null) }
    }

    private fun buildDialoguePages(quest: QuestState): List<DialoguePage> = when (quest.status) {
        QuestStatus.NOT_STARTED -> listOf(
            DialoguePage("츄츄", "안녕하세요!\n혹시 모험가이신가요?"),
            DialoguePage("츄츄", "요즘 마을 주변이 이상해졌어요.\n어디선가 나타난 스켈레톤 워리어들이 마을 주변을 배회하고 있어요."),
            DialoguePage("츄츄", "주민들이 밭을 관리하러 나가지도 못하고 있어요.\n스켈레톤들이 농작물을 망가뜨리고 길까지 막고 있거든요."),
            DialoguePage("츄츄", "저는 싸울 줄 몰라서 어떻게 할 수가 없어요.\n혹시 저를 도와주실 수 있나요?",
                choices = listOf("도와준다", "거절한다"))
        )
        QuestStatus.IN_PROGRESS -> listOf(
            DialoguePage("츄츄", "스켈레톤 워리어들이 아직 남아있어요. 힘내주세요!\n(${quest.killCount} / ${quest.killGoal})")
        )
        QuestStatus.READY_TO_COMPLETE -> listOf(
            DialoguePage("츄츄", "정말 해내셨군요!"),
            DialoguePage("츄츄", "주민들이 다시 밭으로 나갈 수 있게 되었어요."),
            DialoguePage("츄츄", "모두가 모험가님 덕분이라며 감사하고 있어요."),
            DialoguePage("츄츄", "이건 작은 감사의 표시예요.", choices = listOf("보상 받기"))
        )
        QuestStatus.COMPLETED -> listOf(
            DialoguePage("츄츄", "덕분에 평화로워졌어요.\n정말 감사합니다!")
        )
    }

    // ── 상점 시스템 ────────────────────────────────────────────────────────────

    fun closeShop() {
        _uiState.update { it.copy(activeShop = null) }
    }

    fun buyShopItem(shopType: ShopType, shopItemId: Int, quantity: Int = 1) {
        _uiState.update { state ->
            val shopItem = ShopRegistry.itemsFor(shopType).find { it.id == shopItemId } ?: return@update state

            val qty = if (shopItem.stackable) quantity.coerceAtLeast(1) else 1
            val totalCost = shopItem.buyPrice * qty
            if (state.money < totalCost) return@update state

            var newSlots = state.inventorySlots
            var newEquipment = state.equipment
            when (shopItem.itemType) {
                ShopItemType.EQUIPMENT -> {
                    val equip = ShopRegistry.createEquipment(shopItem.itemId) ?: return@update state
                    newSlots = addEquipToSlots(newSlots, equip)
                    // 장착 슬롯이 비어있으면 자동 장착 (드롭 동작과 동일)
                    if (state.equipment == null) newEquipment = equip
                }
                ShopItemType.SCROLL -> {
                    val scrollType = runCatching { ScrollType.valueOf(shopItem.itemId) }.getOrNull() ?: return@update state
                    repeat(qty) { newSlots = addScrollToSlots(newSlots, scrollType) }
                }
                ShopItemType.CONSUMABLE -> {
                    val consumableType = ConsumableCatalog.fromItemId(shopItem.itemId) ?: return@update state
                    repeat(qty) { newSlots = addConsumableToSlots(newSlots, consumableType) }
                }
            }
            val newState = state.copy(money = state.money - totalCost, inventorySlots = newSlots, equipment = newEquipment)
            if (newEquipment !== state.equipment) computeDerived(newState) else newState
        }
    }

    fun sellEquipmentBySlot(slotIndex: Int) {
        _uiState.update { state ->
            val slot = state.inventorySlots.getOrNull(slotIndex) as? InventorySlot.EquipItem ?: return@update state
            val sellPrice = ShopRegistry.sellPriceForEquipment(slot.equipment.name)
            val newSlots = state.inventorySlots.toMutableList()
            newSlots[slotIndex] = null
            state.copy(money = state.money + sellPrice, inventorySlots = newSlots)
        }
    }

    fun sellStackableItem(itemId: String, itemType: ShopItemType, quantity: Int) {
        if (quantity <= 0) return
        _uiState.update { state ->
            var newSlots = state.inventorySlots
            val earned: Int
            when (itemType) {
                ShopItemType.SCROLL -> {
                    val scrollType = runCatching { ScrollType.valueOf(itemId) }.getOrNull() ?: return@update state
                    val slotIdx = newSlots.indexOfFirst { it is InventorySlot.ScrollItem && it.type == scrollType }
                    val slot = newSlots.getOrNull(slotIdx) as? InventorySlot.ScrollItem ?: return@update state
                    if (slot.quantity < quantity) return@update state
                    earned = ShopRegistry.sellPriceForScroll(scrollType) * quantity
                    val m = newSlots.toMutableList()
                    m[slotIdx] = if (slot.quantity > quantity) slot.copy(quantity = slot.quantity - quantity) else null
                    newSlots = m
                }
                ShopItemType.CONSUMABLE -> {
                    val consumableType = ConsumableCatalog.fromItemId(itemId) ?: return@update state
                    val slotIdx = newSlots.indexOfFirst { it is InventorySlot.ConsumableItem && it.type == consumableType }
                    val slot = newSlots.getOrNull(slotIdx) as? InventorySlot.ConsumableItem ?: return@update state
                    if (slot.quantity < quantity) return@update state
                    earned = ShopRegistry.sellPriceForConsumable(consumableType) * quantity
                    val m = newSlots.toMutableList()
                    m[slotIdx] = if (slot.quantity > quantity) slot.copy(quantity = slot.quantity - quantity) else null
                    newSlots = m
                }
                else -> return@update state
            }
            state.copy(money = state.money + earned, inventorySlots = newSlots)
        }
    }

    // ── 포션 사용 ──────────────────────────────────────────────────────────────

    fun usePotion(consumableType: ConsumableType) {
        _uiState.update { state ->
            val slotIdx = state.inventorySlots.indexOfFirst {
                it is InventorySlot.ConsumableItem && it.type == consumableType
            }
            val slot = state.inventorySlots.getOrNull(slotIdx) as? InventorySlot.ConsumableItem ?: return@update state
            if (slot.quantity <= 0) return@update state

            val info = ConsumableCatalog.get(consumableType)
            val newHp = (state.player.hp + info.healAmount).coerceAtMost(state.player.maxHp)

            val newSlots = state.inventorySlots.toMutableList()
            newSlots[slotIdx] = if (slot.quantity > 1) slot.copy(quantity = slot.quantity - 1) else null

            state.copy(
                player = state.player.copy(hp = newHp),
                inventorySlots = newSlots
            )
        }
    }

    fun assignQuickSlot(index: Int, type: ConsumableType) {
        _uiState.update { state ->
            val updated = state.quickSlots.toMutableList().also { it[index] = type }
            state.copy(quickSlots = updated)
        }
    }

    fun useQuickSlotPotion(index: Int) {
        val type = _uiState.value.quickSlots.getOrNull(index) ?: return
        usePotion(type)
    }
}
