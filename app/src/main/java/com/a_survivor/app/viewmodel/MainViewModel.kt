package com.a_survivor.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.a_survivor.app.R
import com.a_survivor.app.model.DamageNumber
import com.a_survivor.app.model.DefaultWeapon
import com.a_survivor.app.model.DefaultWorld
import com.a_survivor.app.model.DerivedStats
import com.a_survivor.app.model.DropItem
import com.a_survivor.app.model.EnhancementResult
import com.a_survivor.app.model.Equipment
import com.a_survivor.app.model.GameWorld
import com.a_survivor.app.model.GroundItem
import com.a_survivor.app.model.MapType
import com.a_survivor.app.model.Monster
import com.a_survivor.app.model.MonsterState
import com.a_survivor.app.model.Player
import com.a_survivor.app.model.PlayerJob
import com.a_survivor.app.model.Portal
import com.a_survivor.app.model.PortalRegistry
import com.a_survivor.app.model.ScrollCatalog
import com.a_survivor.app.model.ScrollType
import com.a_survivor.app.model.StatType
import com.a_survivor.app.model.SlimeDropTable
import com.a_survivor.app.model.Weapon
import com.a_survivor.app.service.AutoAttackService
import com.a_survivor.app.service.DerivedStatsCalculator
import com.a_survivor.app.service.DropService
import com.a_survivor.app.service.EnhancementService
import com.a_survivor.app.service.LevelService
import com.a_survivor.app.service.MonsterAiService
import com.a_survivor.app.service.MonsterSpawner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class InventoryItem(val scrollType: ScrollType, val quantity: Int)

data class PendingRespawn(val monsterId: Int, val diedAt: Long)

data class UiState(
    val equipment: Equipment?,
    val weapon: Weapon?,
    val inventory: List<InventoryItem>,
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
    val jobAdvancementPending: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val service                  = EnhancementService()
    private val autoAttackService        = AutoAttackService()
    private val monsterAiService         = MonsterAiService()
    private val levelService             = LevelService()
    private val dropService              = DropService()
    private val derivedStatsCalculator   = DerivedStatsCalculator()

    private var nextGroundItemId = 0

    // 충돌 판정용 저해상도 비트맵 (원본 1/4 크기)
    private val collisionBitmap: Bitmap? by lazy {
        val opts = BitmapFactory.Options().apply {
            inSampleSize       = 4
            inPreferredConfig  = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(application.resources, R.drawable.map_beginner, opts)
    }
    private val townCollisionBitmap: Bitmap? by lazy {
        val opts = BitmapFactory.Options().apply {
            inSampleSize       = 4
            inPreferredConfig  = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(application.resources, R.drawable.map_town, opts)
    }

    companion object {
        private const val MOVE_SPEED             = 2f
        private const val AUTO_ATTACK_INTERVAL   = 1000L
        private const val AI_TICK_INTERVAL       = 16L
        private const val RESPAWN_DELAY            = 5000L
        private const val RESPAWN_CHECK_INTERVAL  = 1000L
        private const val DAMAGE_NUMBER_DURATION  = 800L
        private const val PICKUP_RANGE            = 50f
        private const val PICKUP_DELAY            = 1500L  // 드랍 후 1.5초 뒤부터 픽업 가능
        private const val COLLISION_RADIUS       = 10f
        private const val LUMINANCE_THRESHOLD    = 80f
        private const val PORTAL_RANGE           = 30f
        private const val PORTAL_COOLDOWN        = 2000L
    }

    init {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_ATTACK_INTERVAL)
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
                delay(RESPAWN_CHECK_INTERVAL)
                respawnTick()
            }
        }
    }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var nextMonsterId      = (_uiState.value.monsters.maxOfOrNull { it.id } ?: 0) + 1
    private var nextDamageNumberId = 0

    fun startGame() {
        _uiState.value = createInitialState()
    }

    fun advanceJob(job: PlayerJob) {
        _uiState.update { state ->
            computeDerived(
                state.copy(
                    player = state.player.copy(job = job),
                    jobAdvancementPending = false
                )
            )
        }
    }

    private fun computeDerived(state: UiState): UiState =
        state.copy(
            derivedStats = derivedStatsCalculator.calculate(state.player, state.equipment)
        )

    private fun createInitialState(job: PlayerJob = PlayerJob.BEGINNER): UiState {
        val initialPlayer = Player(job = job, stats = job.initialStats())
        val initialEquip  = Equipment(
            name = "노가다 목장갑",
            attackPower = 0,
            maxUpgradeCount = 5,
            remainingUpgradeCount = 5,
            failedUpgradeCount = 0,
            destroyed = false,
            description = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
        )
        val base = UiState(
            monsters = MonsterSpawner().spawnSlimes(
                world     = DefaultWorld,
                count     = 5,
                isBlocked = { x, y -> isBlocked(x, y, DefaultWorld) }
            ),
            portals   = PortalRegistry.portalsFor(MapType.BEGINNER_FIELD),
            weapon    = DefaultWeapon,
            equipment = initialEquip,
            player    = initialPlayer,
            inventory = listOf(
                InventoryItem(ScrollType.GLOVE_ATK_100, 0),
                InventoryItem(ScrollType.GLOVE_ATK_60, 0),
                InventoryItem(ScrollType.GLOVE_ATK_10, 0),
                InventoryItem(ScrollType.WHITE_SCROLL_1, 0),
                InventoryItem(ScrollType.WHITE_SCROLL_3, 0)
            )
        )
        return computeDerived(base)
    }

    // ── 충돌 판정 ──────────────────────────────────────────────────────────────

    /** 플레이어 반경 내 5개 지점 중 하나라도 막혀 있으면 true */
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

    // ── 이동 (충돌 + 벽 슬라이딩) ──────────────────────────────────────────────

    fun movePlayer(dirX: Float, dirY: Float) {
        _uiState.update { state ->
            val p     = state.player
            val world = state.world

            val rawX = p.positionX + dirX * MOVE_SPEED
            val rawY = p.positionY + dirY * MOVE_SPEED
            val (clampedX, clampedY) = world.clampPosition(rawX, rawY)

            // 벽 슬라이딩: X / Y 축 독립 판정
            val newX = if (!isBlocked(clampedX, p.positionY, world)) clampedX else p.positionX
            val newY = if (!isBlocked(newX,     clampedY,    world)) clampedY else p.positionY

            val moved = state.copy(player = p.copy(positionX = newX, positionY = newY))
            val afterPickup = checkPickup(moved)

            val now = System.currentTimeMillis()
            if (now - afterPickup.lastTeleportAt < PORTAL_COOLDOWN) return@update afterPickup

            val nearPortal = afterPickup.portals.firstOrNull { portal ->
                val dx = newX - portal.worldX
                val dy = newY - portal.worldY
                sqrt(dx * dx + dy * dy) <= PORTAL_RANGE
            }
            if (nearPortal != null) teleportState(afterPickup, nearPortal, now) else afterPickup
        }
    }

    private fun teleportState(state: UiState, portal: Portal, now: Long): UiState {
        val targetWorld = GameWorld(mapType = portal.targetMap)
        val newMonsters = if (portal.targetMap == MapType.BEGINNER_FIELD)
            MonsterSpawner().spawnSlimes(
                world     = targetWorld,
                count     = 5,
                isBlocked = { x, y -> isBlocked(x, y, targetWorld) }
            )
        else emptyList()

        return state.copy(
            player          = state.player.copy(positionX = portal.targetX, positionY = portal.targetY),
            world           = targetWorld,
            monsters        = newMonsters,
            groundItems     = emptyList(),
            pendingRespawns = emptyList(),
            portals         = PortalRegistry.portalsFor(portal.targetMap),
            lastTeleportAt  = now
        )
    }

    // ── 자동 공격 틱 ───────────────────────────────────────────────────────────

    private fun autoAttackTick() {
        _uiState.update { state ->
            val result = autoAttackService.tick(
                player       = state.player,
                equipment    = state.equipment,
                monsters     = state.monsters,
                derivedStats = state.derivedStats
            )

            // 피격 몬스터가 IDLE이면 AGGRO로 전환
            val killedIds = result.killedMonsters.map { it.id }.toSet()
            val monstersWithAggro = result.updatedMonsters.map { m ->
                if (m.id == result.targetId && m.id !in killedIds && m.state == MonsterState.IDLE)
                    m.copy(state = MonsterState.AGGRO)
                else m
            }

            val gainedExp     = result.killedMonsters.sumOf { it.expReward }
            val updatedPlayer = if (gainedExp > 0)
                levelService.applyExp(state.player, gainedExp)
            else
                state.player

            val now            = System.currentTimeMillis()
            val newGroundItems = mutableListOf<GroundItem>()
            result.killedMonsters.forEach { monster ->
                val drops = dropService.roll(SlimeDropTable.entries)
                drops.forEachIndexed { index, drop ->
                    val angle  = index * (Math.PI * 2.0 / drops.size.coerceAtLeast(1)).toFloat()
                    val spread = if (drops.size > 1) 20f else 0f
                    newGroundItems.add(
                        GroundItem(
                            id        = nextGroundItemId++,
                            positionX = monster.positionX + cos(angle) * spread,
                            positionY = monster.positionY + sin(angle) * spread,
                            dropItem  = drop,
                            droppedAt = now
                        )
                    )
                }
            }

            val newPending = result.killedMonsters.map { PendingRespawn(it.id, now) }

            // 공격 데미지/MISS 숫자
            val targetMonster = state.monsters.find { it.id == result.targetId }
            val attackDmgNum  = if (targetMonster != null) {
                when {
                    result.isMiss  -> DamageNumber(
                        nextDamageNumberId++, 0,
                        targetMonster.positionX, targetMonster.positionY, now,
                        isPlayerDamage = false, isMiss = true
                    )
                    result.damage > 0 -> DamageNumber(
                        nextDamageNumberId++, result.damage,
                        targetMonster.positionX, targetMonster.positionY, now,
                        isPlayerDamage = false
                    )
                    else -> null
                }
            } else null

            // 레벨 3 도달 시 전직 팝업 트리거
            val advancePending = state.jobAdvancementPending || (
                gainedExp > 0 &&
                updatedPlayer.job == PlayerJob.BEGINNER &&
                updatedPlayer.level >= 3 &&
                state.player.level < 3
            )

            val finalState = state.copy(
                monsters              = monstersWithAggro,
                player                = updatedPlayer,
                groundItems           = state.groundItems + newGroundItems,
                pendingRespawns       = state.pendingRespawns + newPending,
                damageNumbers         = state.damageNumbers + listOfNotNull(attackDmgNum),
                jobAdvancementPending = advancePending
            )
            if (gainedExp > 0) computeDerived(finalState) else finalState
        }
    }

    // ── 몬스터 리스폰 틱 ───────────────────────────────────────────────────────

    private fun respawnTick() {
        _uiState.update { state ->
            val now = System.currentTimeMillis()
            val (ready, waiting) = state.pendingRespawns.partition {
                now - it.diedAt >= RESPAWN_DELAY
            }
            if (ready.isEmpty()) return@update state

            val newMonsters = ready.mapNotNull {
                MonsterSpawner().spawnSlimes(
                    world     = state.world,
                    count     = 1,
                    isBlocked = { x, y -> isBlocked(x, y, state.world) }
                ).firstOrNull()?.copy(id = nextMonsterId++)
            }

            state.copy(
                monsters        = state.monsters + newMonsters,
                pendingRespawns = waiting
            )
        }
    }

    private fun monsterAiTick() {
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

            // 피격 또는 회피 숫자 생성
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

            // 만료된 데미지 숫자 정리
            val activeDmgNums = state.damageNumbers
                .filter { now - it.createdAt < DAMAGE_NUMBER_DURATION }

            val newState = state.copy(
                monsters      = aiResult.updatedMonsters,
                player        = state.player.copy(hp = newHp),
                damageNumbers = activeDmgNums + listOfNotNull(playerDmgNum)
            )
            checkPickup(newState)
        }
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

    private fun applyDrops(state: UiState, drops: List<DropItem>): UiState {
        var s = state
        for (drop in drops) {
            s = when (drop) {
                is DropItem.ScrollDrop -> s.copy(
                    inventory = s.inventory.map { item ->
                        if (item.scrollType == drop.scrollType)
                            item.copy(quantity = item.quantity + 1)
                        else item
                    }
                )
                is DropItem.EquipmentDrop -> {
                    if (s.equipment == null) s.copy(equipment = drop.equipment) else s
                }
            }
        }
        return s
    }

    // ── 강화 시스템 ────────────────────────────────────────────────────────────

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

        val item = state.inventory.find { it.scrollType == scrollType }
        if (item == null || item.quantity <= 0) {
            _uiState.update { it.copy(lastResult = EnhancementResult.Error("주문서 수량이 부족합니다.")) }
            return
        }

        val (newEquipment, result) = service.applyScroll(equipment, scroll)
        val newInventory = state.inventory.map {
            if (it.scrollType == scrollType) it.copy(quantity = it.quantity - 1) else it
        }
        _uiState.update { s ->
            computeDerived(s.copy(equipment = newEquipment, inventory = newInventory, lastResult = result))
        }
    }

    fun unequipEquipment() {
        _uiState.update { s ->
            computeDerived(s.copy(equipment = null, selectedScrollType = null, lastResult = null))
        }
    }

    fun resetEquipment() {
        _uiState.update { s ->
            computeDerived(
                s.copy(
                    equipment = Equipment(
                        name = "노가다 목장갑",
                        attackPower = 0,
                        maxUpgradeCount = 5,
                        remainingUpgradeCount = 5,
                        failedUpgradeCount = 0,
                        destroyed = false,
                        description = "노동을 위해 만들어진 낡은 장갑이다.\n강화하면 공격력이 오를 것 같다."
                    ),
                    selectedScrollType = null,
                    lastResult = null
                )
            )
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
            val updated = state.copy(
                player = player.copy(
                    stats              = newStats,
                    availableStatPoint = player.availableStatPoint - 1
                )
            )
            computeDerived(updated)
        }
    }

    fun unequipWeapon() { _uiState.update { computeDerived(it.copy(weapon = null)) } }
    fun resetWeapon()   { _uiState.update { computeDerived(it.copy(weapon = DefaultWeapon)) } }
}
