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
import com.a_survivor.app.model.TownWorld
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
import com.a_survivor.app.model.MaterialCatalog
import com.a_survivor.app.model.MaterialType
import com.a_survivor.app.model.QuestData
import com.a_survivor.app.model.QuestState
import com.a_survivor.app.model.QuestStatus
import com.a_survivor.app.model.TutorialStep
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
    data class MaterialItem(val type: com.a_survivor.app.model.MaterialType, val quantity: Int) : InventorySlot()
}

data class PendingRespawn(val monsterId: Int, val diedAt: Long)

data class PendingPlayerAttack(
    val targetId: Int,
    val damage: Int,
    val isMiss: Boolean,
    val applyAt: Long
)

data class UiState(
    val equipment: Equipment?,   // GLOVE
    val hat: Equipment? = null,   // HAT
    val top: Equipment? = null,   // TOP
    val shoes: Equipment? = null, // SHOES
    val pants: Equipment? = null, // PANTS
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
    val autoAttackEnabled: Boolean = false
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
        val tutStep   = runCatching { TutorialStep.valueOf(data.tutorialStep) }.getOrDefault(TutorialStep.COMPLETED)

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
            hat            = data.hat,
            top            = data.top,
            shoes          = data.shoes,
            pants          = data.pants,
            weapon         = data.weapon,
            money          = data.money,
            inventorySlots = inventory,
            world          = world,
            monsters       = spawnSkeletons(world, 5),
            portals        = PortalRegistry.portalsFor(mapType),
            npcs           = NpcRegistry.npcsFor(mapType),
            questState     = QuestState(
                status                = questStat,
                killCount             = data.questKillCount,
                killGoal              = data.questKillGoal,
                tutorialStep          = tutStep,
                tutorialTravelDistance = data.tutorialTravelDistance,
                mainQuestId           = data.mainQuestId,
                mainQuestProgress     = data.mainQuestProgress,
                mainQuestStatus       = runCatching { QuestStatus.valueOf(data.mainQuestStatus) }
                                            .getOrDefault(QuestStatus.NOT_STARTED),
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
        state.copy(derivedStats = derivedStatsCalculator.calculate(state.player, state.equipment, state.hat, state.top, state.shoes, state.pants))

    private fun createInitialState(job: PlayerJob = PlayerJob.BEGINNER): UiState {
        val initialPlayer = Player(job = job, stats = job.initialStats(), positionX = 450f, positionY = 286f)
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
            monsters   = emptyList(),
            portals    = PortalRegistry.portalsFor(MapType.TOWN),
            npcs       = NpcRegistry.npcsFor(MapType.TOWN),
            weapon     = DefaultWeapon,
            equipment  = initialEquip,
            player     = initialPlayer,
            world      = TownWorld,
            questState = QuestState(
                killGoal     = questData?.targetCount ?: 5,
                tutorialStep = TutorialStep.LEARN_MOVEMENT
            )
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

            // 튜토리얼 0단계: 이동 방법 학습 (30 이동 시 츄츄 만나러 가기 단계로 진행)
            var afterTutorial = afterPickup
            if (afterPickup.questState.tutorialStep == TutorialStep.LEARN_MOVEMENT) {
                val dx = newX - p.positionX
                val dy = newY - p.positionY
                val stepDist = sqrt(dx * dx + dy * dy)
                val newDist = afterPickup.questState.tutorialTravelDistance + stepDist
                afterTutorial = if (newDist >= 30f) {
                    computeDerived(afterPickup.copy(
                        questState = afterPickup.questState.copy(
                            tutorialStep           = TutorialStep.TALK_TO_CHUCHU,
                            tutorialTravelDistance = 0f
                        )
                    ))
                } else {
                    afterPickup.copy(questState = afterPickup.questState.copy(tutorialTravelDistance = newDist))
                }
            }

            // 튜토리얼 2단계: 마을 탐험 거리 누적
            if (afterPickup.questState.tutorialStep == TutorialStep.EXPLORE_TOWN) {
                val dx = newX - p.positionX
                val dy = newY - p.positionY
                val stepDist = sqrt(dx * dx + dy * dy)
                val newDist = afterPickup.questState.tutorialTravelDistance + stepDist
                afterTutorial = if (newDist >= 300f) {
                    val rewarded = levelService.applyExp(afterPickup.player, 10)
                    computeDerived(afterPickup.copy(
                        player = rewarded,
                        money  = afterPickup.money + 50,
                        questState = afterPickup.questState.copy(
                            tutorialStep           = TutorialStep.USE_PORTAL,
                            tutorialTravelDistance = newDist
                        )
                    ))
                } else {
                    afterPickup.copy(questState = afterPickup.questState.copy(tutorialTravelDistance = newDist))
                }
            }

            val now = System.currentTimeMillis()
            if (now - afterTutorial.lastTeleportAt < PORTAL_COOLDOWN) return@update afterTutorial

            val nearPortal = afterTutorial.portals.firstOrNull { portal ->
                val dx = newX - portal.worldX
                val dy = newY - portal.worldY
                sqrt(dx * dx + dy * dy) <= PORTAL_RANGE
            }
            if (nearPortal != null) {
                teleportedMap = nearPortal.targetMap
                teleportState(afterTutorial, nearPortal, now)
            } else afterTutorial
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
            name         = cfg.name,
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

        var result = state.copy(
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

        val tut = state.questState.tutorialStep
        result = when {
            // 3단계: 포탈로 초보자 사냥터 진입
            tut == TutorialStep.USE_PORTAL && portal.targetMap == MapType.BEGINNER_FIELD -> {
                val rewarded = levelService.applyExp(result.player, 15)
                computeDerived(result.copy(
                    player     = rewarded,
                    questState = result.questState.copy(tutorialStep = TutorialStep.LEARN_TAP_ATTACK)
                ))
            }
            // 8단계: 마을 복귀 → 튜토리얼 완료 + 츄츄 자동 대화
            tut == TutorialStep.RETURN_TO_TOWN && portal.targetMap == MapType.TOWN -> {
                val rewarded = levelService.applyExp(result.player, 20)
                val completePages = listOf(
                    DialoguePage("츄츄", "벌써 사냥을 다녀오셨네요!\n생각보다 재능이 있는 것 같아요."),
                    DialoguePage("츄츄", "이제 기본적인 모험 방법은\n충분히 익히신 것 같아요."),
                    DialoguePage("츄츄", "하지만 아직 마을 주변에는\n몬스터들이 남아 있어요."),
                    DialoguePage("츄츄", "주민들이 안심하고 다닐 수 있도록\n도와주실 수 있나요?",
                        choices = listOf("도와주겠습니다", "나중에 할게요"))
                )
                computeDerived(result.copy(
                    player         = rewarded,
                    money          = result.money + 100,
                    questState     = result.questState.copy(tutorialStep = TutorialStep.COMPLETED),
                    activeDialogue = DialogueSession(pages = completePages, npcId = 1)
                ))
            }
            else -> result
        }

        // 메인 퀘스트 ENTER_MAP 자동 완료
        val mqDataEnter = QuestRegistry.get(result.questState.mainQuestId)
        if (result.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
            mqDataEnter?.questType == "ENTER_MAP" &&
            mqDataEnter.targetMapId.isNotBlank() &&
            portal.targetMap.name == mqDataEnter.targetMapId) {
            result = result.copy(questState = result.questState.copy(
                mainQuestStatus = QuestStatus.READY_TO_COMPLETE
            ))
        }

        return result
    }

    // ── 자동 공격 틱 ───────────────────────────────────────────────────────────

    private fun autoAttackTick() {
        if (!_uiState.value.autoAttackEnabled) return
        executeAttack()
    }

    fun manualAttack() {
        // 튜토리얼: 수동 탭 공격 학습 완료
        _uiState.update { state ->
            if (state.questState.tutorialStep == TutorialStep.LEARN_TAP_ATTACK)
                state.copy(questState = state.questState.copy(tutorialStep = TutorialStep.LEARN_AUTO_SWITCH))
            else state  // LEARN_TAP_ATTACK → LEARN_AUTO_SWITCH → LEARN_MANUAL_SWITCH → KILL_MONSTER
        }
        executeAttack()
    }

    fun toggleAutoAttack() {
        _uiState.update { state ->
            val newAuto = !state.autoAttackEnabled
            val newStep = when {
                // 순서: LEARN_TAP_ATTACK → LEARN_AUTO_SWITCH → LEARN_MANUAL_SWITCH → KILL_MONSTER
                state.questState.tutorialStep == TutorialStep.LEARN_AUTO_SWITCH && newAuto ->
                    TutorialStep.LEARN_MANUAL_SWITCH
                state.questState.tutorialStep == TutorialStep.LEARN_MANUAL_SWITCH && !newAuto ->
                    TutorialStep.KILL_MONSTER
                else -> state.questState.tutorialStep
            }
            state.copy(
                autoAttackEnabled = newAuto,
                questState        = state.questState.copy(tutorialStep = newStep)
            )
        }
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

            // 튜토리얼 4단계: 첫 번째 몬스터 처치
            val tutKillAdvance = killedList.isNotEmpty() && state.questState.tutorialStep == TutorialStep.KILL_MONSTER
            val playerWithTutExp = if (tutKillAdvance) levelService.applyExp(updatedPlayer, 20) else updatedPlayer
            if (playerWithTutExp.level > state.player.level) sfxToPlay = SoundManager.Sfx.LEVEL_UP

            // 메인 퀘스트 KILL 진행
            val mqData = QuestRegistry.get(state.questState.mainQuestId)
            val mqKillAdd = if (state.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                mqData?.questType == "KILL" && mqData.targetMonsterId.isNotBlank()) {
                killedList.count { it.monsterId == mqData.targetMonsterId }
            } else 0
            val newMqProgress = (state.questState.mainQuestProgress + mqKillAdd)
                .coerceAtMost(mqData?.targetCount ?: Int.MAX_VALUE)
            val newMqStatus = if (mqKillAdd > 0 && newMqProgress >= (mqData?.targetCount ?: Int.MAX_VALUE))
                QuestStatus.READY_TO_COMPLETE else state.questState.mainQuestStatus
            // 메인 퀘스트 REACH_LEVEL 감지
            val newMqStatusFinal = if (state.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                mqData?.questType == "REACH_LEVEL" &&
                playerWithTutExp.level >= mqData.targetLevel &&
                state.player.level < mqData.targetLevel) QuestStatus.READY_TO_COMPLETE else newMqStatus

            val newQuestState  = state.questState.copy(
                killCount         = newKillCount.coerceAtMost(state.questState.killGoal),
                status            = newQuestStatus,
                tutorialStep      = if (tutKillAdvance) TutorialStep.PICKUP_ITEM else state.questState.tutorialStep,
                mainQuestProgress = newMqProgress,
                mainQuestStatus   = newMqStatusFinal,
            )

            val finalState = baseState.copy(
                monsters              = updatedMonsters,
                player                = playerWithTutExp,
                money                 = baseState.money + (if (tutKillAdvance) 100 else 0),
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

            // 튜토리얼 4단계: 첫 번째 몬스터 처치 (스킬 경로)
            val tutKillAdvSkill = killed.isNotEmpty() && state.questState.tutorialStep == TutorialStep.KILL_MONSTER
            val playerWithTutExpSkill = if (tutKillAdvSkill) levelService.applyExp(updatedPlayer, 20) else updatedPlayer

            // 메인 퀘스트 KILL / REACH_LEVEL 진행 (스킬 경로)
            val mqDataSkill = QuestRegistry.get(state.questState.mainQuestId)
            val mqKillAddSkill = if (state.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                mqDataSkill?.questType == "KILL" && mqDataSkill.targetMonsterId.isNotBlank()) {
                killed.count { it.monsterId == mqDataSkill.targetMonsterId }
            } else 0
            val newMqProgressSkill = (state.questState.mainQuestProgress + mqKillAddSkill)
                .coerceAtMost(mqDataSkill?.targetCount ?: Int.MAX_VALUE)
            val newMqStatusSkill = when {
                mqKillAddSkill > 0 && newMqProgressSkill >= (mqDataSkill?.targetCount ?: Int.MAX_VALUE) ->
                    QuestStatus.READY_TO_COMPLETE
                state.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                    mqDataSkill?.questType == "REACH_LEVEL" &&
                    playerWithTutExpSkill.level >= mqDataSkill.targetLevel &&
                    state.player.level < mqDataSkill.targetLevel ->
                    QuestStatus.READY_TO_COMPLETE
                else -> state.questState.mainQuestStatus
            }

            val advancePending = state.jobAdvancementPending || (
                gainedExp > 0 && playerWithTutExpSkill.job == PlayerJob.BEGINNER &&
                playerWithTutExpSkill.level >= 3 && state.player.level < 3
            )

            val base = state.copy(
                skillCooldownUntil    = newCooldowns,
                skillEffects          = newEffects,
                monsters              = updatedMonsters,
                player                = playerWithTutExpSkill,
                money                 = state.money + (if (tutKillAdvSkill) 100 else 0),
                groundItems           = state.groundItems + newGroundItems,
                pendingRespawns       = state.pendingRespawns + killed.map { PendingRespawn(it.id, now) },
                damageNumbers         = state.damageNumbers + dmgNums,
                questState            = state.questState.copy(
                    killCount         = newKillCount,
                    status            = newQuestStatus,
                    tutorialStep      = if (tutKillAdvSkill) TutorialStep.PICKUP_ITEM else state.questState.tutorialStep,
                    mainQuestProgress = newMqProgressSkill,
                    mainQuestStatus   = newMqStatusSkill,
                ),
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

            // 튜토리얼 4단계: 첫 번째 몬스터 처치 (투사체 경로)
            val tutKillAdvProj = killed.isNotEmpty() && state.questState.tutorialStep == TutorialStep.KILL_MONSTER
            val playerWithTutExpProj = if (tutKillAdvProj) levelService.applyExp(updatedPlayer, 20) else updatedPlayer

            // 메인 퀘스트 KILL / REACH_LEVEL 진행 (투사체 경로)
            val mqDataProj = QuestRegistry.get(state.questState.mainQuestId)
            val mqKillAddProj = if (state.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                mqDataProj?.questType == "KILL" && mqDataProj.targetMonsterId.isNotBlank()) {
                killed.count { it.monsterId == mqDataProj.targetMonsterId }
            } else 0
            val newMqProgressProj = (state.questState.mainQuestProgress + mqKillAddProj)
                .coerceAtMost(mqDataProj?.targetCount ?: Int.MAX_VALUE)
            val newMqStatusProj = when {
                mqKillAddProj > 0 && newMqProgressProj >= (mqDataProj?.targetCount ?: Int.MAX_VALUE) ->
                    QuestStatus.READY_TO_COMPLETE
                state.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                    mqDataProj?.questType == "REACH_LEVEL" &&
                    playerWithTutExpProj.level >= mqDataProj.targetLevel &&
                    state.player.level < mqDataProj.targetLevel ->
                    QuestStatus.READY_TO_COMPLETE
                else -> state.questState.mainQuestStatus
            }

            val newQuestStateProj = state.questState.copy(
                killCount         = newKillCountProj.coerceAtMost(state.questState.killGoal),
                status            = newQuestStatusProj,
                tutorialStep      = if (tutKillAdvProj) TutorialStep.PICKUP_ITEM else state.questState.tutorialStep,
                mainQuestProgress = newMqProgressProj,
                mainQuestStatus   = newMqStatusProj,
            )

            val newState = state.copy(
                projectiles           = projResult.updatedProjectiles,
                monsters              = updatedMonsters,
                player                = playerWithTutExpProj,
                money                 = state.money + (if (tutKillAdvProj) 100 else 0),
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

    private fun addMaterialToSlots(slots: List<InventorySlot?>, type: com.a_survivor.app.model.MaterialType): List<InventorySlot?> {
        val m = slots.toMutableList()
        val idx = m.indexOfFirst { it is InventorySlot.MaterialItem && it.type == type }
        if (idx >= 0) {
            m[idx] = (m[idx] as InventorySlot.MaterialItem).let { it.copy(quantity = it.quantity + 1) }
        } else {
            val empty = m.indexOfFirst { it == null }
            if (empty >= 0) m[empty] = InventorySlot.MaterialItem(type, 1)
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
                is DropItem.MaterialDrop -> {
                    val info = MaterialCatalog.get(drop.materialType)
                    var ms = addMessage(
                        s.copy(inventorySlots = addMaterialToSlots(s.inventorySlots, drop.materialType)),
                        "${info.name} 획득", MessageType.ITEM
                    )
                    // 메인 퀘스트 COLLECT 진행 — 인벤토리 실수량 기준
                    val mqDataCollect = QuestRegistry.get(ms.questState.mainQuestId)
                    if (ms.questState.mainQuestStatus == QuestStatus.IN_PROGRESS &&
                        mqDataCollect?.questType == "COLLECT" &&
                        mqDataCollect.targetMaterialId == drop.materialType.name) {
                        val totalInInv = ms.inventorySlots
                            .filterIsInstance<InventorySlot.MaterialItem>()
                            .filter { it.type == drop.materialType }
                            .sumOf { it.quantity }
                        val newProg = totalInInv.coerceAtMost(mqDataCollect.targetCount)
                        val newStat = if (newProg >= mqDataCollect.targetCount)
                            QuestStatus.READY_TO_COMPLETE else QuestStatus.IN_PROGRESS
                        ms = ms.copy(questState = ms.questState.copy(
                            mainQuestProgress = newProg,
                            mainQuestStatus   = newStat,
                        ))
                    }
                    ms
                }
            }
        }
        // 튜토리얼 5단계: 첫 번째 아이템 획득
        if (drops.isNotEmpty() && s.questState.tutorialStep == TutorialStep.PICKUP_ITEM) {
            val rewarded = levelService.applyExp(s.player, 10)
            s = computeDerived(s.copy(
                player     = rewarded,
                questState = s.questState.copy(tutorialStep = TutorialStep.OPEN_INVENTORY)
            ))
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
        val state      = _uiState.value
        val scrollType = state.selectedScrollType ?: return
        val scroll     = ScrollCatalog.get(scrollType)
        val targetSlot = scroll.targetSlot.ifEmpty { "GLOVE" }

        val slotIdx = state.inventorySlots.indexOfFirst { it is InventorySlot.ScrollItem && it.type == scrollType }
        val slot = state.inventorySlots.getOrNull(slotIdx) as? InventorySlot.ScrollItem
        if (slot == null || slot.quantity <= 0) {
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("주문서 수량이 부족합니다.")) }
            return
        }

        if (targetSlot == "WEAPON") {
            val weapon = state.weapon ?: run {
                _uiState.update { it.copy(lastResult = EnhancementResult.Error("무기 슬롯에 장비가 없습니다.")) }
                return
            }
            val (newWeapon, result) = service.applyScrollToWeapon(weapon, scroll)
            _uiState.update { s ->
                val newSlots = s.inventorySlots.toMutableList()
                newSlots[slotIdx] = if (slot.quantity > 1) slot.copy(quantity = slot.quantity - 1) else null
                computeDerived(s.copy(weapon = newWeapon, inventorySlots = newSlots, lastResult = result))
            }
            when (result) {
                is EnhancementResult.Success   -> SoundManager.playSfx(SoundManager.Sfx.SCROLL_SUCCESS)
                is EnhancementResult.Failure,
                is EnhancementResult.Destroyed -> SoundManager.playSfx(SoundManager.Sfx.SCROLL_FAIL)
                else -> {}
            }
            return
        }

        val equipment = when (targetSlot) {
            "HAT"   -> state.hat
            "TOP"   -> state.top
            "SHOES" -> state.shoes
            "PANTS" -> state.pants
            else    -> state.equipment
        } ?: run {
            val slotName = when (targetSlot) {
                "HAT" -> "모자"; "TOP" -> "상의"; "SHOES" -> "신발"; "PANTS" -> "하의"; else -> "장갑"
            }
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("${slotName} 슬롯에 장비가 없습니다.")) }
            return
        }

        val (newEquipment, result) = service.applyScroll(equipment, scroll)
        _uiState.update { s ->
            val newSlots = s.inventorySlots.toMutableList()
            newSlots[slotIdx] = if (slot.quantity > 1) slot.copy(quantity = slot.quantity - 1) else null
            val updated = when (targetSlot) {
                "HAT"   -> s.copy(hat   = newEquipment, inventorySlots = newSlots, lastResult = result)
                "TOP"   -> s.copy(top   = newEquipment, inventorySlots = newSlots, lastResult = result)
                "SHOES" -> s.copy(shoes = newEquipment, inventorySlots = newSlots, lastResult = result)
                "PANTS" -> s.copy(pants = newEquipment, inventorySlots = newSlots, lastResult = result)
                else    -> s.copy(equipment = newEquipment, inventorySlots = newSlots, lastResult = result)
            }
            computeDerived(updated)
        }
        when (result) {
            is EnhancementResult.Success   -> SoundManager.playSfx(SoundManager.Sfx.SCROLL_SUCCESS)
            is EnhancementResult.Failure,
            is EnhancementResult.Destroyed -> SoundManager.playSfx(SoundManager.Sfx.SCROLL_FAIL)
            else -> {}
        }
    }

    fun unequipEquipment(slot: String = "GLOVE") {
        _uiState.update { s ->
            when (slot) {
                "HAT" -> {
                    val cur = s.hat ?: return@update s
                    computeDerived(s.copy(hat = null, inventorySlots = addEquipToSlots(s.inventorySlots, cur)))
                }
                "TOP" -> {
                    val cur = s.top ?: return@update s
                    computeDerived(s.copy(top = null, inventorySlots = addEquipToSlots(s.inventorySlots, cur)))
                }
                "SHOES" -> {
                    val cur = s.shoes ?: return@update s
                    computeDerived(s.copy(shoes = null, inventorySlots = addEquipToSlots(s.inventorySlots, cur)))
                }
                "PANTS" -> {
                    val cur = s.pants ?: return@update s
                    computeDerived(s.copy(pants = null, inventorySlots = addEquipToSlots(s.inventorySlots, cur)))
                }
                else -> {
                    val cur = s.equipment ?: return@update s
                    val newSlots = addEquipToSlots(s.inventorySlots, cur)
                    computeDerived(s.copy(equipment = null, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
            }
        }
    }

    fun equipFromInventory(equipment: Equipment) {
        _uiState.update { s ->
            val slotIdx = s.inventorySlots.indexOfFirst { it is InventorySlot.EquipItem && it.equipment == equipment }
            if (slotIdx < 0) return@update s
            val newSlots = s.inventorySlots.toMutableList()
            val equipped: UiState = when (equipment.slot) {
                "HAT" -> {
                    newSlots[slotIdx] = if (s.hat != null) InventorySlot.EquipItem(s.hat) else null
                    computeDerived(s.copy(hat = equipment, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
                "TOP" -> {
                    newSlots[slotIdx] = if (s.top != null) InventorySlot.EquipItem(s.top) else null
                    computeDerived(s.copy(top = equipment, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
                "SHOES" -> {
                    newSlots[slotIdx] = if (s.shoes != null) InventorySlot.EquipItem(s.shoes) else null
                    computeDerived(s.copy(shoes = equipment, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
                "PANTS" -> {
                    newSlots[slotIdx] = if (s.pants != null) InventorySlot.EquipItem(s.pants) else null
                    computeDerived(s.copy(pants = equipment, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
                "WEAPON" -> {
                    val newWeapon = equipmentToWeapon(equipment)
                    newSlots[slotIdx] = null
                    computeDerived(s.copy(weapon = newWeapon, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
                else -> {
                    newSlots[slotIdx] = if (s.equipment != null) InventorySlot.EquipItem(s.equipment) else null
                    computeDerived(s.copy(equipment = equipment, inventorySlots = newSlots, selectedScrollType = null, lastResult = null))
                }
            }
            // 튜토리얼 7단계: 장비 착용
            if (equipped.questState.tutorialStep == TutorialStep.EQUIP_ITEM) {
                val rewarded = levelService.applyExp(equipped.player, 20)
                computeDerived(equipped.copy(
                    player     = rewarded,
                    questState = equipped.questState.copy(tutorialStep = TutorialStep.RETURN_TO_TOWN)
                ))
            } else equipped
        }
    }

    private fun equipmentToWeapon(e: Equipment): Weapon = Weapon(
        name                  = e.name,
        attackPower           = e.attackPower,
        magicPower            = e.magicPower,
        strBonus              = e.strBonus,
        reqLevel              = e.requiredLevel,
        itemId                = e.itemId,
        weaponType            = weaponTypeFor(e.itemId),
        attackSpeed           = "보통",
        availableJobs         = PlayerJob.values().toList(),
        maxUpgradeCount       = e.maxUpgradeCount,
        remainingUpgradeCount = e.remainingUpgradeCount,
        failedUpgradeCount    = e.failedUpgradeCount,
        destroyed             = e.destroyed,
        description           = e.description
    )

    private fun weaponTypeFor(itemId: String): String = when (itemId) {
        "TEST_BOW" -> "활"
        else -> "한손검"
    }

    // 튜토리얼 6단계: 인벤토리 열기 (MainActivity에서 호출)
    fun onInventoryOpened() {
        _uiState.update { state ->
            if (state.questState.tutorialStep != TutorialStep.OPEN_INVENTORY) return@update state
            val rewarded = levelService.applyExp(state.player, 10)
            computeDerived(state.copy(
                player     = rewarded,
                questState = state.questState.copy(tutorialStep = TutorialStep.EQUIP_ITEM)
            ))
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

    fun unequipWeapon() {
        _uiState.update { s ->
            val w = s.weapon ?: return@update s
            if (w.itemId.isNotEmpty()) {
                val base = EquipmentRegistry.get(w.itemId)
                if (base != null) {
                    val restored = base.copy(
                        attackPower           = w.attackPower,
                        magicPower            = w.magicPower,
                        strBonus              = w.strBonus,
                        maxUpgradeCount       = w.maxUpgradeCount,
                        remainingUpgradeCount = w.remainingUpgradeCount,
                        failedUpgradeCount    = w.failedUpgradeCount,
                        destroyed             = w.destroyed
                    )
                    computeDerived(s.copy(weapon = null, inventorySlots = addEquipToSlots(s.inventorySlots, restored)))
                } else {
                    computeDerived(s.copy(weapon = null))
                }
            } else {
                computeDerived(s.copy(weapon = null))
            }
        }
    }
    fun resetWeapon()   { _uiState.update { computeDerived(it.copy(weapon = DefaultWeapon)) } }

    // ── NPC 대화 / 퀘스트 ─────────────────────────────────────────────────────

    fun startDialogue(npcId: Int) {
        _uiState.update { state ->
            val npc = state.npcs.find { it.id == npcId } ?: return@update state
            val pages = when (npc.role) {
                NpcRole.QUEST -> {
                    val tut = state.questState.tutorialStep
                    when {
                        tut == TutorialStep.LEARN_MOVEMENT || tut == TutorialStep.TALK_TO_CHUCHU -> listOf(
                            DialoguePage("츄츄", "앗! 처음 보는 얼굴이네요.\n혹시 새로 마을에 오신 모험가신가요?"),
                            DialoguePage("츄츄", "이 마을 주변은 비교적 안전하지만\n밖에는 몬스터들이 돌아다니고 있어요."),
                            DialoguePage("츄츄", "조이스틱으로 이동하는 방법은 이미 아시겠군요!\n먼저 마을을 한 바퀴 더 둘러볼까요?")
                        )
                        tut == TutorialStep.EXPLORE_TOWN -> listOf(
                            DialoguePage("츄츄", "아직 탐험 중이군요!\n왼쪽 하단의 조이스틱을 드래그해서\n마을을 걸어보세요.")
                        )
                        tut == TutorialStep.USE_PORTAL -> listOf(
                            DialoguePage("츄츄", "왼쪽 포탈을 이용해\n초보자 사냥터로 이동해보세요!")
                        )
                        tut.ordinal in TutorialStep.LEARN_TAP_ATTACK.ordinal..TutorialStep.EQUIP_ITEM.ordinal -> listOf(
                            DialoguePage("츄츄", "사냥터에서 모험을 계속해주세요!\n제가 응원하고 있을게요.")
                        )
                        tut == TutorialStep.RETURN_TO_TOWN -> listOf(
                            DialoguePage("츄츄", "마을로 돌아오세요!\n포탈을 이용해 마을로 돌아와 주세요.")
                        )
                        else -> buildDialoguePages(state.questState)
                    }
                }
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
            // 튜토리얼 1단계 완료: 츄츄와 첫 대화 (LEARN_MOVEMENT 중 도달해도 동일하게 처리)
            var newState = state.copy(activeDialogue = DialogueSession(pages = pages, npcId = npcId))
            if (npc.role == NpcRole.QUEST && (
                state.questState.tutorialStep == TutorialStep.TALK_TO_CHUCHU ||
                state.questState.tutorialStep == TutorialStep.LEARN_MOVEMENT
            )) {
                val rewarded = levelService.applyExp(newState.player, 10)
                newState = computeDerived(newState.copy(
                    player     = rewarded,
                    questState = newState.questState.copy(
                        tutorialStep           = TutorialStep.EXPLORE_TOWN,
                        tutorialTravelDistance = 0f
                    )
                ))
            }
            newState
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
                        // QUEST_001 보상 수령
                        quest.status == QuestStatus.READY_TO_COMPLETE -> {
                            val questData = QuestRegistry.get("QUEST_001")
                            val rewardExp = questData?.rewardExp ?: 50
                            val rewarded  = levelService.applyExp(state.player, rewardExp)
                            var newState  = computeDerived(state.copy(
                                player     = rewarded,
                                money      = state.money + (questData?.rewardMoney ?: 0),
                                questState = quest.copy(status = QuestStatus.COMPLETED),
                            ))
                            newState = giveRewardItem(newState, questData?.rewardItemId ?: "GLOVE_ATK_100")
                            // MQ-001 등록 (NOT_STARTED) 후 인트로 대화 표시
                            newState = startNextMainQuest(newState, questData?.nextQuestId ?: "MQ-001")
                            val mq001Pages = listOf(
                                DialoguePage("츄츄", "그런데... 사냥하면서 혹시 이상한 뼈를 보신 적 있나요?"),
                                DialoguePage("츄츄", "평범한 스켈레톤 뼈인데 뭔가 이상한 기운이 느껴지는 게 있어요.\n사냥하다 보면 주울 수 있을 거예요."),
                                DialoguePage("츄츄", "초보자 사냥터에서 수상한 뼈 10개를 모아다 주실 수 있나요?",
                                    choices = listOf("수락한다", "나중에 할게요"))
                            )
                            newState.copy(activeDialogue = DialogueSession(pages = mq001Pages, npcId = dlg.npcId))
                        }
                        // 메인 퀘스트 수락
                        quest.mainQuestStatus == QuestStatus.NOT_STARTED && quest.mainQuestId.isNotBlank() -> {
                            if (index == 0) {
                                acceptMainQuest(state).copy(activeDialogue = null)
                            } else {
                                state.copy(activeDialogue = null)
                            }
                        }
                        // 메인 퀘스트 체인 보상 수령
                        quest.mainQuestStatus == QuestStatus.READY_TO_COMPLETE && index == 0 -> {
                            val qData = QuestRegistry.get(quest.mainQuestId)
                                ?: return@update state.copy(activeDialogue = null)
                            val rewarded = levelService.applyExp(state.player, qData.rewardExp)
                            var newState = computeDerived(state.copy(
                                player     = rewarded,
                                money      = state.money + qData.rewardMoney,
                                questState = quest.copy(mainQuestStatus = QuestStatus.COMPLETED),
                            ))
                            newState = giveRewardItem(newState, qData.rewardItemId)

                            // 다음 퀘스트 등록 (NOT_STARTED)
                            val nextId = qData.nextQuestId
                            newState = if (nextId.isNotBlank()) {
                                startNextMainQuest(newState, nextId)
                            } else {
                                newState
                            }

                            // MQ-006 / MQ-013 특수 인트로 대화 (수락/거절 선택지 포함)
                            val specialPages: List<DialoguePage>? = when (nextId) {
                                "MQ-006" -> listOf(
                                    DialoguePage("츄츄", "최근 스켈레톤들이 출몰하던 지역을\n조사하던 중 이상한 흔적이 발견됐어요."),
                                    DialoguePage("츄츄", "마을에서 사냥터로 가는 포탈 반대편에서\n한 번도 본 적 없는 거대한 발자국이 발견됐어요."),
                                    DialoguePage("츄츄", "스켈레톤들과 관련이 있는지 모르겠지만\n분명 평범한 일은 아닌 것 같아요."),
                                    DialoguePage("츄츄", "직접 가서 조사해 주실 수 있나요?",
                                        choices = listOf("수락한다", "나중에 할게요"))
                                )
                                "MQ-013" -> listOf(
                                    DialoguePage("츄츄", "정말 대단해요!\n덕분에 마을 주변은 한동안 안전할 것 같아요."),
                                    DialoguePage("츄츄", "하지만 이상해요.\n스켈레톤들과 미노타우르스가 동시에 늘어난 건 우연이 아닐 거예요."),
                                    DialoguePage("츄츄", "분명 어딘가에 원인이 있을 거예요.\n지금의 힘으로는 그 원인을 조사하기 어려울지도 몰라요."),
                                    DialoguePage("츄츄", "조금 더 강해져서 돌아와 주세요.\n그때 새로운 이야기를 들려드릴게요.")
                                )
                                else -> null
                            }
                            newState.copy(
                                activeDialogue = specialPages?.let {
                                    DialogueSession(pages = it, npcId = dlg.npcId)
                                }
                            )
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

    private fun buildDialoguePages(quest: QuestState): List<DialoguePage> = when {
        quest.status != QuestStatus.COMPLETED -> buildQuest001Pages(quest)
        quest.mainQuestId.isNotBlank()        -> buildMainQuestDialogue(quest)
        else -> listOf(DialoguePage("츄츄", "덕분에 평화로워졌어요.\n정말 감사합니다!"))
    }

    private fun buildQuest001Pages(quest: QuestState): List<DialoguePage> = when (quest.status) {
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

    private fun buildMainQuestDialogue(quest: QuestState): List<DialoguePage> {
        val data = QuestRegistry.get(quest.mainQuestId)
            ?: return listOf(DialoguePage("츄츄", "계속 모험을 해주세요!"))
        return when (quest.mainQuestStatus) {
            QuestStatus.NOT_STARTED -> listOf(
                DialoguePage("츄츄", buildMainQuestOfferText(data),
                    choices = listOf("수락한다", "나중에 할게요"))
            )
            QuestStatus.IN_PROGRESS -> listOf(
                DialoguePage("츄츄", buildMainQuestProgressText(data, quest))
            )
            QuestStatus.READY_TO_COMPLETE -> listOf(
                DialoguePage("츄츄", "수고하셨어요!\n'${data.name}' 임무를 완수하셨군요."),
                DialoguePage("츄츄", buildRewardDescription(data), choices = listOf("보상 받기"))
            )
            else -> listOf(DialoguePage("츄츄", "계속 모험을 해주세요!"))
        }
    }

    private fun buildMainQuestOfferText(data: QuestData): String = when (data.questType) {
        "KILL" -> {
            val monsterName = MonsterRegistry.get(data.targetMonsterId)?.name ?: data.targetMonsterId
            "${monsterName} ${data.targetCount}마리를 처치해주세요.\n수락하시겠어요?"
        }
        "COLLECT" -> {
            val matName = runCatching { MaterialType.valueOf(data.targetMaterialId) }
                .getOrNull()?.let { MaterialCatalog.get(it).name } ?: data.targetMaterialId
            "${matName} ${data.targetCount}개를 모아다 주세요.\n수락하시겠어요?"
        }
        "ENTER_MAP" -> "해당 지역으로 직접 이동해서 조사해주세요.\n수락하시겠어요?"
        "REACH_LEVEL" -> "레벨 ${data.targetLevel}에 도달해주세요.\n수락하시겠어요?"
        else -> "'${data.name}' 퀘스트를 수락하시겠어요?"
    }

    private fun buildMainQuestProgressText(data: QuestData, quest: QuestState): String = when (data.questType) {
        "KILL"        -> "'${data.name}' 진행 중!\n${quest.mainQuestProgress} / ${data.targetCount} 처치했어요."
        "COLLECT"     -> {
            val matName = runCatching { MaterialType.valueOf(data.targetMaterialId) }
                .getOrNull()?.let { MaterialCatalog.get(it).name } ?: data.targetMaterialId
            "'${data.name}' 진행 중!\n${matName} ${quest.mainQuestProgress} / ${data.targetCount}개 수집했어요."
        }
        "REACH_LEVEL" -> "'${data.name}'\n레벨 ${data.targetLevel}에 도달해주세요!"
        "ENTER_MAP"   -> "'${data.name}'\n해당 지역으로 이동해주세요."
        else          -> "'${data.name}' 진행 중입니다."
    }

    private fun buildRewardDescription(data: QuestData): String {
        val parts = mutableListOf<String>()
        if (data.rewardExp > 0)   parts.add("EXP +${data.rewardExp}")
        if (data.rewardMoney > 0) parts.add("${data.rewardMoney}원")
        when (data.rewardItemId) {
            "RANDOM_SCROLL_BOX" -> parts.add("랜덤 주문서 ×3")
            "GLOVE_ATK_100"     -> parts.add("장갑 공격력 100% 주문서")
            "SWORD_ATK_100"     -> parts.add("무기 공격력 100% 주문서")
            ""                  -> { /* no item */ }
            else                -> parts.add(data.rewardItemId)
        }
        return "보상: ${parts.joinToString(", ")}"
    }

    private fun giveRewardItem(state: UiState, itemId: String): UiState {
        if (itemId.isBlank()) return state
        if (itemId == "RANDOM_SCROLL_BOX") {
            val pool = listOf(
                ScrollType.GLOVE_ATK_100, ScrollType.GLOVE_ATK_60, ScrollType.GLOVE_ATK_10,
                ScrollType.SWORD_ATK_100, ScrollType.SWORD_ATK_60
            )
            var s = state
            repeat(3) { s = s.copy(inventorySlots = addScrollToSlots(s.inventorySlots, pool.random())) }
            return s
        }
        runCatching { ScrollType.valueOf(itemId) }.getOrNull()?.let {
            return state.copy(inventorySlots = addScrollToSlots(state.inventorySlots, it))
        }
        return state
    }

    private fun startNextMainQuest(state: UiState, nextQuestId: String): UiState {
        if (nextQuestId.isBlank()) return state
        QuestRegistry.get(nextQuestId) ?: return state
        return state.copy(
            questState = state.questState.copy(
                mainQuestId       = nextQuestId,
                mainQuestProgress = 0,
                mainQuestStatus   = QuestStatus.NOT_STARTED,
            )
        )
    }

    // 퀘스트 수락 시 호출 — COLLECT는 인벤 기존 수량으로 초기 진행도 동기화
    private fun acceptMainQuest(state: UiState): UiState {
        val qData = QuestRegistry.get(state.questState.mainQuestId) ?: return state
        val initProgress = if (qData.questType == "COLLECT" && qData.targetMaterialId.isNotBlank()) {
            val matType = runCatching { MaterialType.valueOf(qData.targetMaterialId) }.getOrNull()
            if (matType != null) {
                state.inventorySlots
                    .filterIsInstance<InventorySlot.MaterialItem>()
                    .filter { it.type == matType }
                    .sumOf { it.quantity }
                    .coerceAtMost(qData.targetCount)
            } else 0
        } else 0
        val initStatus = if (initProgress >= qData.targetCount) QuestStatus.READY_TO_COMPLETE
                         else QuestStatus.IN_PROGRESS
        return state.copy(
            questState = state.questState.copy(
                mainQuestProgress = initProgress,
                mainQuestStatus   = initStatus,
            )
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
