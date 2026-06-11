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
│   ├── Equipment.kt              (+ itemId/slot/requiredLevel/buyPrice/sellPrice 필드 추가)
│   ├── EquipmentRegistry.kt      (CSV 기반 조회 전용 — EquipmentRegistry.get(itemId))
│   ├── Monster.kt                (+ monsterId: String 필드 추가)
│   ├── MonsterRegistry.kt        (CSV 기반 — MonsterConfig + configForMap(MapType))
│   ├── DropTable.kt              (DropItem sealed class — ScrollDrop/EquipmentDrop/MoneyDrop/MaterialDrop + DropEntry)
│   ├── MaterialType.kt           (MaterialType enum + MaterialCatalog — 재료 아이템 이름·판매가)
│   ├── DropRegistry.kt           (CSV 기반 드랍 조회 — dropEntriesFor(monsterId, mapType))
│   ├── QuestRegistry.kt          (CSV 기반 — QuestData + questForNpc(npcId))
│   ├── Npc.kt                    (NpcRegistry — CSV 기반 로드로 전환)
│   ├── ShopRegistry.kt           (CSV 기반 로드로 전환, EquipmentRegistry 연동)
│   ├── Scroll.kt / EnhancementResult.kt
│   ├── Player.kt                 (+ facingLeft: Boolean)
│   ├── PlayerJob.kt              (+ attackType/attackRange/projectileType/projectileSpeed 확장함수)
│   ├── PlayerStats.kt            (+ StatType enum)
│   ├── Weapon.kt                 (+ DefaultWeapon)
│   ├── GameWorld.kt              (1024×572 + MapType enum)
│   ├── MonsterState.kt           (IDLE / AGGRO / ATTACKING)
│   ├── CameraState.kt            (좌표 변환 / 추적)
│   ├── GroundItem.kt             (바닥 드랍 아이템 + droppedAt 타임스탬프)
│   ├── DamageNumber.kt           (데미지 숫자 floating 표시 + isMiss 필드)
│   ├── DerivedStats.kt           (전투 파생 능력치 모델)
│   ├── Portal.kt                 (Portal 모델 + PortalRegistry)
│   ├── AttackType.kt             (MELEE / PROJECTILE)
│   ├── ProjectileType.kt         (ENERGY_BOLT / ARROW / THROWING_STAR / BULLET)
│   ├── Projectile.kt             (투사체 모델 + traveledDistance / maxTravelDistance)
│   ├── QuestState.kt             (QuestStatus enum + QuestState)
│   ├── DialogueState.kt          (DialoguePage + DialogueSession)
│   ├── GameMessage.kt            (MessageType enum + GameMessage 데이터 클래스)
│   ├── Skill.kt                  (SkillType enum + Skill 데이터 클래스 + SkillRegistry)
│   └── SkillEffect.kt            (SkillEffectType enum + SkillEffect 데이터 클래스)
├── service/
│   ├── DataLoader.kt             (CSV 파싱 유틸 — loadCsv(context, fileName) → List<Map<String,String>>)
│   ├── GameDataInitializer.kt    (앱 시작 시 모든 CSV 로드 — initialize(context))
│   ├── EnhancementService.kt     (+ 슬롯 검증 + strBonus/dexBonus/intBonus/lukBonus 적용)
│   ├── CombatStatCalculator.kt   (레거시, 미사용)
│   ├── DerivedStatsCalculator.kt (직업별 스탯→전투 능력치 계산)
│   ├── MonsterSpawner.kt         (+ monsterId 파라미터 추가)
│   ├── AutoAttackService.kt      (근접/원거리 분기 + 직업별 공격 범위)
│   ├── ProjectileService.kt      (투사체 이동 + 충돌 판정)
│   ├── SkillService.kt           (SkillHit/SkillResult + MELEE_BURST/AOE/MULTI_SHOT 실행 로직)
│   ├── MonsterAiService.kt       (추적 / 공격 / 어그로 해제 + 회피 판정)
│   ├── LevelService.kt
│   ├── DropService.kt
│   ├── SoundManager.kt           (BGM/SFX 관리 — SoundPool + MediaPlayer 이중 경로, 캐시 복사 방식)
│   ├── GameSaveData.kt           (GameSaveData + SavedSlot 직렬화 데이터 클래스)
│   └── SaveService.kt            (Gson 직렬화 + SharedPreferences 저장/로드)
├── viewmodel/
│   └── MainViewModel.kt          (GameDataInitializer 호출 + MonsterRegistry/DropRegistry/QuestRegistry 연동)
├── assets/data/                  ← 게임 데이터 CSV (엑셀 편집 → CSV 저장 → 자동 반영)
│   ├── equipment.csv             (itemId,name,slot,requiredLevel,attackPower,...,buyPrice,sellPrice,description)
│   ├── monster.csv               (monsterId,name,mapId,variant,level,hp,exp,...,count,moneyMin,moneyMax)
│   ├── drop.csv                  (monsterId,itemId,dropRate)
│   ├── npc.csv                   (npcId,name,role,mapId,x,y)
│   ├── shop.csv                  (shopType,itemId,buyPrice,sellPrice)
│   └── quest.csv                 (questId,name,npcId,targetMonsterId,targetCount,rewardExp,rewardMoney,rewardItemId)
├── assets/sounds/                ← 오디오 파일 배치 폴더 (없어도 정상 실행)
│   ├── README.txt                (파일 네이밍 가이드)
│   ├── bgm_town.*                (마을 BGM)
│   ├── bgm_battle.*              (전투 BGM)
│   ├── sfx_attack.*              (플레이어 공격)
│   ├── sfx_monster_hit.*         (몬스터 피격)
│   ├── sfx_monster_die.*         (몬스터 사망)
│   ├── sfx_player_hit.*          (플레이어 피격)
│   ├── sfx_level_up.*            (레벨업)
│   ├── sfx_item_pickup.*         (아이템 획득)
│   ├── sfx_scroll_success.*      (주문서 성공)
│   ├── sfx_scroll_fail.*         (주문서 실패)
│   └── sfx_portal.*              (포탈 이동)
└── res/drawable/
    ├── map_beginner.jpg           ← 초보자 사냥터 맵 (1024×572)
    ├── map_town.jpg               ← 마을 맵 (1024×572)
    ├── energy_bolt_1.png ~ energy_bolt_3.png  ← 에너지볼트 3프레임
    ├── skeleton_idle_0~5.png      ← 스켈레톤 Crusader_1 Idle (6프레임)
    ├── skeleton_walk_0~7.png      ← 스켈레톤 Crusader_1 Walk (8프레임)
    ├── skeleton_slash_0~5.png     ← 스켈레톤 Crusader_1 Slash (6프레임)
    ├── skeleton2_idle/walk/slash  ← Crusader_2 (각 6/8/6프레임)
    ├── skeleton3_idle/walk/slash  ← Crusader_3 (각 6/8/6프레임)
    ├── minotaur1_idle/walk/slash  ← Minotaur_1 variant 4 (각 6/8/6프레임, Idle 3f간격/Walk 3f간격/Slash 2f간격 서브샘플)
    ├── minotaur2_idle/walk/slash  ← Minotaur_2 variant 5 (각 6/8/6프레임)
    ├── minotaur3_idle/walk/slash  ← Minotaur_3 variant 6 (각 6/8/6프레임)
    ├── skeleton_bone.png          ← 재료 아이템 — 스켈레톤뼈
    ├── beef.png                   ← 재료 아이템 — 소고기
    ├── nogada_glove.png
    ├── nogada_sword.png
    ├── item_hat_test.png          ← 장비 아이템 이미지 — 갈샛 삿갓 (256×256 정규화)
    ├── item_top_test.png          ← 장비 아이템 이미지 — 지장의 (256×256 정규화)
    ├── item_shoes_test.png        ← 장비 아이템 이미지 — 흰색 고무신 (256×256 정규화)
    ├── item_pants_test.png        ← 장비 아이템 이미지 — 백진일갑주 바지 (256×256 정규화)
    ├── item_bow_test.png          ← 장비 아이템 이미지 — 사냥꾼의 활 (256×256 정규화)
    ├── scroll_100.png / scroll_60.png / scroll_10.png
    ├── npc_chuchu.png             ← 마을 NPC 츄츄 이미지
    ├── warrior_idle_0~3.png       ← 전사 Idle 4프레임 (200ms/frame)
    ├── warrior_walk_0~3.png       ← 전사 Walk 4프레임 (100ms/frame)
    ├── warrior_attack_0~4.png     ← 전사 Attack 5프레임 (60ms/frame)
    ├── warrior_hurt_0~2.png       ← 전사 Hurt 3프레임 (100ms/frame)
    ├── warrior_die_0~5.png        ← 전사 Die 6프레임 (200ms/frame)
    ├── coin_0~3.png               ← 동전 애니메이션 4프레임 (150ms 순환)
    ├── archer_idle_0~4.png        ← 궁수 Idle 5프레임 (203×203, 투명 배경)
    ├── archer_walk_0~3.png        ← 궁수 Walk 4프레임 (203×203)
    ├── archer_attack_0~5.png      ← 궁수 Attack 6프레임 (203×203)
    ├── archer_hurt_0~3.png        ← 궁수 Hurt 4프레임 (203×203)
    ├── archer_die_0~4.png         ← 궁수 Die 5프레임 (203×203)
    ├── map_cemetery.png           ← 스켈레톤 사냥터(FIELD_2/FIELD_3) 배경 비트맵 (1024×572, 묘지 테마)
    └── map_forest.jpg             ← 초보자 사냥터(BEGINNER_FIELD) 배경 비트맵 (1024×572, 숲 테마)
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
 ├── Row (우하단) ← SkillButton + AttackButton 가로 배치 (spacing 10dp, padding end=16dp, bottom=20dp)
 │    ├── SkillButton ← 직업별 스킬 1개, 72dp 원형, 쿨다운 파이 오버레이
 │    └── AttackButton ← 기본 공격 버튼, 72dp 원형, 자동/수동 모드 표시
 ├── Box (우상단, zIndex=30) ← 메뉴 버튼(≡, 44dp) + 메뉴 패널 (isMenuOpen 시)
 │    └── GameMenuPanel ← 음향 설정(BGM/SFX ON/OFF) + 스탯/장비/인벤 버튼
 ├── JoystickControl (좌하단)
 ├── DragGhost
 ├── JobAdvancementDialog  ← jobAdvancementPending=true 일 때만 표시 (전직 팝업 오버레이)
 ├── MessageLogOverlay (우하단, bottom=110.dp) ← EXP/돈/아이템 획득 메시지 (최대 5개, 2초 자동 소멸)
 ├── PotionQuickSlotRow (하단 중앙) ← 물약 퀵슬롯 3개 행 (padding bottom=20.dp)
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
| 1 | `drawWorldBackground` — TOWN: map_town.jpg / BEGINNER_FIELD: map_forest.jpg / FIELD_2·FIELD_3: map_cemetery.png / 그 외: map_beginner.jpg |
| 2 | `drawGroundItem` × N — 바닥 드랍 아이템 (이미지만, 글로우·이름 없음) |
| 3 | `drawPortal` × N — 포탈 (다층 파란 글로우 + 맵 이름 레이블) |
| 4 | `drawNpc` × N — NPC 이미지 (size.height×0.20f 높이, 너비=높이×1.6) + 이름 텍스트 |
| 5 | `drawAttackRange` — 공격 범위 원 (반투명 흰색, 직업별 반경) |
| 6 | `drawMonster` × N — 스켈레톤 애니메이션(variant별 프레임) → HP 바 → 어그로 "!" |
| 7 | `drawProjectile` × N — 직업별 투사체 (임시 도형) |
| 8 | `drawPlayer` — 전사 스프라이트 (IDLE/WALK/ATTACK/HURT/DIE 상태별 애니메이션, facingLeft 수평 반전) |
| 8.5 | `drawSkillEffects` × N — 스킬 이펙트 (SLASH_BURST 흰 링+선 / EXPLOSION 오렌지 링, 500ms 지속) |
| 9 | `drawDamageNumber` × N — 데미지 숫자 (노랑: 플→몬, 빨강: 몬→플) |

### 시각 사양 (화면 비례 크기)

| 대상 | 표현 방식 | 크기 |
|------|-----------|------|
| 플레이어 (전사) | 스프라이트 애니메이션 (IDLE 4f / WALK 4f / ATTACK 5f / HURT 3f / DIE 6f) | `size.height * 0.11f` (정사각형) |
| 플레이어 (궁수) | 스프라이트 애니메이션 (IDLE 5f / WALK 4f / ATTACK 6f / HURT 4f / DIE 5f) | `size.height * 0.15f` (정사각형, vertRatio=0.95f) |
| 스켈레톤 (IDLE) | Idle 애니메이션 6프레임 + 초록 HP 바 | `size.height * 0.15f` |
| 스켈레톤 (AGGRO) | Walk 애니메이션 8프레임 + 주황 HP 바 + "!" | `size.height * 0.15f` |
| 스켈레톤 (ATTACKING) | Slash 애니메이션 6프레임 + 주황 HP 바 + "!" | `size.height * 0.15f` |
| 미노타우르스 | 동일 drawMonster 함수 사용, monsterId.startsWith("MINOTAUR") 분기로 minoFrames 선택 | `size.height * 0.15f` |
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
enum class MapType { BEGINNER_FIELD, TOWN, FIELD_2, FIELD_3, MINOTAUR_FIELD_1, MINOTAUR_FIELD_2, MINOTAUR_FIELD_3 }
```

### 맵별 설정

| 맵 | 이미지 | 월드 크기 | 몬스터 | variant |
|----|--------|-----------|--------|---------|
| BEGINNER_FIELD | map_forest.jpg | 1024×572 | 스켈레톤 워리어 5마리 (HP 20, EXP 8) | 2 (Crusader_2) |
| TOWN | map_town.jpg | 1024×572 | 없음 | — |
| FIELD_2 | map_cemetery.png | 1024×572 | 스켈레톤 워리어 5마리 (HP 60, EXP 20) | 3 (Crusader_3) |
| FIELD_3 | map_cemetery.png | 1024×572 | 스켈레톤 워리어 5마리 (HP 150, EXP 45) | 1 (Crusader_1) |
| MINOTAUR_FIELD_1 | map_beginner.jpg | 1024×572 | 미노타우르스 5마리 (HP 300, EXP 80) | 4 (Minotaur_1) |
| MINOTAUR_FIELD_2 | map_beginner.jpg | 1024×572 | 미노타우르스 5마리 (HP 500, EXP 130) | 5 (Minotaur_2) |
| MINOTAUR_FIELD_3 | map_beginner.jpg | 1024×572 | 미노타우르스 5마리 (HP 800, EXP 200) | 6 (Minotaur_3) |

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

## 사냥터 배경 시스템

사냥터 배경은 비트맵 이미지를 `drawWorldBackground`로 렌더링하는 방식을 사용한다.
각 맵에 맞는 이미지를 `cemeteryBitmap`/`forestBitmap` 등으로 로드한 뒤, `when (world.mapType)` 분기로 선택.

```kotlin
val currentMapBitmap = when (world.mapType) {
    MapType.TOWN                           -> townBitmap
    MapType.BEGINNER_FIELD                 -> forestBitmap    // map_forest.jpg (1024×572, 숲 테마)
    MapType.FIELD_2, MapType.FIELD_3       -> cemeteryBitmap  // map_cemetery.png (1024×572, 묘지 테마)
    else                                   -> mapBitmap       // map_beginner.jpg (미노타우르스 맵)
}
drawWorldBackground(cam, world, currentMapBitmap)
```

> 충돌 비트맵(`collisionBitmap`)은 여전히 `map_beginner.jpg` 기반. 시각 배경과 충돌은 독립적.

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
| BEGINNER_FIELD | (880, 286) | TOWN | (350, 286) |
| BEGINNER_FIELD | (144, 286) | FIELD_2 | (800, 286) |
| TOWN | (250, 286) | BEGINNER_FIELD | (860, 286) |
| TOWN | (750, 286) | MINOTAUR_FIELD_1 | (300, 286) |
| FIELD_2 | (880, 286) | BEGINNER_FIELD | (160, 286) |
| FIELD_2 | (144, 286) | FIELD_3 | (800, 286) |
| FIELD_3 | (880, 286) | FIELD_2 | (160, 286) |
| MINOTAUR_FIELD_1 | (174, 286) | TOWN | (750, 286) |
| MINOTAUR_FIELD_1 | (850, 286) | MINOTAUR_FIELD_2 | (300, 286) |
| MINOTAUR_FIELD_2 | (174, 286) | MINOTAUR_FIELD_1 | (800, 286) |
| MINOTAUR_FIELD_2 | (850, 286) | MINOTAUR_FIELD_3 | (300, 286) |
| MINOTAUR_FIELD_3 | (174, 286) | MINOTAUR_FIELD_2 | (800, 286) |

포탈 체인:
- 스켈레톤 루트: `FIELD_3 ←→ FIELD_2 ←→ BEGINNER_FIELD ←→ TOWN`
- 미노타우르스 루트: `TOWN ←→ MINOTAUR_FIELD_1 ←→ MINOTAUR_FIELD_2 ←→ MINOTAUR_FIELD_3`
- TOWN은 두 루트의 허브 (왼쪽=초보자 사냥터, 오른쪽=미노타우르스 사냥터)

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
AppScreen.Title
  ├── 세이브 있음 → "이어하기" 클릭 → AppScreen.Game (startGame() 호출 없음 — 로드된 상태 유지)
  │               "새 게임" 클릭  → AppScreen.JobSelect
  └── 세이브 없음 → "게임 시작" 클릭 → AppScreen.JobSelect

AppScreen.JobSelect → 직업 선택 후 "전직하기" → vm.startGame(job) → AppScreen.Game
AppScreen.Game      → 초보자(BEGINNER)로 시작, 레벨 3 도달 시 JobAdvancementDialog 자동 표시
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
| 공격 주기 | `derivedStats.attackIntervalMs` (무기 속도 기준, 장비 보너스 차감) — autoAttackEnabled=true 시에만 자동 발동 |
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
| 갈샛 삿갓 (TEST_HAT) | 5% | 5% | 5% |
| 지장의 (TEST_TOP) | 5% | 5% | 5% |
| 흰색 고무신 (TEST_SHOES) | 5% | 5% | 5% |
| 백진일갑주 바지 (TEST_PANTS) | 5% | 5% | 5% |
| 사냥꾼의 활 (TEST_BOW) | 5% | 5% | 5% |
| 장갑 공격력 100% | 20% | 20% | 20% |
| 장갑 공격력 60% | 10% | 10% | 10% |
| 장갑 공격력 10% | 3% | 3% | 3% |
| 백의 주문서 1% | 1% | 1% | 1% |

- **MoneyDrop**: ground item으로 바닥에 드랍 → `coin_0~3.png` 4프레임 애니메이션(150ms 순환) Canvas 렌더링 → PICKUP_DELAY(2000ms) 경과 후 PICKUP_RANGE(30f) 내 자동 습득 → `UiState.money` 가산
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
    val attackPower: Int = 0,         // 스탯 성장
    val magicPower: Int = 0,          // 스탯 성장
    val accuracy: Int = 0,            // 스탯 성장
    val avoidability: Int = 0,        // 스탯 성장
    val physicalDefense: Int = 0,     // 장비 전용
    val magicDefense: Int = 0,        // 장비 전용
    val criticalRate: Float = 0f,     // 장비 전용
    val moveSpeed: Float = 0f,        // 장비 전용
    val attackSpeed: Float = 0f,      // 장비 보너스 (ms 감소량)
    val attackIntervalMs: Long = 900L // 실제 공격 주기 = 무기 기준값 − attackSpeed 보너스
)
```

### 무기 공격속도 → 기준 주기 (Weapon.attackIntervalMs())

| 공격속도 문자열 | 기준 주기 |
|--------------|---------|
| 매우빠름 | 600ms |
| 빠름     | 750ms |
| 보통     | 900ms |
| 느림     | 1050ms |
| 매우느림  | 1200ms |

`attackIntervalMs = (weapon.attackIntervalMs() - equipment.attackSpeed).coerceAtLeast(300L)`

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
MOVE_SPEED                  = 2f
AUTO_ATTACK_CHECK_INTERVAL  = 100ms  // 공격 루프 체크 빈도 (실제 간격은 derivedStats.attackIntervalMs)
AI_TICK_INTERVAL            = 16ms
RESPAWN_DELAY          = 5000ms
RESPAWN_CHECK_INTERVAL = 1000ms
DAMAGE_NUMBER_DURATION = 800ms
PICKUP_RANGE           = 30f
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
- 단, 공격 애니메이션 재생 중(ATTACK_ANIM_DURATION=300ms)에는 이동 방향에 의한 `facingLeft` 갱신 차단 (타겟 방향 유지)
- `autoAttackTick`에서 공격 시 타겟 몬스터 X < 플레이어 X → `facingLeft = true`, 그 반대 → `facingLeft = false`

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
      ├── InventorySlot.EquipItem → EquipmentBagItem (탭 → ItemInfoDialog)
      ├── InventorySlot.ConsumableItem → ConsumableInventoryItem (탭 → 정보창, 꾹 눌러 퀵슬롯 드래그)
      └── InventorySlot.MaterialItem → MaterialInventoryItem (PNG 이미지 + 수량)
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
    data class ConsumableItem(val type: ConsumableType, val quantity: Int) : InventorySlot()
    data class MaterialItem(val type: MaterialType, val quantity: Int) : InventorySlot()
}
```

- 같은 종류 스크롤은 동일 슬롯에 스택 (`addScrollToSlots`)
- 장비는 개별 슬롯 점유 (`addEquipToSlots`)
- 같은 ConsumableType 소비 아이템은 동일 슬롯에 스택
- 같은 MaterialType 재료 아이템은 동일 슬롯에 스택 (ddMaterialToSlots)

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

### 장갑 공격력 계열

| 주문서 | 성공률 | 성공 시 |
|--------|--------|---------|
| 100% | 100% | 공격력 +1 |
| 60%  | 60%  | 공격력 +2 |
| 10%  | 10%  | 공격력 +3 |
| 백의 1%/3% | 1%/3% | 실패 횟수 -1 / 실패 시 파괴 |

### 장비 슬롯별 스탯 주문서 (GLOVE / TOP / HAT / SHOES)

각 슬롯마다 STR·DEX·INT·LUK × 10%/60%/100% = 슬롯4 × 스탯4 × 등급3 = **48종**

| 등급 | 성공률 | 성공 시 보너스 |
|------|--------|--------------|
| 100% | 100% | 해당 스탯 +1 |
| 60%  | 60%  | 해당 스탯 +2 |
| 10%  | 10%  | 해당 스탯 +3 |

- `targetSlot` 필드로 슬롯 검증 — 불일치 시 강화 거절
- `Scroll` 데이터 클래스에 `strBonus/dexBonus/intBonus/lukBonus` 필드 추가
- `EnhancementService`가 성공 시 해당 `Equipment` 스탯 필드에 보너스 가산

### 무기 주문서

| 주문서 | 대상 무기 | 성공률 | 성공 시 |
|--------|----------|--------|---------|
| 검 공격력 10%/60%/100% | SWORD (targetSlot="SWORD") | 10/60/100% | 공격력 +3/+2/+1 |
| 표창 공격력 10%/60%/100% | STAR (targetSlot="STAR") | 10/60/100% | 공격력 +3/+2/+1 |
| 완드 마력 10%/60%/100% | WAND (targetSlot="WAND") | 10/60/100% | 마력 +3/+2/+1 |
| 스태프 마력 10%/60%/100% | STAFF (targetSlot="STAFF") | 10/60/100% | 마력 +3/+2/+1 |

### ScrollType enum (model/Scroll.kt)

62+ 종 — 장갑 공격력 3종 + 백의 2종 + 슬롯4×스탯4×등급3(48종) + 무기4×등급3(12종) + ...

---

## 사운드 시스템 (SoundManager)

### 구조

```kotlin
object SoundManager {
    enum class Bgm { NONE, TOWN, BATTLE }
    enum class Sfx { ATTACK, MONSTER_HIT, MONSTER_DIE, PLAYER_HIT, LEVEL_UP,
                     ITEM_PICKUP, SCROLL_SUCCESS, SCROLL_FAIL, PORTAL }

    var bgmMuted: Boolean   // setter → bgmPlayer 일시정지/재개
    var sfxMuted: Boolean

    fun init(context: Context)          // SoundPool + MediaPlayer 폴백 초기화
    fun playSfx(sfx: Sfx)               // SoundPool(저지연) 우선, 실패 시 MediaPlayer 폴백
    fun switchBgm(bgm: Bgm)             // 현재 BGM 교체 (루프 재생)
    fun bgmForMap(mapType: MapType): Bgm
    fun onPause() / onResume() / release()
}
```

### 오디오 파일 로딩 방식 (cacheAsset)

1. `assets.open()` 스트림으로 파일 존재 확인 (APK 압축 여부 무관)
2. `context.cacheDir/snd_파일명.확장자` 로 복사 (이미 있으면 재사용)
3. 절대 경로를 `SoundPool.load()` / `MediaPlayer.setDataSource()` 에 전달

→ aapt2의 FLAC/OGG 압축으로 `openFd()` 실패하는 문제를 근본 해결

### APK 압축 방지 (build.gradle.kts)

```kotlin
androidResources {
    noCompress += listOf("flac", "ogg", "mp3", "wav")
}
```

### 사운드 트리거 위치 (MainViewModel)

| 이벤트 | 함수 | SFX |
|--------|------|-----|
| 플레이어 자동 공격 | `autoAttackTick` | ATTACK |
| 몬스터 피격 | `pendingAttackTick` | MONSTER_HIT |
| 몬스터 사망 | `pendingAttackTick` | MONSTER_DIE |
| 레벨업 | `pendingAttackTick` / `projectileTick` | LEVEL_UP |
| 플레이어 피격 | `monsterAiTick` | PLAYER_HIT |
| 아이템 픽업 | `monsterAiTick` / `movePlayer` | ITEM_PICKUP |
| 포탈 이동 | `movePlayer` | PORTAL + switchBgm |
| 주문서 성공 | `useSelectedScroll` | SCROLL_SUCCESS |
| 주문서 실패 | `useSelectedScroll` | SCROLL_FAIL |

### BGM 전환

| 맵 | BGM |
|----|-----|
| TOWN | bgm_town.* |
| 그 외 (전투 맵) | bgm_battle.* |

### 생명주기 (MainActivity)

```kotlin
SoundManager.init(this)         // onCreate
SoundManager.onPause()          // onPause
SoundManager.onResume()         // onResume
SoundManager.release()          // onDestroy
```

### 음소거 버튼 (MainScreen HUD)

- 우상단 "BGM"/"BGM✗" + "SFX"/"SFX✗" HudButton 2개
- `SoundManager.bgmMuted` / `sfxMuted` 토글

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
| 130 | DataLoader 구현 — assets/data/ CSV 파싱 유틸 (따옴표 필드 지원, 컬럼 불일치 안전 처리) | ✅ |
| 131 | GameDataInitializer 구현 — 앱 시작 시 모든 CSV 로드 (장비→몬스터→드랍→NPC→상점→퀘스트 순서 보장) | ✅ |
| 132 | EquipmentRegistry 구현 — equipment.csv 기반 조회 전용, Equipment에 itemId/slot/requiredLevel/buyPrice/sellPrice 필드 추가 | ✅ |
| 133 | MonsterRegistry 구현 — monster.csv 기반, MonsterConfig(맵별 스폰 설정) + configForMap(MapType) | ✅ |
| 134 | DropRegistry 구현 — drop.csv 기반, 몬스터별 아이템 드랍 + MonsterRegistry에서 돈 범위 읽기 | ✅ |
| 135 | QuestRegistry 구현 — quest.csv 기반, QuestData(보상/목표 설정) + questForNpc(npcId) | ✅ |
| 136 | NpcRegistry CSV 전환 — 하드코딩 제거, npc.csv 로드 방식으로 전환 | ✅ |
| 137 | ShopRegistry CSV 전환 — 하드코딩 제거, shop.csv + EquipmentRegistry/ScrollCatalog/ConsumableCatalog 연동 | ✅ |
| 138 | DropTable.kt 정리 — 하드코딩 데이터 제거, DropItem/DropEntry 모델만 유지 | ✅ |
| 139 | Monster.monsterId 필드 추가 — 드랍 테이블 조회용 식별자, MonsterSpawner에 monsterId 파라미터 추가 | ✅ |
| 140 | MainViewModel 데이터 테이블 연동 — skeletonConfig 제거 후 MonsterRegistry 사용, dropEntriesFor → DropRegistry, 퀘스트 보상 → QuestRegistry | ✅ |
| 141 | assets/data/ CSV 6종 생성 — equipment/monster/drop/npc/shop/quest (엑셀 편집→CSV 저장→자동 반영) | ✅ |
| 142 | ScrollType enum 확장 — 장비 슬롯4(GLOVE/TOP/HAT/SHOES) × 스탯4(STR/DEX/INT/LUK) × 등급3(10/60/100%) = 48종 추가 | ✅ |
| 143 | 무기 주문서 추가 — SWORD/STAR 공격력 × 3등급, WAND/STAFF 마력 × 3등급 (12종) | ✅ |
| 144 | Scroll 데이터 클래스 확장 — strBonus/dexBonus/intBonus/lukBonus/magicBonus/targetSlot 필드 추가 | ✅ |
| 145 | EnhancementService 업데이트 — targetSlot 슬롯 검증 + 전 스탯 보너스 적용 로직 | ✅ |
| 146 | shop.csv 주문서 항목 확장 — 슬롯별·스탯별·무기별 주문서 ~40종 추가 | ✅ |
| 147 | SoundManager 구현 — SoundPool(저지연) + MediaPlayer(폴백) 이중 경로, cacheAsset() 캐시 복사 방식 | ✅ |
| 148 | APK 오디오 압축 방지 — build.gradle.kts androidResources.noCompress += flac/ogg/mp3/wav | ✅ |
| 149 | MainViewModel 사운드 트리거 추가 — 공격/피격/사망/레벨업/픽업/포탈/주문서 성공실패 SFX | ✅ |
| 150 | MainActivity 사운드 생명주기 연동 — init/onPause/onResume/release + 맵별 BGM 전환 | ✅ |
| 151 | HUD 음소거 버튼 추가 — 우상단 BGM/SFX 토글 버튼 (MainScreen) | ✅ |
| 152 | assets/sounds/README.txt 생성 — 사운드 파일 배치 가이드 (BGM·SFX 파일명 규칙, 추천 무료 소스) | ✅ |
| 153 | 궁수 스프라이트 개별 프레임 전환 — archer_sheet sliceSheet 방식 → loadBitmap 개별 PNG 24장 (idle5/walk4/attack6/hurt4/die5) | ✅ |
| 154 | sliceSheet / thresholdAlpha 함수 제거 — 미사용으로 삭제 | ✅ |
| 155 | drawPlayer 궁수 크기·정렬 수정 — imgW=imgH(정방형), vertRatio=0.95f, size.height*0.15f | ✅ |
| 156 | onPause 즉시 저장 추가 — moveVm.saveNow() 호출로 강제종료 시에도 데이터 보존 | ✅ |
| 157 | init 블록 몬스터 스폰 버그 수정 — CSV 로드 전 createInitialState() 호출로 MonsterRegistry 비어있던 문제, 로드 후 재설정 | ✅ |
| 158 | 아이템 픽업 범위 축소 — PICKUP_RANGE 150f → 30f (캐릭터가 아이템 위에 올라가야 획득) | ✅ |
| 159 | 투사체 범위 외 몬스터 피격 버그 수정 — ProjectileService 충돌 판정을 targetMonsterId 일치 몬스터만으로 제한 | ✅ |
| 160 | ConsumableType/ConsumableCatalog 모델 추가 + InventorySlot.ConsumableItem 추가 — 소비 아이템(물약) 인벤토리 슬롯 타입 및 카탈로그 | ✅ |
| 161 | DragDropState consumableType 확장 — 소비 아이템 드래그 지원, ConsumableDragGhost 렌더링 | ✅ |
| 162 | 물약 퀵슬롯 3개 구현 — 하단 중앙 PotionQuickSlotRow, 인벤토리 꾹 눌러 드래그 등록, 클릭으로 사용 | ✅ |
| 163 | mutableStateListOf import 누락 빌드 오류 수정 — quickSlotBounds 3슬롯 List 선언에 필요한 import 추가 | ✅ |
| 164 | 앱 재시작 시 세이브 데이터 초기화 버그 수정 — 타이틀 이어하기/새 게임 버튼 분리, 이어하기는 startGame() 호출 없이 로드된 상태 유지 | ✅ |
| 165 | 캐릭터·몬스터 발밑 그림자 제거 — drawPlayer / drawMonster의 그림자 drawCircle 삭제 | ✅ |
| 166 | 공격 시 타겟 방향으로 facingLeft 갱신 — autoAttackTick에서 타겟 X < 플레이어 X 비교, 근접·원거리 모두 적용 | ✅ |
| 167 | 공격 애니메이션 중 이동 방향에 의한 facingLeft 덮어쓰기 방지 — movePlayer에서 ATTACK_ANIM_DURATION(300ms) 동안 facingLeft 고정 | ✅ |
| 168 | Skill.kt 추가 — SkillType enum + Skill 데이터 클래스 + SkillRegistry (직업별 스킬 6종 정의) | ✅ |
| 169 | SkillEffect.kt 추가 — SkillEffectType enum (SLASH_BURST/EXPLOSION) + SkillEffect 데이터 클래스 (500ms 지속) | ✅ |
| 170 | SkillService.kt 추가 — MELEE_BURST/AOE/MULTI_SHOT 실행 로직 (SkillHit/SkillResult 반환, projectileType import 명시) | ✅ |
| 171 | UiState 스킬 필드 추가 — skillCooldownUntil: Map<String, Long> + skillEffects: List<SkillEffect> | ✅ |
| 172 | useSkill() ViewModel 함수 추가 — 쿨다운 체크, SkillService.execute(), MELEE_BURST/AOE 즉시 피해+킬처리, MULTI_SHOT 투사체 추가 | ✅ |
| 173 | skillEffectTick() 코루틴 추가 — 16ms 루프, 만료(500ms) SkillEffect 제거 | ✅ |
| 174 | drawSkillEffects Canvas 렌더 추가 — SLASH_BURST(흰 링+선) / EXPLOSION(오렌지 링) 이펙트, drawPlayer 직후 렌더 | ✅ |
| 175 | SkillButton 컴포저블 추가 — 72dp 원형, 쿨다운 파이 오버레이, 준비/Xs 텍스트, HUD 버튼 위 우하단 배치 | ✅ |
| 176 | MessageLogOverlay bottom 조정 — 80.dp → 170.dp (SkillButton 겹침 방지) | ✅ |
| 177 | 우측 상단 메뉴 버튼(≡) 추가 — isMenuOpen 토글, 기존 BGM/SFX 버튼 행 제거 | ✅ |
| 178 | 메뉴 패널 구현 — 음향 설정(BGM/SFX ON/OFF 2단 표시), 스탯/장비/인벤 GameMenuButton, 창 열면 메뉴 자동 닫힘 | ✅ |
| 179 | HudButton ×3 (스탯/장비/인벤) 하단 Row 제거 — 메뉴 패널로 이동 | ✅ |
| 180 | AttackButton 컴포저블 추가 — 72dp 원형, 위로 50px 스와이프 시 자동↔수동 모드 토글, 수동 모드 탭 시 1회 공격 | ✅ |
| 181 | UiState.autoAttackEnabled 추가 — autoAttackTick은 false 시 스킵, executeAttack()으로 로직 분리 | ✅ |
| 182 | toggleAutoAttack() / manualAttack() ViewModel 함수 추가 — AttackButton 콜백 연결 | ✅ |
| 183 | MessageLogOverlay bottom 조정 — 170.dp → 110.dp (HudButton 제거 후 여백 축소) | ✅ |
| 184 | 수동 공격 연타 방지 — executeAttack()에 AUTO_ATTACK_INTERVAL 쿨다운 체크 추가 | ✅ |
| 185 | 무기 공격속도 데이터 기반 공격 주기 적용 — Weapon.attackIntervalMs() 추가, DerivedStats.attackIntervalMs 필드, DerivedStatsCalculator 계산, 루프 100ms 전환 | ✅ |
| 186 | 미노타우르스 몬스터 3종 추가 — image/미노타우르스/Minotaur_1~3 PNG 서브샘플(Idle 6/Walk 8/Slash 6프레임)→drawable 복사, variant 4/5/6 | ✅ |
| 187 | MapType 확장 — MINOTAUR_FIELD_1/2/3 추가 + GameWorld 인스턴스 | ✅ |
| 188 | 마을 우측 포탈 추가 (750,286)→MINOTAUR_FIELD_1, 미노타우르스 사냥터 3단계 포탈 체인 구성 | ✅ |
| 189 | monster.csv 미노타우르스 3행 추가 — HP 300→500→800, EXP 80→130→200, 속도 1.2→1.4→1.6 | ✅ |
| 190 | drop.csv 미노타우르스 드랍 추가 — 스켈레톤 대비 드랍률 상향 (최대 글장100% 35%, 클로버 5%) | ✅ |
| 191 | MonsterSpawner name 파라미터 추가 — skeletonWarrior 하드코딩 제거, Monster() 직접 생성으로 변경 | ✅ |
| 192 | MainActivity 미노타우르스 스프라이트 로딩 + drawMonster monsterId 분기 (MINOTAUR→minoFrames, 나머지→skelFrames) | ✅ |
| 193 | MaterialType 모델 추가 — MaterialType enum (SKELETON_BONE/BEEF) + MaterialCatalog (이름·판매가) | ✅ |
| 194 | DropItem.MaterialDrop 추가 — DropTable sealed class 확장, DropRegistry.resolveItem에 MaterialType 분기 | ✅ |
| 195 | 재료 아이템 드랍 설정 — 스켈레톤 워리어 전 사냥터 스켈레톤뼈 100%, 미노타우르스 전 사냥터 소고기 100% | ✅ |
| 196 | InventorySlot.MaterialItem 추가 — addMaterialToSlots 헬퍼, applyDrops MaterialDrop 처리, 획득 메시지 포함 | ✅ |
| 197 | SaveService MaterialItem 직렬화 — SavedSlot.materialType/materialQty 필드 추가, MATERIAL 슬롯 타입 지원 | ✅ |
| 198 | drawGroundItem 재료 아이템 렌더링 — skeleton_bone/beef 비트맵 로딩 및 분기, 글로우·이름 텍스트 제거 | ✅ |
| 199 | MaterialInventoryItem 컴포저블 추가 — painterResource로 PNG 이미지 표시 + 우하단 수량 텍스트 | ✅ |
| 200 | 반응형 창 너비 도입 — `panelW = (screenWidthDp * 0.29f).dp` 계산, 장비창·스탯창·인벤토리창 `.width(panelW)` 적용 | ✅ |
| 201 | `rememberSlotSize()` 컴포저블 헬퍼 추가 — `(screenWidthDp * 0.032f).dp.coerceIn(10.dp, 18.dp)`, `GlovesSlot`/`WeaponSlot` 시그니처에 `slotSize: Dp` 파라미터 추가, `import androidx.compose.ui.unit.Dp` 필요 | ✅ |
| 202 | EquipmentWindow 내부 UI 축소 — padding 12→6/6→3dp, Spacer 6→3dp, slotSz = rememberSlotSize() 사용 | ✅ |
| 203 | StatWindow 높이 비례 제한 — `statMaxH = screenHeightDp * 0.32f`, `heightIn(max=statMaxH)` + verticalScroll 적용 | ✅ |
| 204 | StatWindow 내부 텍스트 축소 — DerivedStatRow·StatAllocRow fontSize 11→9.sp, StatAllocRow + 버튼 size 22→18.dp, Divider padding 8→4.dp | ✅ |
| 205 | WindowTitleBar 크기 축소 — padding 8/8→6/4dp, dot size 8→5dp, title/✕ fontSize 12→10.sp | ✅ |
| 206 | InventoryWindow 그리드 높이 비례 — `maxGridH = screenHeightDp * 0.62f`, `heightIn(max=maxGridH)` + verticalScroll; 슬롯 간격 4→2dp; money row padding 축소 | ✅ |
| 207 | ItemInfoDialog 너비 비례 + 레이아웃 수직 전환 — 너비 `screenWidthDp * 0.29f`, 이미지+스탯 Row→Column, 이미지 size 96→48dp, WeaponReqRow/WeaponStatRow fontSize 9.sp, Job tab fontSize 9.sp | ✅ |
| 208 | ShopWindow 너비 비례 + 레이아웃 수직 누적 전환 — 너비 `screenWidthDp * 0.29f`, 리스트·상세 Row→Column stacked, shopListH=`screenHeightDp*0.18f`, shopDetailH=`screenHeightDp*0.22f`, 내부 버튼 height 24dp, 텍스트 8~10.sp | ✅ |
| 209 | ground_tileset.png 추가 — 1254×1254 4×4 바닥 타일셋 (흙/풀/자갈/석재 각 4종), GT_CELL=313, GT_BORDER=40, GT_TILE=233 | ✅ |
| 210 | drawCemeteryBackground 구현 — 스켈레톤·미노타우르스 사냥터 전용 타일 기반 배경 렌더러, 32×18 그리드, FilterQuality.None | ✅ |
| 211 | 맵별 바닥 타일 구성 — BEGINNER: 풀 4종(row1), FIELD_2: 흙+풀 혼합, FIELD_3: 흙+균열+자갈, 엣지 고정 타일 | ✅ |
| 212 | 사냥터 오브젝트 전체 제거 — DECOR_* 상수·DECOR_LIST_* 삭제, drawCemeteryBackground 순수 바닥 렌더링만 유지 | ✅ |
| 213 | 스켈레톤 사냥터 포탈 위치 상단으로 이동 — y=286 → y=80 (묘지 석문 위치 반영), 양방향 도착 좌표도 y=100으로 조정 | ✅ |
| 214 | 스켈레톤 사냥터(FIELD_2/FIELD_3) 배경을 map_cemetery.png 비트맵으로 교체 — drawCemeteryBackground 타일 렌더러 전면 제거, GT_CELL/GT_BORDER/GT_TILE 상수 삭제 | ✅ |
| 215 | BEGINNER_FIELD 배경을 map_forest.jpg 숲 이미지로 교체 — image/초보자 사냥터.jpg(1024×572) drawable 복사, forestBitmap 로드 추가 | ✅ |
| 216 | 테스트 장비 4종 추가 — TEST_HAT(모자1)/TEST_TOP(상의1)/TEST_GLOVE(장갑1)/TEST_SHOES(신발1), 공격력 1, 강화 가능 횟수 5 (equipment.csv) | ✅ |
| 217 | 테스트 장비 드랍 설정 — 스켈레톤·미노타우르스 전 몬스터 6종에 각 5% 드랍 (drop.csv) | ✅ |
| 218 | DerivedStatsCalculator 다중 슬롯 지원 — glove/hat/top/shoes 4개 부위 스탯 합산, calculate() 파라미터 확장 | ✅ |
| 219 | UiState hat/top/shoes 필드 추가 — GameSaveData/SaveService 저장·복원 포함 | ✅ |
| 220 | equipFromInventory slot 기반 라우팅 — equipment.slot("HAT"/"TOP"/"SHOES"/"GLOVE") 분기로 올바른 슬롯에 장착 | ✅ |
| 221 | unequipEquipment(slot: String = "GLOVE") 확장 — 기존 GLOVE 동작 유지하며 HAT/TOP/SHOES 해제 지원 | ✅ |
| 222 | useSelectedScroll slot 라우팅 — scroll.targetSlot 기준으로 hat/top/shoes/equipment 중 올바른 슬롯에 주문서 적용, 오류 메시지 슬롯명 표시 | ✅ |
| 223 | EquipmentWindow ArmorSlot 컴포저블 추가 — 모자/상의/신발 슬롯 LockedSlot→ArmorSlot으로 교체, 텍스트 기반 (탭=정보, 꾹=해제) | ✅ |
| 224 | 포탈 y 좌표 수정 — 스켈레톤 사냥터 포탈 y=80 → y=286 (충돌 맵 기준 접근 가능 위치), 도착 좌표도 y=286으로 통일 | ✅ |
| 225 | 주문서 UI 이미지 통합 — scrollDrawableRes를 suffix 기반으로 전환 (_100→scroll_100, _60→scroll_60, _10→scroll_10), 모든 슬롯·스탯 주문서에 동일 이미지 적용 | ✅ |
| 226 | drawGroundItem 주문서 비트맵 분기 통합 — suffix 기반 분기로 변경, 모든 주문서 종류가 바닥 드랍 시 올바른 이미지로 렌더링 | ✅ |
| 227 | ArmorSlot 드래그-드랍 강화 지원 — hatSlotBounds/topSlotBounds/shoesSlotBounds 상태 추가, ArmorSlot에 onBoundsChanged 파라미터 연결, EquipmentWindow에 onHatBounds/onTopBounds/onShoesBounds 전달 | ✅ |
| 228 | 주문서 슬롯 일치 검증 — onDragEnd에서 scroll.targetSlot 기준으로 해당 슬롯에만 드랍 감지 (장갑 주문서→장갑 슬롯에만, 모자 주문서→모자 슬롯에만 발동) | ✅ |
| 229 | Weapon 강화 필드 추가 — remainingUpgradeCount/failedUpgradeCount/destroyed 필드 추가, 기존 세이브 호환 (GSON 기본값 처리) | ✅ |
| 230 | EnhancementService.applyScrollToWeapon() 추가 — 무기 전용 주문서 강화 로직 (targetSlot="WEAPON" 검증, attackPower/magicPower/strBonus 적용) | ✅ |
| 231 | useSelectedScroll WEAPON 분기 추가 — targetSlot=="WEAPON"일 때 applyScrollToWeapon() 호출, 무기 슬롯 강화 결과 UiState 반영 | ✅ |
| 232 | WeaponSlot 드래그-드랍 강화 지원 — weaponSlotBounds 상태 추가, WeaponSlot에 onBoundsChanged 파라미터 연결, onDragEnd에서 "WEAPON"→weaponSlotBounds 체크 | ✅ |
| 233 | 테스트 장비 이미지 교체 — TEST_HAT(갈샛 삿갓)/TEST_TOP(지장의)/TEST_SHOES(흰색 고무신) drawable 변경, TEST_GLOVE 삭제(equipment.csv+drop.csv), TEST_PANTS(백진일갑주 바지)/TEST_BOW(사냥꾼의 활) 신규 추가 | ✅ |
| 234 | 장비 이미지 256×256 정규화 — Python PIL resize(LANCZOS)로 투명 경계 크롭 후 콘텐츠 업스케일 (thumbnail()은 다운스케일 전용이라 소형 이미지 정규화에 사용 불가) | ✅ |
| 235 | 인벤토리 장비 이미지 크기 수정 — EquipmentBagItem 이중 패딩(Box 4dp + Image inner 4dp) 제거, Image Modifier.fillMaxSize() 단독 적용 | ✅ |
| 236 | 바닥 드랍 장비 이미지 수정 — drawGroundItem에 equipBitmaps: Map<String,ImageBitmap> 파라미터 추가, itemId별 올바른 비트맵 렌더링 (모든 장비가 NOGADA_GLOVE로 표시되던 하드코딩 버그 수정) | ✅ |
| 237 | 바닥 드랍 장비 아이콘 크기 분기 — when 표현식: TEST_HAT 1.6×, NOGADA_GLOVE 1.0×(기본), 기타 장비(TEST_TOP/SHOES/PANTS/BOW) 1.3× | ✅ |
| 238 | PANTS 슬롯 전체 플러밍 — UiState.pants 필드, DerivedStatsCalculator pants 파라미터, equipFromInventory/unequipEquipment/useSelectedScroll PANTS 분기, GameSaveData/SaveService 저장·복원 | ✅ |
| 239 | WEAPON 슬롯 장착 변환 — equipFromInventory WEAPON 분기: equipmentToWeapon() 헬퍼로 Equipment→Weapon 변환(weaponTypeFor: TEST_BOW→"활", 그 외→"한손검") | ✅ |
| 240 | ArmorSlot 장착 이미지 표시 — equipmentDrawableRes(itemId) 기반 PNG 이미지 표시, 이미지 없으면 슬롯 이름 텍스트 폴백 | ✅ |
| 241 | PANTS 주문서 드래그-드랍 지원 — pantsSlotBounds 상태, ArmorSlot onBoundsChanged 파라미터 추가, EquipmentWindow onPantsBounds 전달, onDragEnd "PANTS" 분기 | ✅ |
| 242 | Weapon 모델 itemId 필드 추가 — equipmentToWeapon 시 원본 itemId 보존 | ✅ |
| 243 | unequipWeapon 인벤토리 복원 — itemId 있으면 EquipmentRegistry로 Equipment 복원 후 인벤토리에 추가 (기존: weapon=null만 해 아이템 소실) | ✅ |
| 244 | ItemInfoDialog 이미지 수정 — nogada_glove 하드코딩 → equipmentDrawableRes(itemId) 사용, 없으면 nogada_glove 폴백 | ✅ |
| 245 | ItemInfoDialog 스탯 표시 개선 — 공격력 외 마력·STR·DEX·INT·LUK·물리방어·마법방어·명중률·회피율 값>0 시 조건부 표시 | ✅ |
| 246 | 장비창 전용 너비 분리 — equipPanelW (0.155f) / equipPanelWPx 추가, 스탯창·인벤토리창은 panelW (0.29f) 유지 | ✅ |
| 247 | 장비창 슬롯 크기 조정 — rememberSlotSize 0.044f, coerceIn(14.dp, 26.dp) | ✅ |
| 248 | 상점 구매/판매 버튼 클리핑 수정 — 상세 패널 height() 고정 제거, 콘텐츠 스크롤 영역 + 버튼 고정 분리 구조로 전환 (Spacer weight 오버플로우 시 버튼이 잘리던 문제 해결) | ✅ |

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
val panelW   = (configuration.screenWidthDp * 0.29f).dp   // 화면 너비의 29% ≈ 30%
val panelWPx = with(density) { panelW.toPx() }
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

- `panelW`는 화면 너비의 **29%** 비례값 — 장비창·스탯창·인벤토리창·장비정보창·상점창 모두 동일 너비
- 슬롯 크기: `rememberSlotSize() = (screenWidthDp * 0.032f).dp.coerceIn(10.dp, 18.dp)` 화면 비례
- 인벤토리 그리드 최대 높이: `screenHeightDp * 0.62f`
- 상점창·장비정보창은 Dialog 내부에서 `LocalConfiguration.current.screenWidthDp * 0.29f` 로 동일 너비 계산

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

- 위치: `Alignment.BottomEnd`, `padding(end=12.dp, bottom=170.dp)` (SkillButton 겹침 방지)
- 각 메시지: 검정 반투명 배경 + RoundedCorner(4.dp) + 13sp Bold 텍스트
- `messages.isNotEmpty()` 일 때만 렌더링

---

## 궁수 스프라이트 시스템

### 개별 프레임 파일 구조

- 원본 그리드 이미지(`image/궁수/`) → Python PIL로 자동 커팅 → 개별 203×203 PNG 24장
- 배경 제거: `R>200 & G>200 & B>200 & saturation≤30` → alpha=0

| 애니메이션 | 파일 | 프레임 수 |
|-----------|------|----------|
| Idle      | `archer_idle_0~4.png`   | 5 |
| Walk      | `archer_walk_0~3.png`   | 4 |
| Attack    | `archer_attack_0~5.png` | 6 |
| Hurt      | `archer_hurt_0~3.png`   | 4 |
| Die       | `archer_die_0~4.png`    | 5 |

### 로딩 방식 (GameCanvas 내부)

```kotlin
val archerIdle   = remember { listOf(
    loadBitmap(context, R.drawable.archer_idle_0, 256), ...
) }
// Walk / Attack / Hurt / Die 동일 패턴
```

### 직업별 프레임 선택 (GameCanvas Canvas 블록)

```kotlin
val isArcher = player.job == PlayerJob.ARCHER
val pIdle   = if (isArcher) archerIdle   else warriorIdle
// Walk / Attack / Hurt / Die 동일
drawPlayer(player, cam, pIdle, pWalk, pAttack, pHurt, pDie, ...)
```

### drawPlayer 궁수 전용 파라미터

```kotlin
val imgH = (size.height * (if (isArcher) 0.15f else 0.11f)).toInt()
val imgW = imgH   // 203×203 정사각형
val vertRatio = if (isArcher) 0.95f else 0.75f  // 발 위치 95% 높이
```

- 애니메이션 타이밍은 전사와 동일 (IDLE 200ms/f, WALK 100ms/f, ATTACK 60ms/f, HURT 100ms/f, DIE 200ms/f)

---

## 스킬 시스템

### 모델 (model/Skill.kt)

```kotlin
enum class SkillType { MELEE_BURST, AOE, MULTI_SHOT }

data class Skill(
    val id: String,
    val name: String,
    val type: SkillType,
    val cooldownMs: Long,
    val range: Float,
    val damageMultiplier: Float,
    val aoeRadius: Float = 0f,
    val shotCount: Int = 1
)

object SkillRegistry {
    fun skillFor(job: PlayerJob): Skill = when (job) { ... }
}
```

### 직업별 스킬 (SkillRegistry)

| 직업 | 스킬 ID | 이름 | 타입 | 쿨다운 | 범위 | 배율 | 비고 |
|------|---------|------|------|--------|------|------|------|
| WARRIOR  | warrior_skill  | 강타   | MELEE_BURST | 8초 | 70f  | 3.0× | — |
| MAGE     | mage_skill     | 파이어볼 | AOE        | 6초 | 150f | 3.0× | aoeRadius=80f |
| ARCHER   | archer_skill   | 연사   | MULTI_SHOT  | 5초 | 220f | 2.0× | shotCount=3 |
| THIEF    | thief_skill    | 연속표창 | MULTI_SHOT  | 4초 | 180f | 1.5× | shotCount=3 |
| PIRATE   | pirate_skill   | 폭발탄 | AOE         | 7초 | 190f | 2.5× | aoeRadius=70f |
| BEGINNER | beginner_skill | 기합   | MELEE_BURST | 5초 | 60f  | 1.5× | — |

### 스킬 이펙트 모델 (model/SkillEffect.kt)

```kotlin
enum class SkillEffectType { SLASH_BURST, EXPLOSION }

data class SkillEffect(
    val id: Int,
    val type: SkillEffectType,
    val worldX: Float, val worldY: Float,
    val radius: Float,
    val startedAt: Long
) {
    companion object { const val DURATION = 500L }
    fun isExpired(now: Long) = now - startedAt > DURATION
    fun progress(now: Long) = ((now - startedAt).toFloat() / DURATION).coerceIn(0f, 1f)
}
```

### SkillService (service/SkillService.kt)

```kotlin
data class SkillHit(val monsterId: Int, val damage: Int)
data class SkillResult(val hits: List<SkillHit>, val newProjectiles: List<Projectile>, val effect: SkillEffect?)

class SkillService {
    fun execute(skill, player, monsters, derivedStats, nextProjectileId, nextEffectId): SkillResult
}
```

| 타입 | 동작 | 이펙트 위치 |
|------|------|-----------|
| MELEE_BURST | 범위 내 모든 몬스터 즉시 피해 | 플레이어 위치에 SLASH_BURST |
| AOE | 최근접 타겟 이후 aoeRadius 내 모든 몬스터 피해 | 타겟 위치에 EXPLOSION |
| MULTI_SHOT | 가장 가까운 N마리에 각각 투사체 생성 | 플레이어 위치에 SLASH_BURST (35f) |

> **참고**: `projectileType()` / `projectileSpeed()`는 `model/PlayerJob.kt`의 extension 함수 — `SkillService.kt`에서 명시적 import 필요
> ```kotlin
> import com.a_survivor.app.model.projectileSpeed
> import com.a_survivor.app.model.projectileType
> ```

### UiState 추가 필드

```kotlin
val skillCooldownUntil: Map<String, Long> = emptyMap(),  // key = skill.id
val skillEffects: List<SkillEffect> = emptyList()
```

### ViewModel 함수

| 함수 | 동작 |
|------|------|
| `useSkill()` | 쿨다운 확인 → SkillService.execute() → hits 처리 (즉사/EXP/드랍/킬 전체) + MULTI_SHOT은 projectiles 추가 + effect 추가 + cooldownUntil 갱신 |
| `skillEffectTick()` | 16ms 루프, 만료된 SkillEffect 제거 (`isExpired(now)`) |

### drawSkillEffects (MainActivity Canvas)

```kotlin
private fun DrawScope.drawSkillEffects(effects: List<SkillEffect>, cam: CameraState)
```

| 이펙트 타입 | 렌더링 |
|------------|--------|
| SLASH_BURST | 흰 확장 링 + 내부 금색 링 + 45°/135°/225°/315° 방향 짧은 선 (alpha = 1-progress) |
| EXPLOSION | 오렌지 반투명 채움 원 + 적색 링 + 황색 내부 링 (alpha = 1-progress) |

### SkillButton 컴포저블

- **크기**: 72dp 원형
- **배치**: `Alignment.BottomEnd`, `padding(end=16.dp, bottom=20.dp)` + Column에서 HUD 버튼 위
- **쿨다운 UI**: 100ms tick(`LaunchedEffect`), 파이 오버레이(`drawArc`, startAngle=-90f, sweepAngle=fraction×360f, useCenter=true)
- **텍스트**: 쿨다운 중 "Xs" (초 단위 올림) / 준비 시 "준비" (직업 accent color)
- **경계**: 준비 시 accent color 2.5dp 링 + 외부 글로우, 쿨다운 시 어두운 1.5dp 링

---

## 기본 공격 버튼 시스템

### AttackButton 컴포저블

- **크기**: 72dp 원형 (SkillButton과 동일)
- **배치**: SkillButton과 함께 Row(우하단), `Alignment.BottomEnd`, `padding(end=16.dp, bottom=20.dp)`
- **자동 모드** (`autoAttackEnabled=true`): 금색(`0xFFFFD700`) 링 + 글로우, "자동" 텍스트
- **수동 모드** (`autoAttackEnabled=false`): 파란색(`0xFF6699AA`) 링, "수동" 텍스트

### 제스처

| 제스처 | 동작 |
|--------|------|
| 위로 50px 이상 스와이프 | 자동↔수동 모드 토글 (`toggleAutoAttack()`) |
| 탭 (수동 모드) | 즉시 1회 공격 (`manualAttack()`) |
| 탭 (자동 모드) | 아무 동작 없음 |

- `rememberUpdatedState` 사용 — `pointerInput(Unit)` 코루틴을 재시작하지 않고 최신 상태 참조
- raw `awaitPointerEventScope` + `pressed && !previousPressed` 패턴으로 DOWN 감지

### UiState 추가 필드

```kotlin
val autoAttackEnabled: Boolean = true  // 세이브에 미포함, 세션마다 true로 초기화
```

### ViewModel 함수

| 함수 | 동작 |
|------|------|
| `toggleAutoAttack()` | autoAttackEnabled 반전 |
| `manualAttack()` | executeAttack() 직접 호출 (1회) |
| `executeAttack()` | hp>0 체크 → `derivedStats.attackIntervalMs` 쿨다운 체크 → 투사체/근접 분기 |

- `autoAttackTick()`: `!autoAttackEnabled` 시 즉시 return, 아니면 `executeAttack()` 호출
- 자동·수동 모두 `executeAttack()` 공유 → 동일한 `attackIntervalMs` 쿨다운 적용, 연타 불가

---

## 메뉴 시스템

### 메뉴 버튼

- **위치**: `Alignment.TopEnd`, `padding(end=12.dp, top=8.dp)`, `zIndex(30f)` — 창 위에 표시
- **크기**: 44dp 정사각형, RoundedCornerShape(8.dp)
- **레이블**: "≡" (열림: 금색 테두리 + 배경, 닫힘: 반투명)
- 클릭 시 `isMenuOpen` 토글

### 메뉴 패널 (isMenuOpen=true 시 버튼 아래 offset=52dp에 표시)

```
Column (170dp 너비, RoundedCornerShape 8dp, 반투명 패널)
 ├── "음향 설정" 헤더 텍스트
 ├── Row
 │    ├── BGM 토글 버튼 (ON/OFF 표시, 녹/적 색상)
 │    └── SFX 토글 버튼 (ON/OFF 표시, 녹/적 색상)
 ├── HorizontalDivider
 ├── GameMenuButton "스탯"  → isStatOpen 토글 + isMenuOpen=false
 ├── GameMenuButton "장비"  → isEquipmentOpen 토글 + isMenuOpen=false
 └── GameMenuButton "인벤" → isInventoryOpen 토글 + isMenuOpen=false
```

### GameMenuButton 컴포저블

- `fillMaxWidth()`, `height(36.dp)`, `RoundedCornerShape(6.dp)`
- 활성화 시 금색 테두리 + 금색 텍스트 Bold, 비활성화 시 반투명

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
| 앱 실행 시 | `init` 블록 — `GameDataInitializer.initialize()` 후 저장 데이터 복원, 없으면 신규 시작 |
| 10초마다 | `viewModelScope.launch { while(true) { delay(10_000L); save() } }` |
| 앱 백그라운드/종료 | `onPause()` → `moveVm.saveNow()` 즉시 저장 (강제종료 대응) |
| ViewModel 소멸 | `onCleared()` 오버라이드에서 즉시 저장 |
| 새 게임 시작 | `startGame()` 에서 새 상태 즉시 저장 (기존 세이브 덮어씀) |

### 이어하기 흐름

1. ViewModel `init` → `saveService.load()` → `_uiState.value = restoreState(data)` (세이브 로드)
2. 타이틀 "이어하기" 클릭 → `currentScreen = AppScreen.Game` **(`startGame()` 호출 없음)**
3. 로드된 상태 그대로 게임 진입 (레벨·인벤토리·소지금·맵 모두 유지)

> **주의**: 타이틀에서 `startGame()`을 호출하면 `createInitialState()`로 초기화되어 세이브가 덮어씌워짐.
> 이어하기는 반드시 `startGame()` 없이 화면 전환만 수행해야 함.

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

## 포션 퀵슬롯 시스템

### UiState 추가 필드

```kotlin
val quickSlots: List<ConsumableType?> = List(3) { null }
```

### ViewModel 함수

| 함수 | 동작 |
|------|------|
| `assignQuickSlot(index, type)` | quickSlots[index] = type (인벤토리에서 드래그 등록) |
| `useQuickSlotPotion(index)` | quickSlots[index] 포션 사용 (usePotion 호출) |

### 드래그 플로우

```
인벤토리 ConsumableItem 꾹 누르기
  → dragState.consumableType = ConsumableType
  → ConsumableDragGhost 렌더링 (손가락 위치 추적)
  → 손 뗌 → onConsumableDragEnd(ct, dropPos)
     → quickSlotBounds.indexOfFirst { it?.contains(dropPos) == true }
     → 일치 슬롯 있으면 onAssignQuickSlot(idx, ct)
```

### DragDropState 확장

```kotlin
class DragDropState {
    var scrollType     by mutableStateOf<ScrollType?>(null)
    var consumableType by mutableStateOf<ConsumableType?>(null)
    var position       by mutableStateOf(Offset.Zero)
    val isDragging     get() = scrollType != null || consumableType != null
}
```

### 퀵슬롯 UI 컴포저블

| 컴포저블 | 역할 |
|---------|------|
| `PotionQuickSlotRow` | 3개 슬롯 Row, 하단 중앙 (`Alignment.BottomCenter`, padding bottom=20.dp) |
| `PotionQuickSlot` | 단일 슬롯 — 등록된 포션 이미지/이름 표시, 클릭 시 사용, `onGloballyPositioned`로 bounds 등록 |
| `ConsumableDragGhost` | 드래그 중 손가락 위치에 포션 이미지 표시 |

### 슬롯 위치 추적 (드롭 판정용)

```kotlin
val quickSlotBounds = remember { mutableStateListOf<Rect?>(null, null, null) }
// 각 슬롯이 onSlotPositioned { idx, rect -> quickSlotBounds[idx] = rect } 호출
// import androidx.compose.runtime.mutableStateListOf 필요
```

---

## 다음 작업 후보 (우선순위 순)

- [ ] 사운드 파일 실제 배치 — bgm_town/bgm_battle + sfx_* (freesound.org 등 CC0 소스)
- [ ] 투사체 PNG 리소스 교체 (에너지볼트/화살/표창/총알 이미지)
- [ ] NPC 퀘스트 2차 — quest.csv에 퀘스트 추가만으로 확장 가능
- [ ] 장비 창고
- [ ] 레벨업 연출 (파티클/메시지 이펙트)
- [ ] 스킬 강화 / 스킬 트리 확장
- [ ] map.csv / portal.csv / dialogue.csv / monster_spawn.csv / equipment_set.csv 추가 예정

---

## UI 반응형 크기 시스템 (작업 #200–208)

모든 플로팅 창이 디바이스 화면 크기에 비례하도록 변경. 기준 단위는 `LocalConfiguration.current.screenWidthDp` / `screenHeightDp`.

### 창 너비

| 창 | 공식 | 비고 |
|----|------|------|
| 장비창·스탯창·인벤토리창 | `screenWidthDp * 0.29f` | MainScreen에서 `panelW` 로 계산 |
| 장비정보창 (Dialog) | `screenWidthDp * 0.29f` | ItemInfoDialog 내부에서 재계산 |
| 상점창 (Dialog) | `screenWidthDp * 0.29f` | ShopWindow 내부에서 재계산 |

### 슬롯 크기

```kotlin
@Composable
private fun rememberSlotSize(): Dp =
    (LocalConfiguration.current.screenWidthDp * 0.032f).dp.coerceIn(10.dp, 18.dp)
```

- `GlovesSlot(slotSize: Dp = rememberSlotSize())` — 기본값으로 호출 가능
- `WeaponSlot(slotSize: Dp = rememberSlotSize())` — 동일
- `EquipmentWindow` 내부에서 `val slotSz = rememberSlotSize()` 한 번 계산 후 자식에 전달

### 높이 제한

| 컨테이너 | 공식 |
|----------|------|
| StatWindow 스크롤 영역 | `screenHeightDp * 0.32f` |
| InventoryWindow 그리드 | `screenHeightDp * 0.62f` |
| ShopWindow 목록 | `screenHeightDp * 0.18f` |
| ShopWindow 상세 패널 | `screenHeightDp * 0.22f` (`height()` 고정 — `Spacer(weight)` 동작 유지) |

### 레이아웃 변경 내역

- **ItemInfoDialog**: 이미지+스탯 `Row` → `Column` (29% 너비에서 이미지 96dp 좌·스탯 우 배치 불가), 이미지 `size(48.dp)`로 축소
- **ShopWindow**: 리스트·상세 `Row` 나란히 → `Column` 수직 누적 (좁은 창에서 두 컬럼 렌더링 불가)

### 내부 텍스트 크기 기준

| 용도 | sp |
|------|----|
| 창 제목 (`WindowTitleBar`) | 10 |
| 섹션 헤더 (`"전투 능력치"` 등) | 9 |
| 일반 레이블·수치 행 | 9 |
| 보조 주석 (`note`) | 7 |
| 아이템 이름 (상점·정보창 주제목) | 10 |
| 아이템 설명·부제목 | 8–9 |
