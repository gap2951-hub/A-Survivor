# A-Survivor HandOff

## 프로젝트 개요

메이플스토리 스타일의 픽셀아트 사냥터를 배경으로 한 안드로이드 생존형 게임.
주문서 강화 시스템, 몬스터 AI, 픽셀 충돌, 마을/포탈 시스템, NPC/퀘스트 시스템, 스켈레톤 워리어 애니메이션, 전사 플레이어 스프라이트 애니메이션(히트 프레임 시스템 포함), 인벤토리 아이템 이미지화 구현 완료.

- **패키지명:** `com.a_survivor.app`
- **언어:** Kotlin + Jetpack Compose
- **minSdk:** 24 / **targetSdk:** 36
- **GitHub:** https://github.com/gap2951-hub/A-Survivor

---

## 아키텍처

```
com.a_survivor.app/
├── MainActivity.kt               (+ dispatchKeyEvent PC 방향키/WASD 지원)
├── model/
│   ├── Equipment.kt              (+ 스탯 보정 필드 + 전투 능력치 보정 필드)
│   ├── Scroll.kt / EnhancementResult.kt
│   ├── Player.kt                 (+ facingLeft: Boolean)
│   ├── PlayerJob.kt              (+ attackType/attackRange/projectileType/projectileSpeed 확장함수)
│   ├── PlayerStats.kt            (+ StatType enum)
│   ├── Weapon.kt                 (+ DefaultWeapon)
│   ├── GameWorld.kt              (1024×572 + MapType enum)
│   ├── Monster.kt                (+ skeletonWarrior() + distanceTo() + MonsterState + avoidability/accuracy + facingLeft + variant)
│   ├── MonsterState.kt           (IDLE / AGGRO / ATTACKING)
│   ├── CameraState.kt            (좌표 변환 / 추적)
│   ├── DropTable.kt              (DropItem + SlimeDropTable)
│   ├── GroundItem.kt             (바닥 드랍 아이템 + droppedAt 타임스탬프)
│   ├── DamageNumber.kt           (데미지 숫자 floating 표시 + isMiss 필드)
│   ├── DerivedStats.kt           (전투 파생 능력치 모델)
│   ├── Portal.kt                 (Portal 모델 + PortalRegistry)
│   ├── AttackType.kt             (MELEE / PROJECTILE)
│   ├── ProjectileType.kt         (ENERGY_BOLT / ARROW / THROWING_STAR / BULLET)
│   ├── Projectile.kt             (투사체 모델 + traveledDistance / maxTravelDistance)
│   ├── Npc.kt                    (Npc 데이터 클래스 + NpcRegistry)
│   ├── QuestState.kt             (QuestStatus enum + QuestState)
│   ├── DialogueState.kt          (DialoguePage + DialogueSession)
│   └── GameMessage.kt            (MessageType enum + GameMessage 데이터 클래스)
├── service/
│   ├── EnhancementService.kt
│   ├── CombatStatCalculator.kt   (레거시, 미사용)
│   ├── DerivedStatsCalculator.kt (직업별 스탯→전투 능력치 계산)
│   ├── MonsterSpawner.kt         (spawnMonsters — variant/hp/exp/avoidability/accuracy/speed 파라미터)
│   ├── AutoAttackService.kt      (근접/원거리 분기 + 직업별 공격 범위)
│   ├── ProjectileService.kt      (투사체 이동 + 충돌 판정)
│   ├── MonsterAiService.kt       (추적 / 공격 / 어그로 해제 + 회피 판정)
│   ├── LevelService.kt
│   ├── DropService.kt
│   ├── GameSaveData.kt           (GameSaveData + SavedSlot 직렬화 데이터 클래스)
│   └── SaveService.kt            (Gson 직렬화 + SharedPreferences 저장/로드)
├── viewmodel/
│   └── MainViewModel.kt          (AndroidViewModel — 충돌 비트맵 + AI/투사체 틱 루프 + DerivedStats + PendingPlayerAttack + pendingAttackTick + 자동 저장)
└── res/drawable/
    ├── map_beginner.jpg           ← 초보자 사냥터 맵 (1024×572)
    ├── map_town.jpg               ← 마을 맵 (1024×572)
    ├── energy_bolt_1.png ~ energy_bolt_3.png  ← 에너지볼트 3프레임
    ├── skeleton_idle_0~5.png      ← 스켈레톤 Crusader_1 Idle (6프레임)
    ├── skeleton_walk_0~7.png      ← 스켈레톤 Crusader_1 Walk (8프레임)
    ├── skeleton_slash_0~5.png     ← 스켈레톤 Crusader_1 Slash (6프레임)
    ├── skeleton2_idle/walk/slash  ← Crusader_2 (각 6/8/6프레임)
    ├── skeleton3_idle/walk/slash  ← Crusader_3 (각 6/8/6프레임)
    ├── nogada_glove.png
    ├── nogada_sword.png
    ├── scroll_100.png / scroll_60.png / scroll_10.png
    ├── npc_chuchu.png             ← 마을 NPC 츄츄 이미지
    ├── warrior_idle_0~3.png       ← 전사 Idle 4프레임 (200ms/frame)
    ├── warrior_walk_0~3.png       ← 전사 Walk 4프레임 (100ms/frame)
    ├── warrior_attack_0~4.png     ← 전사 Attack 5프레임 (60ms/frame)
    ├── warrior_hurt_0~2.png       ← 전사 Hurt 3프레임 (100ms/frame)
    ├── warrior_die_0~5.png        ← 전사 Die 6프레임 (200ms/frame)
    ├── coin_0~3.png               ← 동전 애니메이션 4프레임 (150ms 순환)
    └── archer_sheet.png           ← 궁수 스프라이트시트 (4900×109, 140px/frame)
```

---

## 화면 구조

```
Box (게임 화면)
 ├── GameCanvas              ← Canvas 기반 게임 렌더링 (fillMaxSize)
 ├── GameHud (좌상단)         ← Lv. 직업명 + HP 바 + EXP 바
 ├── QuestTrackerPanel (좌상단 HUD 아래) ← 퀘스트 진행 중/완료 가능 시만 표시
 ├── ResultPanel (상단 중앙)  ← 강화 결과 (있을 때만)
 ├── Column + zIndex(10) > EquipmentWindow  ← 플로팅 드래그 창
 ├── Column + zIndex(10) > StatWindow       ← 플로팅 드래그 창
 ├── Column + zIndex(10) > InventoryWindow  ← 플로팅 드래그 창
 ├── HudButton ×3 (우하단)   ← 스탯 / 장비 / 인벤
 ├── JoystickControl (좌하단)
 ├── DragGhost
 ├── JobAdvancementDialog  ← jobAdvancementPending=true 일 때만 표시 (전직 팝업 오버레이)
 ├── MessageLogOverlay (우하단) ← EXP/돈/아이템 획득 메시지 (최대 5개, 2초 자동 소멸)
 ├── [대화하기] Button (하단 중앙) ← NPC 근접 시만 표시 (activeDialogue==null)
 └── DialogueWindow (zIndex 20, 하단 전체폭) ← activeDialogue != null 시 표시
```

---

## 화면 방향 / 전체화면

- **방향:** `sensorLandscape` (AndroidManifest)
- **전체화면 몰입 모드:** `WindowInsetsControllerCompat.hide(systemBars)` + `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`
- `WindowCompat.setDecorFitsSystemWindows(window, false)` 적용

---

## 렌더링 시스템 (GameCanvas)

### 렌더 순서

| 레이어 | 내용 |
|--------|------|
| 1 | `drawWorldBackground` — 현재 맵 이미지를 월드 전체에 렌더링 |
| 2 | `drawGroundItem` × N — 바닥 드랍 아이템 (글로우 → 이미지 → 이름 텍스트) |
| 3 | `drawPortal` × N — 포탈 (다층 파란 글로우 + 맵 이름 레이블) |
| 4 | `drawNpc` × N — NPC 이미지 (size.height×0.20f 높이, 너비=높이×1.6) + 이름 텍스트 |
| 5 | `drawAttackRange` — 공격 범위 원 (반투명 흰색, 직업별 반경) |
| 6 | `drawMonster` × N — 그림자 → 스켈레톤 애니메이션(variant별 프레임) → HP 바 → 어그로 "!" |
| 7 | `drawProjectile` × N — 직업별 투사체 (임시 도형) |
| 8 | `drawPlayer` — 그림자 → 전사 스프라이트 (IDLE/WALK/ATTACK/HURT/DIE 상태별 애니메이션, facingLeft 수평 반전) |
| 9 | `drawDamageNumber` × N — 데미지 숫자 (노랑: 플→몬, 빨강: 몬→플) |

### 시각 사양 (화면 비례 크기)

| 대상 | 표현 방식 | 크기 |
|------|-----------|------|
| 플레이어 (전사) | 스프라이트 애니메이션 (IDLE 4f / WALK 4f / ATTACK 5f / HURT 3f / DIE 6f) | `size.height * 0.11f` (정사각형) |
| 스켈레톤 (IDLE) | Idle 애니메이션 6프레임 + 초록 HP 바 | `size.height * 0.15f` |
| 스켈레톤 (AGGRO) | Walk 애니메이션 8프레임 + 주황 HP 바 + "!" | `size.height * 0.15f` |
| 스켈레톤 (ATTACKING) | Slash 애니메이션 6프레임 + 주황 HP 바 + "!" | `size.height * 0.15f` |
| NPC 츄츄 | PNG 이미지 + 이름 텍스트 | 높이 `size.height * 0.20f`, 너비 `높이 * 1.6` |
| 바닥 아이템 | PNG 이미지 + 이름 텍스트 | `size.height * 0.048f` |
| 포탈 | 다층 파란 글로우 링 + 레이블 | `24f * cam.zoom` |
| 데미지 숫자 | 노랑 (플→몬) / 빨강 (몬→플) | `20f or 17f * (size.height/1080f)` |

> 스프라이트 크기는 `cam.zoom` 기반이 아닌 `size.height` 비례로 설정 (zoom 변화 시 크기 불변)

### 카메라

```kotlin
val zoom = maxOf(size.width / world.width, size.height / world.height)
val cam = CameraState(zoom = zoom)
    .followPlayer(player.positionX, player.positionY)
    .clampToWorld(world, size.width, size.height)
```

---

## 맵 시스템

### MapType

```kotlin
enum class MapType { BEGINNER_FIELD, TOWN, FIELD_2, FIELD_3 }
```

### 맵별 설정

| 맵 | 이미지 | 월드 크기 | 몬스터 | 스켈레톤 variant |
|----|--------|-----------|--------|-----------------|
| BEGINNER_FIELD | map_beginner.jpg | 1024×572 | 스켈레톤 워리어 5마리 (HP 20, EXP 8) | Crusader_2 |
| TOWN | map_town.jpg | 1024×572 | 없음 | — |
| FIELD_2 | map_beginner.jpg | 1024×572 | 스켈레톤 워리어 5마리 (HP 60, EXP 20) | Crusader_3 |
| FIELD_3 | map_beginner.jpg | 1024×572 | 스켈레톤 워리어 5마리 (HP 150, EXP 45) | Crusader_1 |

### 픽셀 충돌 시스템 (MainViewModel)

```kotlin
// 맵 타입에 따라 충돌 비트맵 선택
private val collisionBitmap: Bitmap? by lazy { /* map_beginner, inSampleSize=4 */ }
private val townCollisionBitmap: Bitmap? by lazy { /* map_town, inSampleSize=4 */ }

private fun isPixelBlocked(...): Boolean {
    val bmp = when (world.mapType) {
        MapType.TOWN -> townCollisionBitmap
        else         -> collisionBitmap
    } ?: return false
    ...
}
```

- 루미넌스 = 0.299R + 0.587G + 0.114B
- **LUMINANCE_THRESHOLD = 80f**: 나무 트렁크(lum<77) 차단 / 잔디·연결로(lum≥80) 통과
- `isBlocked`: 중심 + 상하좌우 10f 지점 5곳 중 하나라도 막히면 true
- `movePlayer`: X/Y 축 독립 판정 → 벽 슬라이딩

---

## 포탈 시스템

### Portal 모델

```kotlin
data class Portal(
    val worldX: Float, val worldY: Float,
    val targetMap: MapType,
    val targetX: Float, val targetY: Float,  // 도착 좌표
    val label: String
)
```

### 포탈 위치 (PortalRegistry)

| 맵 | 포탈 위치 | 목적지 | 도착 위치 |
|----|-----------|--------|-----------|
| BEGINNER_FIELD | (850, 286) | TOWN | (350, 286) |
| BEGINNER_FIELD | (174, 286) | FIELD_2 | (800, 286) |
| TOWN | (250, 286) | BEGINNER_FIELD | (750, 286) |
| FIELD_2 | (850, 286) | BEGINNER_FIELD | (300, 286) |
| FIELD_2 | (174, 286) | FIELD_3 | (800, 286) |
| FIELD_3 | (850, 286) | FIELD_2 | (300, 286) |

포탈 체인: `FIELD_3 ←→ FIELD_2 ←→ BEGINNER_FIELD ←→ TOWN` (왼쪽이 더 고급 구역)

> 마을 포탈이 x=250에 있는 이유: x=144(lum=68)·x=168~176(lum=30~70) 장애물 때문에
> x=180 이전은 접근 불가 함정 지대. x=250(lum=146)부터 완전 개활지.

### 포탈 동작

- 진입 범위: `PORTAL_RANGE = 30f`
- 쿨다운: `PORTAL_COOLDOWN = 2000ms` (역방향 즉시 이동 방지)
- 맵 전환 시: 몬스터·드랍·리스폰 전부 초기화
- 사냥터 맵 진입 시: 해당 맵 `skeletonConfig`에 따라 스켈레톤 5마리 스폰

---

## NPC 시스템

### 모델

```kotlin
data class Npc(
    val id: Int,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val interactRange: Float = 14f   // 대화하기 버튼 표시 범위 (월드 유닛)
)

object NpcRegistry {
    fun npcsFor(mapType: MapType): List<Npc> = when (mapType) {
        MapType.TOWN -> listOf(Npc(id = 1, name = "츄츄", worldX = 450f, worldY = 260f))
        else -> emptyList()
    }
}
```

### 등록 NPC

| ID | 이름 | 맵 | 위치 | 역할 |
|----|------|----|------|------|
| 1 | 츄츄 | TOWN | (450, 260) | 슬라임 소탕 퀘스트 의뢰 |

### 대화 시스템

```kotlin
data class DialoguePage(
    val speaker: String,
    val text: String,
    val choices: List<String> = emptyList()  // 비어있으면 [다음/닫기] 버튼
)

data class DialogueSession(
    val pages: List<DialoguePage>,
    val currentIndex: Int = 0
) {
    val currentPage get() = pages[currentIndex]
    val isLastPage get() = currentIndex >= pages.size - 1
}
```

### UiState 추가 필드

```kotlin
val npcs: List<Npc> = emptyList(),
val questState: QuestState = QuestState(),
val activeDialogue: DialogueSession? = null,
```

- `npcs` — 맵 전환 시 `NpcRegistry.npcsFor(portal.targetMap)`으로 갱신 (questState는 유지)

### ViewModel 함수

| 함수 | 동작 |
|------|------|
| `startDialogue(npcId)` | 현재 questState에 따라 대화 페이지 빌드 후 activeDialogue 설정 |
| `nextDialoguePage()` | 다음 페이지 이동 / 마지막 페이지면 닫기 / 선택지 페이지면 무시 |
| `chooseDialogueOption(index)` | 선택지 처리 (수락/거절/보상받기) |
| `closeDialogue()` | activeDialogue = null |

---

## 퀘스트 시스템

### 모델

```kotlin
enum class QuestStatus { NOT_STARTED, IN_PROGRESS, READY_TO_COMPLETE, COMPLETED }

data class QuestState(
    val status: QuestStatus = QuestStatus.NOT_STARTED,
    val killCount: Int = 0,
    val killGoal: Int = 5
)
```

### 퀘스트: 스켈레톤 소탕 작전

| 단계 | 조건 | 동작 |
|------|------|------|
| NOT_STARTED | 츄츄에게 대화 | 4페이지 + 수락/거절 선택지 |
| IN_PROGRESS | 스켈레톤 처치마다 | killCount++ (autoAttackTick + projectileTick 양쪽) |
| READY_TO_COMPLETE | killCount >= 5 | 자동 전환 |
| COMPLETED | 츄츄에게 대화 후 보상 수령 | EXP +50, 장갑 공격력 100% 주문서 +1 |

### 대화 시나리오

**NOT_STARTED (4페이지 + 선택지):**
1. "안녕하세요! 혹시 모험가이신가요?"
2. "요즘 마을 주변이 이상해졌어요..."
3. "주민들이 밭을 관리하러 나가지도 못하고 있어요..."
4. "혹시 저를 도와주실 수 있나요?" → [도와준다] / [거절한다]
   - 수락: "정말 감사합니다!..." / "스켈레톤 워리어 5마리를 처치하면..." → 닫기
   - 거절: "그렇군요... 마음이 바뀌면..." → 닫기

**IN_PROGRESS (1페이지):** "스켈레톤 워리어들이 아직 남아있어요. (N/5)" → 닫기

**READY_TO_COMPLETE (4페이지 + 선택지):**
1. "정말 해내셨군요!"
2. "주민들이 다시 밭으로 나갈 수 있게 되었어요."
3. "모두가 모험가님 덕분이라며 감사하고 있어요."
4. "이건 작은 감사의 표시예요." → [보상 받기] → EXP +50 + 주문서 지급 + COMPLETED

**COMPLETED (1페이지):** "덕분에 평화로워졌어요. 정말 감사합니다!" → 닫기

### 퀘스트 트래커 UI (QuestTrackerPanel)

- **위치:** 좌상단 HUD 아래 (padding start=12dp, top=80dp)
- **표시 조건:** IN_PROGRESS 또는 READY_TO_COMPLETE
- **내용:** 상태 레이블 + "스켈레톤 소탕 작전" + 진행 프로그레스 바 + "스켈레톤 처치 N/5" 텍스트
- **색상:** 진행 중 주황 / 완료 가능 금색 (테두리도 변경)

---

## 데미지 숫자 시스템

```kotlin
data class DamageNumber(
    val id: Int, val value: Int,
    val worldX: Float, val worldY: Float,
    val createdAt: Long,
    val isPlayerDamage: Boolean,
    val isMiss: Boolean = false
)
```

- 800ms 동안 위로 55 월드 유닛 float + 페이드아웃
- `false` (노랑): 플레이어→몬스터 / `true` (빨강): 몬스터→플레이어
- `isMiss=true` (하늘색 `"MISS"`): 명중 실패 / 회피 성공

---

## 화면 흐름 (AppScreen)

```
AppScreen.Title    → "게임 시작" 클릭 → vm.startGame() → AppScreen.Game
AppScreen.Game     → 초보자(BEGINNER)로 시작, 레벨 3 도달 시 JobAdvancementDialog 자동 표시
AppScreen.JobSelect → NPC 전직 연동용으로 코드 유지 (현재 미사용)
```

---

## 전직 시스템

### 흐름

| 조건 | 동작 |
|------|------|
| 시작 | 항상 BEGINNER(초보자)로 시작 |
| 레벨 3 도달 | `jobAdvancementPending = true` → 전직 팝업 표시 |
| 전직 선택 | `advanceJob(job)` — 직업 변경, 투자 포인트 반환, DerivedStats 재계산 |

### JobAdvancementDialog

- 게임 화면 위에 오버레이 (반투명 검정 배경 78%)
- 왼쪽: 5개 직업 카드 2행 그리드 (WARRIOR/MAGE/ARCHER / THIEF/PIRATE)
- 오른쪽: 선택 직업 상세 (이름·태그라인·초기 스탯·성장 방향)
- 하단: `전직하기` 확인 버튼 (선택 직업 색상)
- 팝업 닫기 버튼 없음 — 직업 선택 필수

### UiState 추가 필드

```kotlin
val jobAdvancementPending: Boolean = false
```

### 트리거 로직 (autoAttackTick)

```kotlin
val advancePending = state.jobAdvancementPending || (
    gainedExp > 0 &&
    updatedPlayer.job == PlayerJob.BEGINNER &&
    updatedPlayer.level >= 3 &&
    state.player.level < 3
)
```

---

## 플레이어 시스템

| 필드 | 초기값 |
|------|--------|
| level / exp | 1 / 0 |
| hp / maxHp | 100 / 100 |
| job | BEGINNER (초보자) |
| availableStatPoint | 0 |
| positionX / positionY | 512f / 286f |

### 직업 및 초기 스탯

| 직업 | 한국명 | STR | DEX | INT | LUK |
|------|--------|-----|-----|-----|-----|
| BEGINNER | 초보자 | 10 | 10 | 10 | 10 |
| WARRIOR  | 전사   | 20 | 5  | 4  | 4  |
| MAGE     | 마법사 | 4  | 4  | 20 | 5  |
| ARCHER   | 궁수   | 5  | 20 | 4  | 4  |
| THIEF    | 도적   | 5  | 10 | 4  | 15 |
| PIRATE   | 해적   | 10 | 16 | 4  | 6  |

---

## 자동 공격 (AutoAttackService)

| 항목 | 값 |
|------|-----|
| 공격 범위 | 직업별 상이 (아래 공격 방식 참조) |
| 공격 주기 | 1초 |
| 타겟 | 범위 내 최근접 몬스터 |
| 명중 판정 | `accuracy / (accuracy + monster.avoidability)` 확률로 HIT |
| MISS 처리 | 명중 실패 시 MISS 텍스트, 투사체 미생성 |
| 근접 히트 | 히트 프레임 시스템 — 공격 시작 시 `PendingPlayerAttack` 저장, `ATTACK_ANIM_DURATION(300ms)` 후 `pendingAttackTick`이 실제 데미지 적용 |
| 원거리 히트 | 투사체 생성, 충돌 시 데미지 |

---

## 직업별 공격 방식

| 직업 | 공격 타입 | 투사체 | 공격 범위 | 데미지 기준 |
|------|----------|--------|-----------|------------|
| 초보자 | MELEE | — | 60f | attackPower |
| 전사 | MELEE | — | 60f | attackPower |
| 마법사 | PROJECTILE | ENERGY_BOLT | 170f | **magicPower** |
| 도적 | PROJECTILE | THROWING_STAR | 180f | attackPower |
| 해적 | PROJECTILE | BULLET | 190f | attackPower |
| 궁수 | PROJECTILE | ARROW | 220f | attackPower |

### ProjectileType 렌더링

| 타입 | 표현 |
|------|------|
| ENERGY_BOLT | PNG 3프레임 애니메이션 (energy_bolt_1~3, 100ms 순환, `size.height * 0.10f` 크기, 몬스터 방향 회전) |
| ARROW | 갈색 선 (길이 14px, 방향 기반) |
| THROWING_STAR | 회색 원 (5px) + 외곽 링 |
| BULLET | 검은 원 (4px) + 회색 중심 (2px) |

#### 에너지볼트 이미지 애니메이션

```kotlin
// GameCanvas 내부
val energyBoltFrames = remember { listOf(
    loadBitmap(context, R.drawable.energy_bolt_1, 128),
    loadBitmap(context, R.drawable.energy_bolt_2, 128),
    loadBitmap(context, R.drawable.energy_bolt_3, 128)
) }

// drawProjectile 내부 (ENERGY_BOLT)
val frameIndex = ((System.currentTimeMillis() / 100L) % 3).toInt()
val imgSize = (size.height * 0.10f).toInt().coerceAtLeast(24)
val angleDeg = atan2(dy, dx).toDegrees() + 180f  // +180f: 이미지 앞면 보정
withTransform({ rotate(angleDeg, pivot = c) }) {
    drawImage(bitmap, dstOffset, dstSize, filterQuality = High)
}
```

> ARROW / THROWING_STAR / BULLET은 추후 PNG 리소스로 교체 예정

### Projectile 모델

```kotlin
data class Projectile(
    val id: Int,
    val type: ProjectileType,
    val positionX: Float, val positionY: Float,
    val targetX: Float,   val targetY: Float,
    val speed: Float,
    val damage: Int,
    val traveledDistance: Float = 0f,
    val maxTravelDistance: Float = 300f,  // attackRange * 1.5f
    val targetMonsterId: Int = -1         // 중복 타겟 방지용
)
```

### 투사체 중복 타겟 방지

비행 중인 투사체가 이미 타겟하고 있는 몬스터는 다음 공격 사이클에서 타겟 제외.

```kotlin
// autoAttackTick
val lockedIds = state.projectiles.map { it.targetMonsterId }.toSet()
autoAttackService.tick(..., lockedMonsterIds = lockedIds)

// AutoAttackService.findTarget
.filter { it.id !in lockedIds && 거리 <= 공격범위 }
```

### ProjectileService

```kotlin
COLLISION_RADIUS = 16f  // 몬스터 중심과의 거리
// maxTravelDistance 초과 시 투사체 소멸
// 충돌 시: hitEvent 생성 → 데미지 적용 → 어그로 → exp/드랍/리스폰
```

### 투사체 틱 (projectileTick, 16ms 루프)

- 투사체 이동 → 충돌 감지 → 몬스터 HP 감소 → 처치 처리
- 레벨 3 전직 트리거도 projectileTick에서 감지
- 포탈 이동 시 `projectiles = emptyList()` 초기화

---

## 몬스터 AI 시스템 (MonsterAiService)

| 상태 | 전환 조건 |
|------|-----------|
| IDLE | 기본 상태 |
| AGGRO | 플레이어 자동 공격에 피격 시 |
| ATTACKING | 플레이어와 거리 ≤ 35f |
| IDLE 복귀 | 플레이어와 거리 > 500f |

| 항목 | 값 |
|------|-----|
| 이동 속도 | 맵별 상이 (초보자 1.0f / 중급 1.3f / 상급 1.5f) |
| AI 틱 | 16ms |
| 공격 범위 | 35f |
| 공격 주기 | 1000ms |
| 공격 데미지 | 5 |
| 회피 판정 | `playerAvoidability / (playerAvoidability + monster.accuracy)` 확률로 DODGE |
| MISS 처리 | 회피 성공 시 playerDodged=true, 하늘색 MISS 표시 |

---

## 몬스터 리스폰 시스템

- 처치 후 5초 뒤 유효 위치에 재스폰
- `PendingRespawn(monsterId, diedAt)` → UiState 포함
- 포탈 이동 시 `pendingRespawns` 초기화 (사냥터 복귀 시 5마리 즉시 스폰)

---

## 드랍 시스템

### dropEntriesFor(mapType) — 맵별 드랍 테이블

| 아이템 | BEGINNER_FIELD | FIELD_2 | FIELD_3 |
|--------|---------------|---------|---------|
| **돈** | 10~15원 (100%) | 15~20원 (100%) | 25~35원 (100%) |
| 노가다 목장갑 | 5% | 5% | 5% |
| 장갑 공격력 100% | 20% | 20% | 20% |
| 장갑 공격력 60% | 10% | 10% | 10% |
| 장갑 공격력 10% | 3% | 3% | 3% |
| 백의 주문서 1% | 1% | 1% | 1% |

- **MoneyDrop**: ground item으로 바닥에 드랍 → `coin_0~3.png` 4프레임 애니메이션(150ms 순환) Canvas 렌더링 → PICKUP_DELAY(2000ms) 경과 후 PICKUP_RANGE(150f) 내 자동 습득 → `UiState.money` 가산
  - 프레임 인덱스: `((now - item.droppedAt) / 150L % 4).toInt()` — 드랍 시점 기준 개별 타이머
- **스크롤/장비**: ground item으로 바닥에 드랍 → PICKUP_DELAY(2000ms) 후 PICKUP_RANGE(150f) 내 자동 습득
- 포탈 이동 시 바닥 아이템 초기화

---

## 전투 스탯 계산 (DerivedStatsCalculator)

### 계산 순서

1. 플레이어 기본 스탯 + 장비 STR/DEX/INT/LUK 보정 → 최종 스탯
2. 직업별 공식으로 attackPower/magicPower/accuracy/avoidability 계산
3. 장비 전용 능력치(physicalDefense/magicDefense/criticalRate/moveSpeed/attackSpeed) 합산

### 직업별 공식

| 직업 | 공격력/마력 | 명중률 | 회피율 |
|------|------------|--------|--------|
| 전사 | 장비공격력 + STR×0.5 + DEX×0.1 | 10 + DEX×2 | LUK×0.5 |
| 마법사 | 장비마력 + INT×0.5 + LUK×0.1 | 10 + LUK×2 + DEX | LUK + DEX×0.2 |
| 궁수/해적 | 장비공격력 + DEX×0.5 + STR×0.1 | 10 + DEX×2 | LUK×0.5 |
| 도적 | 장비공격력 + LUK×0.5 + DEX×0.1 | 10 + DEX×2 | LUK |
| 초보자 | 장비공격력 + STR×0.5 | 10 + DEX + LUK | LUK×0.3 |

> 장비 공격력 = weapon.attackPower + equipment.attackPower (강화 포함)

### DerivedStats 모델

```kotlin
data class DerivedStats(
    val attackPower: Int = 0,    // 스탯 성장
    val magicPower: Int = 0,     // 스탯 성장
    val accuracy: Int = 0,       // 스탯 성장
    val avoidability: Int = 0,   // 스탯 성장
    val physicalDefense: Int = 0, // 장비 전용
    val magicDefense: Int = 0,   // 장비 전용
    val criticalRate: Float = 0f, // 장비 전용
    val moveSpeed: Float = 0f,   // 장비 전용
    val attackSpeed: Float = 0f  // 장비 전용
)
```

### Equipment 확장 필드 (모두 기본값 0)

```kotlin
// 기본 스탯 보정
strBonus, dexBonus, intBonus, lukBonus: Int

// 전투 능력치 보정 (장비 전용)
magicPower, accuracy, avoidability,
physicalDefense, magicDefense: Int
criticalRate, moveSpeed, attackSpeed: Float
```

### UiState 변경

- `derivedStats: DerivedStats` 추가
- `computeDerived(state)` — 스탯 투자·장비 변경·강화·레벨업 시 자동 재계산

### 스켈레톤 워리어 맵별 스탯 (skeletonConfig)

| 맵 | variant | HP | EXP | avoidability | accuracy | speed |
|----|---------|-----|-----|-------------|---------|-------|
| BEGINNER_FIELD | 2 (Crusader_2) | 20 | 8 | 5 | 15 | 1.0f |
| FIELD_2 | 3 (Crusader_3) | 60 | 20 | 10 | 20 | 1.3f |
| FIELD_3 | 1 (Crusader_1) | 150 | 45 | 18 | 28 | 1.5f |

- `skeletonConfig(mapType)` — MainViewModel 내 헬퍼, 맵별 스탯 집중 관리
- `spawnSkeletons(world, count)` — 내부적으로 skeletonConfig 참조, 3곳 스폰 로직 통일

---

## MainViewModel — 주요 상수

```kotlin
MOVE_SPEED             = 2f
AUTO_ATTACK_INTERVAL   = 1000ms
AI_TICK_INTERVAL       = 16ms
RESPAWN_DELAY          = 5000ms
RESPAWN_CHECK_INTERVAL = 1000ms
DAMAGE_NUMBER_DURATION = 800ms
PICKUP_RANGE           = 150f
PICKUP_DELAY           = 2000ms
COLLISION_RADIUS       = 10f
LUMINANCE_THRESHOLD    = 80f
PORTAL_RANGE           = 30f
PORTAL_COOLDOWN        = 2000ms
ATTACK_ANIM_DURATION   = 300ms   // 5프레임 × 60ms (히트 프레임 딜레이)
```

---

## 플레이어 스프라이트 애니메이션 시스템

### UiState 추가 필드

```kotlin
val playerAttackAnimStart: Long = 0L,   // 공격 애니메이션 시작 타임스탬프
val playerHurtAnimStart: Long = 0L,     // 피격 애니메이션 시작 타임스탬프
val playerDeathTime: Long = 0L,         // 사망 타임스탬프
val pendingPlayerAttack: PendingPlayerAttack? = null,  // 히트 프레임 대기 공격
```

### PendingPlayerAttack

```kotlin
data class PendingPlayerAttack(
    val targetId: Int,
    val damage: Int,
    val isMiss: Boolean,
    val applyAt: Long   // now + ATTACK_ANIM_DURATION
)
```

### 애니메이션 상태 우선순위

```
DEAD > HURT(400ms) > ATTACK(800ms 윈도우, 60ms/frame) > WALK(이동 중) > IDLE
```

| 상태 | 조건 | 프레임 수 | 속도 |
|------|------|-----------|------|
| DIE | player.hp <= 0 | 6 | 200ms/frame (마지막 프레임에서 정지) |
| HURT | hp > 0 && now − playerHurtAnimStart < 400ms | 3 | 100ms/frame |
| ATTACK | hp > 0 && !isHurt && now − playerAttackAnimStart < 800ms | 5 | 60ms/frame |
| WALK | isMoving (조이스틱 X or Y ≠ 0) | 4 | 100ms/frame |
| IDLE | 그 외 | 4 | 200ms/frame |

### 스프라이트 플리핑

```kotlin
withTransform({ if (player.facingLeft) scale(-1f, 1f, pivot = c) }) {
    drawImage(bitmap, dstOffset, dstSize, filterQuality = FilterQuality.High)
}
```

- `movePlayer`에서 `dirX < 0f → facingLeft = true`, `dirX > 0f → facingLeft = false`

### 히트 프레임 시스템 (pendingAttackTick)

- 매 16ms 호출, `pendingPlayerAttack != null && now >= pending.applyAt` 조건 충족 시 데미지 적용
- 애니메이션 도중 몬스터가 이미 사망한 경우에도 안전하게 처리 (이미 dead → 무시)
- 피격 시 `playerHurtAnimStart`, 사망 시 `playerDeathTime` UiState 갱신

### 스프라이트 파일 (수동 커팅, image/전사 폴더 원본 기반)

| 파일 | 프레임 수 | 원본 |
|------|-----------|------|
| warrior_idle_0~3.png | 4 | IDLE_0~3.png |
| warrior_walk_0~3.png | 4 | WALK_0~3.png |
| warrior_attack_0~4.png | 5 | ATTACK_0~4.png |
| warrior_hurt_0~2.png | 3 | HURT_0~2.png |
| warrior_die_0~5.png | 6 | auto-cut |

---

## 인벤토리 시스템

### InventoryWindow 구조

```
InventoryWindow (280dp, 드래그 이동 가능)
 ├── WindowTitleBar "인벤토리"
 ├── 소지금 헤더 (항상 표시) — "소지금 N원"
 ├── HorizontalDivider
 └── 4×8 슬롯 그리드 (스크롤 가능, heightIn(max=300.dp))
      ├── null → EmptyInventorySlot (어두운 빈 슬롯)
      ├── InventorySlot.ScrollItem → DraggableScrollItem (수량 표시, 꾹 눌러 드래그)
      └── InventorySlot.EquipItem → EquipmentBagItem (탭 → ItemInfoDialog)
```

### UiState 필드

```kotlin
val money: Int = 0,
val inventorySlots: List<InventorySlot?> = List(32) { null },  // 4열 × 8행
```

### InventorySlot sealed class

```kotlin
sealed class InventorySlot {
    data class ScrollItem(val type: ScrollType, val quantity: Int) : InventorySlot()
    data class EquipItem(val equipment: Equipment) : InventorySlot()
}
```

- 같은 종류 스크롤은 동일 슬롯에 스택 (`addScrollToSlots`)
- 장비는 개별 슬롯 점유 (`addEquipToSlots`)

### 장비 해제 → 인벤토리 이동

```kotlin
fun unequipEquipment() {
    _uiState.update { s ->
        val newSlots = addEquipToSlots(s.inventorySlots, s.equipment)
        computeDerived(s.copy(equipment = null, inventorySlots = newSlots, ...))
    }
}
```

- 장갑 슬롯 꾹 누르기 → "장비 해제" 확인 다이얼로그 → "해제" 탭 → `unequipEquipment()` 호출
- 해제된 장갑이 `inventorySlots`의 빈 슬롯에 `EquipItem`으로 들어감

### 인벤토리 → 장착

```kotlin
fun equipFromInventory(equipment: Equipment) {
    _uiState.update { s ->
        val slotIdx = s.inventorySlots.indexOfFirst { it is InventorySlot.EquipItem && it.equipment == equipment }
        val newSlots = s.inventorySlots.toMutableList()
        newSlots[slotIdx] = if (s.equipment != null) InventorySlot.EquipItem(s.equipment) else null
        computeDerived(s.copy(equipment = equipment, inventorySlots = newSlots, ...))
    }
}
```

- 인벤토리 장갑 탭 → `ItemInfoDialog` 표시 (스크롤 내리면 "장착" 버튼 노출)
- "장착" 탭 → `equipFromInventory()` 호출 → 선택 장갑이 equipment 슬롯으로, 기존 장착 장갑은 그 슬롯으로 교환

### ItemInfoDialog "장착" 버튼

```kotlin
private fun ItemInfoDialog(equipment: Equipment, onDismiss: () -> Unit, onEquip: (() -> Unit)? = null) {
    // ...
    // ⑥ 장착 버튼 — onEquip != null (인벤토리에서 열 때) && 파괴 안됨
    if (onEquip != null && !isDestroyed) {
        Button(onClick = { onEquip(); onDismiss() }) { Text("장착") }
    }
}
```

- 장비창의 `GlovesSlot`에서 여는 다이얼로그: `onEquip = null` (버튼 없음)
- 인벤의 `EquipmentBagItem`에서 여는 다이얼로그: `onEquip = { onEquip(equipment) }` (버튼 표시)

### 주문서 슬롯 — DraggableScrollItem

| ScrollType | 이미지 | 폴백 |
|------------|--------|------|
| GLOVE_ATK_100 | scroll_100.png | — |
| GLOVE_ATK_60 | scroll_60.png | — |
| GLOVE_ATK_10 | scroll_10.png | — |
| WHITE_SCROLL_1/3 | 없음 | "백의" 텍스트 |

- 수량 0 포함 모든 슬롯을 탭하면 `ScrollInfoDialog` 표시
- 꾹 누르면 드래그 시작 (기존 동작 유지)

### ScrollInfoDialog

- 이름 / 수량 / 성공률 / 효과 (공격력 보너스 or "업그레이드 실패 횟수 복구")
- 이미지가 있으면 PNG, 없으면 텍스트 폴백

### scrollDrawableRes 헬퍼

```kotlin
private fun scrollDrawableRes(scrollType: ScrollType): Int? = when (scrollType) {
    ScrollType.GLOVE_ATK_100 -> R.drawable.scroll_100
    ScrollType.GLOVE_ATK_60  -> R.drawable.scroll_60
    ScrollType.GLOVE_ATK_10  -> R.drawable.scroll_10
    else                     -> null
}
```

---

## 주문서 강화 시스템

| 주문서 | 성공률 | 성공 시 |
|--------|--------|---------|
| 100% | 100% | 공격력 +1 |
| 60%  | 60%  | 공격력 +2 |
| 10%  | 10%  | 공격력 +3 |
| 백의 1%/3% | 1%/3% | 실패 횟수 -1 / 실패 시 파괴 |

---

## 작업 내역

| # | 작업 | 상태 |
|---|------|------|
| 1 | 강화 시스템 로직 구현 | ✅ |
| 2 | 장비창 UI | ✅ |
| 3 | 인벤토리창 UI | ✅ |
| 4 | 드래그 앤 드롭 강화 | ✅ |
| 5 | 아이템 이미지 + 정보창 (맵스토리 스타일) | ✅ |
| 6 | Player / PlayerJob / PlayerStats 모델 | ✅ |
| 7 | CombatStatCalculator | ✅ |
| 8 | Weapon + DefaultWeapon | ✅ |
| 9 | GameWorld 모델 (1024×572 가로형 + MapType) | ✅ |
| 10 | Monster + MonsterSpawner (isBlocked 지원) | ✅ |
| 11 | 가상 조이스틱 + 이동 | ✅ |
| 12 | 게임 화면 구조 개편 (오버레이) | ✅ |
| 13 | CameraState | ✅ |
| 14 | AutoAttackService | ✅ |
| 15 | LevelService | ✅ |
| 16 | DropService + DropTable | ✅ |
| 17 | StatType + StatWindow + allocateStat | ✅ |
| 18 | Canvas 렌더링 (플레이어 / 몬스터 / 배경 / 공격 범위) | ✅ |
| 19 | GroundItem — 바닥 드랍 아이템 스폰 + Canvas 렌더링 | ✅ |
| 20 | 자동 픽업 시스템 + PICKUP_DELAY 1500ms | ✅ |
| 21 | 아이템 PNG 이미지 적용 | ✅ |
| 22 | 픽셀 루미넌스 기반 충돌 시스템 (LUMINANCE_THRESHOLD=80) | ✅ |
| 23 | AndroidViewModel 전환 — Application 컨텍스트로 충돌 비트맵 로드 | ✅ |
| 24 | 벽 슬라이딩 이동 (X/Y 축 독립 충돌 판정) | ✅ |
| 25 | 슬라임 PNG 이미지 적용 | ✅ |
| 26 | MonsterAiService — 추적 / 공격 / 어그로 해제 | ✅ |
| 27 | 몬스터 리스폰 시스템 (5초 뒤 재스폰) | ✅ |
| 28 | 데미지 숫자 표시 — 노랑(플→몬) / 빨강(몬→플) + fade-out | ✅ |
| 29 | 가로 화면 + 전체화면 몰입 모드 (sensorLandscape) | ✅ |
| 30 | 동적 줌 계산 — maxOf(screenW/worldW, screenH/worldH) | ✅ |
| 31 | 스프라이트 크기 화면 비례 방식 전환 (size.height × 비율) | ✅ |
| 32 | 전사 공격 범위 조정 — ATTACK_RANGE 120f → 60f | ✅ |
| 33 | 마을 맵 추가 (map_town.jpg 1024×572) | ✅ |
| 34 | Portal 모델 + PortalRegistry 구현 | ✅ |
| 35 | 포탈 시스템 — 자동 진입 + 맵 전환 + 2초 쿨다운 | ✅ |
| 36 | 포탈 렌더링 — 다층 파란 글로우 + 맵 이름 레이블 | ✅ |
| 37 | 마을 충돌 비트맵 분리 (townCollisionBitmap) | ✅ |
| 38 | 마을 포탈 위치 픽셀 분석 후 안전 지점 확정 (x=250, lum=146) | ✅ |
| 39 | 장비창/스탯창/인벤토리창 — 타이틀바 드래그로 창 이동 기능 추가 | ✅ |
| 40 | SlotSize 48.dp → 40.dp 축소 (창 크기 최적화) | ✅ |
| 41 | DerivedStats 모델 + DerivedStatsCalculator 구현 | ✅ |
| 42 | Equipment 스탯 보정 필드 추가 (strBonus/dexBonus/physicalDefense 등) | ✅ |
| 43 | 명중 판정 — accuracy/(accuracy+몬스터회피율) 확률 MISS | ✅ |
| 44 | 회피 판정 — avoidability/(avoidability+몬스터명중) 확률 DODGE | ✅ |
| 45 | MISS 하늘색 floating 텍스트 렌더링 | ✅ |
| 46 | 스탯창 전투 능력치 섹션 추가 (공격력/마력/명중률/회피율/방어력 등) | ✅ |
| 47 | UiState derivedStats 추가, 스탯·장비 변경 시 자동 재계산 | ✅ |
| 48 | AppScreen sealed class + 화면 라우팅 (Title / JobSelect / Game) | ✅ |
| 49 | TitleScreen — 타이틀 화면 (게임 시작 버튼) | ✅ |
| 50 | JobSelectScreen — 직업 선택 화면 (2×3 카드 그리드 + 상세 패널) | ✅ |
| 51 | 직업 extension 함수 (themeColor/accentColor/tagline/jobDescription 등) | ✅ |
| 52 | 레벨 3 전직 트리거 — autoAttackTick에서 레벨 크로싱 감지 | ✅ |
| 53 | JobAdvancementDialog — 인게임 전직 팝업 오버레이 | ✅ |
| 54 | advanceJob() — 직업 변경 + 투자 포인트 반환 + DerivedStats 재계산 | ✅ |
| 55 | 전직 시 스탯 교체 버그 수정 (BEGINNER 스탯 유지되던 문제) | ✅ |
| 56 | 타이틀 → 직업 선택 → 게임 진입 흐름 변경 (startGame(job) 파라미터 추가) | ✅ |
| 57 | AttackType / ProjectileType enum 추가 | ✅ |
| 58 | Projectile 모델 추가 (traveledDistance / maxTravelDistance) | ✅ |
| 59 | PlayerJob extension 함수 추가 (attackType / attackRange / projectileType / projectileSpeed) | ✅ |
| 60 | AutoAttackService 근접/원거리 분기 — 원거리는 Projectile 반환 | ✅ |
| 61 | ProjectileService — 16ms 틱 투사체 이동 + 충돌 판정 (반경 16f) | ✅ |
| 62 | MainViewModel projectileTick 루프 추가 (exp/드랍/리스폰 처리 포함) | ✅ |
| 63 | drawProjectile Canvas 렌더링 추가 (직업별 임시 도형) | ✅ |
| 64 | drawAttackRange 직업별 공격 범위 반영 | ✅ |
| 65 | 게임 시작 흐름 복구 — 타이틀 → 초보자 진입 → 레벨 3 전직 팝업 | ✅ |
| 66 | 강화 결과 메시지 2초 후 자동 소멸 (LaunchedEffect + clearLastResult) | ✅ |
| 67 | 에너지볼트 PNG 3프레임 애니메이션 (energy_bolt_1~3, 100ms 순환) | ✅ |
| 68 | 에너지볼트 몬스터 방향 회전 (atan2 + withTransform) + 크기 증가 (0.10f) | ✅ |
| 69 | 에너지볼트 방향 반전 수정 (angleDeg + 180f) | ✅ |
| 70 | PC 방향키/WASD 이동 지원 — dispatchKeyEvent ACTION_DOWN마다 movePlayer 직접 호출 | ✅ |
| 71 | 슬라임 HP 1 (빠른 테스트용) | ✅ |
| 72 | 투사체 중복 타겟 방지 — 비행 중 투사체의 타겟 몬스터는 다음 공격에서 제외 (lockedMonsterIds) | ✅ |
| 73 | GameHud EXP 바 추가 — HP 바 아래 노란색 경험치 바 (현재/필요량, level*20) | ✅ |
| 74 | NPC 모델 추가 — Npc 데이터 클래스 + NpcRegistry (TOWN: 츄츄 450,260) | ✅ |
| 75 | QuestState 모델 추가 — QuestStatus enum (NOT_STARTED/IN_PROGRESS/READY_TO_COMPLETE/COMPLETED) | ✅ |
| 76 | DialogueState 모델 추가 — DialoguePage + DialogueSession | ✅ |
| 77 | UiState NPC/퀘스트/대화 필드 추가 (npcs, questState, activeDialogue) | ✅ |
| 78 | teleportState에서 NpcRegistry 연동 — 맵 전환 시 NPC 목록 갱신 | ✅ |
| 79 | startDialogue / nextDialoguePage / chooseDialogueOption / closeDialogue VM 함수 추가 | ✅ |
| 80 | 퀘스트 킬 카운팅 — autoAttackTick + projectileTick 양쪽에서 슬라임 처치 수 누적 | ✅ |
| 81 | 보상 지급 — EXP +50 + GLOVE_ATK_100 주문서 +1 (chooseDialogueOption READY_TO_COMPLETE) | ✅ |
| 82 | NPC Canvas 렌더링 — PNG 이미지(높이 0.20f × 너비 1.6배) + 이름 텍스트 (drawNpc) | ✅ |
| 83 | [대화하기] 버튼 — NPC interactRange(14f) 이내 접근 시 하단 중앙에 표시 | ✅ |
| 84 | DialogueWindow 컴포저블 — 하단 대화창 (화자명 + 텍스트 + 다음/닫기/선택지 버튼, zIndex 20) | ✅ |
| 85 | DialogueWindow 터치 차단 — pointerInput detectTapGestures 로 대화 중 배경 터치 방지 | ✅ |
| 86 | QuestTrackerPanel 컴포저블 — 좌상단 퀘스트 진행 상황 패널 (프로그레스 바 포함) | ✅ |
| 87 | npc_chuchu.png 리소스 추가 | ✅ |
| 88 | 슬라임 → 스켈레톤 워리어 교체 — Idle(6)/Walk(8)/Slash(6) 프레임 PNG + MonsterState별 애니메이션 | ✅ |
| 89 | Monster.facingLeft 필드 추가 — MonsterAiService 이동 시 방향 갱신 + drawMonster scale(-1) 반전 | ✅ |
| 90 | Monster.variant 필드 추가 — 3종 Crusader 색상 맵별 구분 | ✅ |
| 91 | skeletonWarrior() 팩토리 스탯 파라미터화 (hp/expReward/avoidability/accuracy/speed) | ✅ |
| 92 | MonsterSpawner: spawnSlimes → spawnMonsters (variant + 스탯 파라미터 전달) | ✅ |
| 93 | QuestState: slimeKillCount → killCount / slimeKillGoal → killGoal 필드명 일반화 | ✅ |
| 94 | 퀘스트 대화문 슬라임 → 스켈레톤 워리어로 교체 (MainViewModel buildDialoguePages) | ✅ |
| 95 | FIELD_2 / FIELD_3 맵 추가 (MapType enum + map_beginner 재사용) | ✅ |
| 96 | 포탈 체인 구성 — FIELD_3 ←→ FIELD_2 ←→ BEGINNER_FIELD ←→ TOWN | ✅ |
| 97 | skeletonConfig(mapType) + spawnSkeletons() 헬퍼 — 맵별 스탯·variant 집중화 | ✅ |
| 98 | 맵별 스켈레톤 배치 조정 — 초보자 Crusader_2(HP20) / 중급 Crusader_3(HP60) / 상급 Crusader_1(HP150) | ✅ |
| 99 | Player.facingLeft 필드 추가 — 이동 방향에 따라 좌우 반전 여부 저장 | ✅ |
| 100 | UiState 애니메이션 필드 추가 (playerAttackAnimStart / playerHurtAnimStart / playerDeathTime / pendingPlayerAttack) | ✅ |
| 101 | 전사 스프라이트 프레임 수동 커팅 — image/전사 폴더 원본 → drawable 복사 (IDLE 4 / WALK 4 / ATTACK 5 / HURT 3 / DIE 6) | ✅ |
| 102 | drawPlayer 교체 — 주황 원 → 전사 스프라이트 애니메이션 (IDLE/WALK/ATTACK/HURT/DIE 상태 분기) | ✅ |
| 103 | facingLeft 기반 스프라이트 수평 반전 — withTransform { scale(-1f, 1f, pivot=c) } | ✅ |
| 104 | 전사 캐릭터 크기 조정 — 0.18f → 0.15f → 0.11f (스켈레톤 대비 비율 맞춤) | ✅ |
| 105 | ATTACK 애니메이션 오동작 수정 — 타겟 없을 때 playerAttackAnimStart 갱신하던 버그 제거 | ✅ |
| 106 | 히트 프레임 시스템 구현 — PendingPlayerAttack 데이터 클래스 + pendingAttackTick 함수 | ✅ |
| 107 | ATTACK_ANIM_DURATION = 300ms 상수 추가 — 5프레임 × 60ms 기준 | ✅ |
| 108 | 공격 애니메이션 속도 조정 — 100ms/frame → 60ms/frame (빠른 타격감) | ✅ |
| 109 | GameCanvas 파라미터 추가 — isMoving / playerAttackAnimStart / playerHurtAnimStart / playerDeathTime | ✅ |
| 110 | UiState.equipmentBag 추가 — 드랍 장갑을 인벤토리 가방에 저장 (기존 무시 버그 수정) | ✅ |
| 111 | DraggableScrollItem 이미지화 — 텍스트 → PNG 이미지 (100%/60%/10%), 수량만 하단 표시 | ✅ |
| 112 | ScrollInfoDialog 추가 — 주문서 슬롯 탭 시 이름·수량·성공률·효과 표시 (수량 0도 가능) | ✅ |
| 113 | EquipmentBagItem 추가 — 인벤토리 상단 장갑 이미지 슬롯, 탭 시 ItemInfoDialog | ✅ |
| 114 | InventoryWindow 장비 아이템 섹션 추가 — equipmentBag 비어있지 않을 때 상단에 렌더링 | ✅ |
| 115 | DragGhost 이미지화 — 드래그 중 고스트도 PNG 이미지로 표시 (백의는 텍스트 폴백) | ✅ |
| 116 | unequipEquipment() 수정 — 해제된 장갑이 equipmentBag으로 이동 (기존: 버려짐) | ✅ |
| 117 | equipFromBag() 추가 — 인벤토리 장갑을 장착 슬롯으로, 기존 장착 장갑은 bag으로 교환 | ✅ |
| 118 | ItemInfoDialog onEquip 파라미터 추가 — 인벤토리에서 열 때만 "장착" 버튼 표시 (스크롤 하단) | ✅ |
| 119 | 인벤토리 시스템 개편 — equipmentBag → InventorySlot sealed class (ScrollItem/EquipItem) 통합 4×8 그리드 | ✅ |
| 120 | InventoryWindow 높이 클리핑 수정 — heightIn(max=300.dp) + verticalScroll, 소지금 헤더 스크롤 영역 밖으로 분리 | ✅ |
| 121 | 돈 드랍 시스템 구현 — MoneyDrop ground item 방식 (노란 원형 동전 Canvas 렌더링, PICKUP_RANGE=150f, PICKUP_DELAY=2000ms) | ✅ |
| 122 | 장비 인벤토리/장비창 표시 수정 — 장갑 이미지 제거, 이름 축약+공격력 텍스트로 교체 (EquipmentBagItem, GlovesSlot) | ✅ |
| 123 | GameMessage 모델 추가 — MessageType enum (EXP/MONEY/ITEM) + GameMessage 데이터 클래스 | ✅ |
| 124 | 메시지 로그 오버레이 추가 — 우측 하단 MessageLogOverlay, 최대 5개, 2초 자동 소멸 (coroutine delay) | ✅ |
| 125 | 몬스터 처치/드랍 시 메시지 발생 — +N EXP (금색) / +N원 (초록) / 아이템명 획득 (파랑) | ✅ |
| 126 | 동전 애니메이션 교체 — 노란 원 → coin_0~3.png 4프레임 150ms 순환 (드랍 시점 기준 개별 타이머) | ✅ |
| 127 | 궁수 스프라이트 시스템 추가 — archer_sheet.png (4900×109, 프레임 140×109) + sliceSheet() 헬퍼 구현 | ✅ |
| 128 | 직업별 플레이어 스프라이트 분기 — player.job == ARCHER 일 때 궁수 프레임 선택, 나머지는 전사 프레임 | ✅ |
| 129 | 게임 데이터 저장 시스템 구현 — GameSaveData/SaveService (Gson+SharedPreferences), 10초 자동 저장 + 종료 시 저장 + 시작 시 로드 | ✅ |

---

## PC 키보드 입력 (에뮬레이터 테스트용)

`MainActivity.dispatchKeyEvent` 오버라이드로 처리. ACTION_DOWN마다 `moveVm.movePlayer()` 즉시 호출 (키 리피트 포함).

```kotlin
private val moveVm: MainViewModel by lazy {
    ViewModelProvider(this)[MainViewModel::class.java]
}

override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.action != ACTION_DOWN) return super.dispatchKeyEvent(event)
    val dx = when (keyCode) { DPAD_LEFT/A -> -1f; DPAD_RIGHT/D -> 1f; else -> 0f }
    val dy = when (keyCode) { DPAD_UP/W   -> -1f; DPAD_DOWN/S  -> 1f; else -> 0f }
    if (dx != 0f || dy != 0f) { moveVm.movePlayer(dx, dy); return true }
    return super.dispatchKeyEvent(event)
}
```

- 지원 키: ←↑↓→ (DPAD) + W/A/S/D
- 키 홀드 시 Android 키 리피트(~20fps)로 연속 호출

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 드래그 창 시스템 (MainActivity)

### 플로팅 창 공통 구조

```kotlin
// MainScreen 내부
val density       = LocalDensity.current
val configuration = LocalConfiguration.current
val screenW  = with(density) { configuration.screenWidthDp.dp.toPx() }
val screenH  = with(density) { configuration.screenHeightDp.dp.toPx() }
val panelWPx = with(density) { 280.dp.toPx() }
val headerPx = with(density) { 36.dp.toPx() }
val initPad  = with(density) { 8.dp.toPx() }

var equipOffset  by remember { mutableStateOf(Offset(initPad, initPad)) }
var statOffset   by remember { mutableStateOf(Offset(initPad, initPad)) }
var inventOffset by remember { mutableStateOf(Offset(initPad, initPad)) }

fun clampWin(o: Offset) = Offset(
    o.x.coerceIn(0f, (screenW - panelWPx).coerceAtLeast(0f)),
    o.y.coerceIn(0f, (screenH - headerPx).coerceAtLeast(0f))
)
```

### WindowTitleBar 드래그 핸들러

```kotlin
@Composable
private fun WindowTitleBar(
    title: String,
    onClose: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelHeader)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .then(
                if (onDrag != null) Modifier.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) { ... }
}
```

- `EquipmentWindow`, `StatWindow`, `InventoryWindow` 모두 `onDrag: ((Offset) -> Unit)? = null` 파라미터 추가
- 각 창은 `zIndex(10f)` + `offset { IntOffset(...) }` + `width(280.dp)` 적용

---

## 메시지 로그 시스템

### 모델 (model/GameMessage.kt)

```kotlin
enum class MessageType { EXP, MONEY, ITEM }

data class GameMessage(val id: Long, val text: String, val type: MessageType)
```

### UiState 추가 필드

```kotlin
val messages: List<GameMessage> = emptyList()
```

### addMessage 헬퍼 (MainViewModel)

```kotlin
private var nextMessageId = 0L

private fun addMessage(state: UiState, text: String, type: MessageType): UiState {
    val newMsg = GameMessage(id = nextMessageId++, text = text, type = type)
    viewModelScope.launch {
        delay(2000L)
        _uiState.update { s -> s.copy(messages = s.messages.filter { it.id != newMsg.id }) }
    }
    return state.copy(messages = (state.messages + newMsg).takeLast(5))
}
```

### 발생 시점

| 이벤트 | 메시지 | 색상 |
|--------|--------|------|
| 몬스터 처치 (EXP 획득) | `+N EXP` | 금색 `0xFFFFD700` |
| 돈 줍기 | `+N원` | 초록 `0xFF66BB6A` |
| 스크롤 줍기 | `아이템명 획득` | 파랑 `0xFF64B5F6` |
| 장비 줍기 | `아이템명 획득` | 파랑 `0xFF64B5F6` |

### MessageLogOverlay 컴포저블

- 위치: `Alignment.BottomEnd`, `padding(end=12.dp, bottom=80.dp)`
- 각 메시지: 검정 반투명 배경 + RoundedCorner(4.dp) + 13sp Bold 텍스트
- `messages.isNotEmpty()` 일 때만 렌더링

---

## 궁수 스프라이트 시스템

### 스프라이트시트 구조 (archer_sheet.png)

- 원본: `image/궁수/궁수-Sheet.png`
- 전체 크기: 4900×109px, 프레임 크기: **140×109px**

| 애니메이션 | 시작 X | 프레임 수 |
|-----------|--------|----------|
| Idle      | 0      | 5        |
| Walk      | 980    | 4        |
| Attack    | 1960   | 6        |
| Hurt      | 2940   | 4        |
| Die       | 4060   | 5        |

### sliceSheet() 헬퍼 (MainActivity)

```kotlin
private fun sliceSheet(
    context: android.content.Context,
    resId: Int,
    frameW: Int,
    startX: Int,
    count: Int
): List<ImageBitmap> {
    val full = BitmapFactory.decodeResource(
        context.resources, resId,
        BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    )
    return (0 until count).map { i ->
        Bitmap.createBitmap(full, startX + i * frameW, 0, frameW, full.height).asImageBitmap()
    }
}
```

### 직업별 프레임 선택 (GameCanvas Canvas 블록)

```kotlin
val isArcher = player.job == PlayerJob.ARCHER
val pIdle   = if (isArcher) archerIdle   else warriorIdle
val pWalk   = if (isArcher) archerWalk   else warriorWalk
val pAttack = if (isArcher) archerAttack else warriorAttack
val pHurt   = if (isArcher) archerHurt   else warriorHurt
val pDie    = if (isArcher) archerDie    else warriorDie
drawPlayer(player, cam, pIdle, pWalk, pAttack, pHurt, pDie, ...)
```

- `drawPlayer` 함수 시그니처 변경 없음 — 프레임 리스트를 선택해서 전달
- 애니메이션 타이밍은 전사와 동일 (IDLE 200ms/f, WALK 100ms/f, ATTACK 60ms/f, HURT 100ms/f, DIE 200ms/f)

---

## 저장 시스템

### 저장 내용

| 항목 | 설명 |
|------|------|
| 플레이어 | 레벨, EXP, HP, MaxHP, 직업, STR/DEX/INT/LUK, 스탯포인트, 위치(X/Y) |
| 소지금 | money: Int |
| 현재 맵 | world.mapType |
| 퀘스트 | status, killCount, killGoal |
| 장착 장비 | equipment: Equipment? |
| 무기 | weapon: Weapon? |
| 인벤토리 | inventorySlots (32개, ScrollItem/EquipItem/ConsumableItem) |

### 저장되지 않는 것 (세션 임시)

몬스터 위치, 바닥 드랍 아이템, 대화 상태, 투사체, 데미지 숫자 → 앱 재시작 시 초기화

### 파일 구조

**`service/GameSaveData.kt`**
```kotlin
data class GameSaveData(
    val playerLevel: Int = 1, val playerExp: Int = 0,
    val playerHp: Int = 100,  val playerMaxHp: Int = 100,
    val playerJob: String = "BEGINNER",
    val playerStr: Int, val playerDex: Int, val playerInt: Int, val playerLuk: Int,
    val playerStatPoints: Int, val playerPosX: Float, val playerPosY: Float,
    val money: Int, val mapType: String, val questStatus: String,
    val questKillCount: Int, val questKillGoal: Int,
    val equipment: Equipment?, val weapon: Weapon?,
    val inventory: List<SavedSlot>
)

data class SavedSlot(
    val slotType: String,           // "EMPTY" | "SCROLL" | "EQUIP" | "CONSUMABLE"
    val scrollType: String? = null,
    val scrollQty: Int = 0,
    val equipment: Equipment? = null,
    val consumableType: String? = null,
    val consumableQty: Int = 0
)
```

**`service/SaveService.kt`** — Gson + SharedPreferences("game_save")
- `save(state: UiState)` — UiState → GameSaveData → JSON 직렬화
- `load(): GameSaveData?` — JSON 역직렬화, 실패 시 null 반환
- `hasSave()` / `deleteSave()`

### 저장 시점 (MainViewModel)

| 시점 | 방법 |
|------|------|
| 앱 실행 시 | `init` 블록에서 로드, `_uiState.value` 복원 |
| 10초마다 | `viewModelScope.launch { while(true) { delay(10_000L); save() } }` |
| 앱 종료 시 | `onCleared()` 오버라이드에서 즉시 저장 |
| 새 게임 시작 | `startGame()` 에서 새 상태 즉시 저장 (기존 세이브 덮어씀) |

### 복원 로직 (restoreState)

- enum은 `runCatching { Enum.valueOf(str) }.getOrDefault(기본값)` — 알 수 없는 값 안전 처리
- HP는 `.coerceAtLeast(1)` — 0HP로 로드되는 버그 방지
- 인벤토리 슬롯이 32개 미만이면 null로 패딩하여 항상 32개 유지
- 맵에 맞는 몬스터/포탈/NPC 재스폰 (spawnSkeletons + PortalRegistry + NpcRegistry)
- `computeDerived(base)` — 로드 후 파생 스탯 재계산

### 의존성

```kotlin
// app/build.gradle.kts
implementation("com.google.code.gson:gson:2.10.1")
```

---

## 다음 작업 후보 (우선순위 순)

- [ ] 투사체 PNG 리소스 교체 (에너지볼트/화살/표창/총알 이미지)
- [ ] NPC 퀘스트 2차 — 다양한 퀘스트 / 퀘스트 목록 UI
- [ ] 무기 강화 시스템
- [ ] 장비 창고
- [ ] 애니메이션 (공격 이펙트, 레벨업 연출)
- [ ] 직업별 스킬 시스템
