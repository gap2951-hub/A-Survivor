# A-Survivor HandOff

## 프로젝트 개요

메이플스토리 스타일의 픽셀아트 사냥터를 배경으로 한 안드로이드 생존형 게임.
주문서 강화 시스템, 몬스터 AI, 픽셀 충돌, 마을/포탈 시스템 구현 완료.

- **패키지명:** `com.a_survivor.app`
- **언어:** Kotlin + Jetpack Compose
- **minSdk:** 24 / **targetSdk:** 36
- **GitHub:** https://github.com/gap2951-hub/A-Survivor

---

## 아키텍처

```
com.a_survivor.app/
├── MainActivity.kt
├── model/
│   ├── Equipment.kt
│   ├── Scroll.kt / EnhancementResult.kt
│   ├── Player.kt
│   ├── PlayerJob.kt
│   ├── PlayerStats.kt            (+ StatType enum)
│   ├── Weapon.kt                 (+ DefaultWeapon)
│   ├── GameWorld.kt              (1024×572 + MapType enum)
│   ├── Monster.kt                (+ slime() + distanceTo() + MonsterState)
│   ├── MonsterState.kt           (IDLE / AGGRO / ATTACKING)
│   ├── CameraState.kt            (좌표 변환 / 추적)
│   ├── DropTable.kt              (DropItem + SlimeDropTable)
│   ├── GroundItem.kt             (바닥 드랍 아이템 + droppedAt 타임스탬프)
│   ├── DamageNumber.kt           (데미지 숫자 floating 표시)
│   └── Portal.kt                 (Portal 모델 + PortalRegistry)
├── service/
│   ├── EnhancementService.kt
│   ├── CombatStatCalculator.kt
│   ├── MonsterSpawner.kt         (isBlocked 람다 파라미터)
│   ├── AutoAttackService.kt      (ATTACK_RANGE = 60f)
│   ├── MonsterAiService.kt       (추적 / 공격 / 어그로 해제)
│   ├── LevelService.kt
│   └── DropService.kt
├── viewmodel/
│   └── MainViewModel.kt          (AndroidViewModel — 충돌 비트맵 + AI 틱 루프)
└── res/drawable/
    ├── map_beginner.jpg           ← 초보자 사냥터 맵 (1024×572)
    ├── map_town.jpg               ← 마을 맵 (1024×572)
    ├── slime.png
    ├── nogada_glove.png
    ├── nogada_sword.png
    ├── scroll_100.png
    ├── scroll_60.png
    └── scroll_10.png
```

---

## 화면 구조

```
Box (게임 화면)
 ├── GameCanvas              ← Canvas 기반 게임 렌더링 (fillMaxSize)
 ├── GameHud (좌상단)         ← Lv. 직업명 + HP 바
 ├── ResultPanel (상단 중앙)  ← 강화 결과 (있을 때만)
 ├── Column + zIndex(10) > EquipmentWindow  ← 플로팅 드래그 창
 ├── Column + zIndex(10) > StatWindow       ← 플로팅 드래그 창
 ├── Column + zIndex(10) > InventoryWindow  ← 플로팅 드래그 창
 ├── HudButton ×3 (우하단)   ← 스탯 / 장비 / 인벤
 ├── JoystickControl (좌하단)
 └── DragGhost
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
| 4 | `drawAttackRange` — 공격 범위 원 (반투명 흰색, r=60) |
| 5 | `drawMonster` × N — 그림자 → 슬라임 이미지 → HP 바 → 어그로 "!" |
| 6 | `drawPlayer` — 그림자 → 몸통 → 테두리 → 하이라이트 |
| 7 | `drawDamageNumber` × N — 데미지 숫자 (노랑: 플→몬, 빨강: 몬→플) |

### 시각 사양 (화면 비례 크기)

| 대상 | 표현 방식 | 크기 |
|------|-----------|------|
| 플레이어 | 주황 원 `#FFAA33` | `size.height * 0.026f` |
| 슬라임 (IDLE) | PNG 이미지 + 초록 HP 바 | `size.height * 0.088f` |
| 슬라임 (AGGRO/ATTACKING) | PNG 이미지 + 주황 HP 바 + 빨간 "!" | `size.height * 0.088f` |
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
enum class MapType { BEGINNER_FIELD, TOWN }
```

### 맵별 설정

| 맵 | 이미지 | 월드 크기 | 몬스터 | 플레이어 초기 위치 |
|----|--------|-----------|--------|------------------|
| BEGINNER_FIELD | map_beginner.jpg | 1024×572 | 슬라임 5마리 + 리스폰 | (512, 286) |
| TOWN | map_town.jpg | 1024×572 | 없음 | - |

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
| TOWN | (250, 286) | BEGINNER_FIELD | (750, 286) |

> 마을 포탈이 x=250에 있는 이유: x=144(lum=68)·x=168~176(lum=30~70) 장애물 때문에
> x=180 이전은 접근 불가 함정 지대. x=250(lum=146)부터 완전 개활지.

### 포탈 동작

- 진입 범위: `PORTAL_RANGE = 30f`
- 쿨다운: `PORTAL_COOLDOWN = 2000ms` (역방향 즉시 이동 방지)
- 맵 전환 시: 몬스터·드랍·리스폰 전부 초기화
- BEGINNER_FIELD 복귀 시: 슬라임 5마리 새로 스폰

---

## 데미지 숫자 시스템

```kotlin
data class DamageNumber(
    val id: Int, val value: Int,
    val worldX: Float, val worldY: Float,
    val createdAt: Long,
    val isPlayerDamage: Boolean
)
```

- 800ms 동안 위로 55 월드 유닛 float + 페이드아웃
- `false` (노랑): 플레이어→몬스터 / `true` (빨강): 몬스터→플레이어

---

## 플레이어 시스템

| 필드 | 초기값 |
|------|--------|
| level / exp | 1 / 0 |
| hp / maxHp | 100 / 100 |
| job | WARRIOR |
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
| 공격 범위 | 60f (전사 근접 기준) |
| 공격 주기 | 1초 |
| 타겟 | 범위 내 최근접 몬스터 |
| 데미지 | CombatStatCalculator (최소 1) |

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
| 이동 속도 | 1.2f / tick |
| AI 틱 | 16ms |
| 공격 범위 | 35f |
| 공격 주기 | 1000ms |
| 공격 데미지 | 5 |

---

## 몬스터 리스폰 시스템

- 처치 후 5초 뒤 유효 위치에 재스폰
- `PendingRespawn(monsterId, diedAt)` → UiState 포함
- 포탈 이동 시 `pendingRespawns` 초기화 (사냥터 복귀 시 5마리 즉시 스폰)

---

## 드랍 시스템

### SlimeDropTable

| 아이템 | 확률 |
|--------|------|
| 노가다 목장갑 | 5% |
| 장갑 공격력 100% | 20% |
| 장갑 공격력 60% | 10% |
| 장갑 공격력 10% | 3% |
| 백의 주문서 1% | 1% |

- 드랍 후 **PICKUP_DELAY = 1500ms** 경과 후부터 픽업 가능
- **PICKUP_RANGE = 50f** 이내 자동 습득
- 포탈 이동 시 바닥 아이템 초기화

---

## 전투 스탯 계산

| 직업 | 계산식 |
|------|--------|
| 전사 / 초보자 | 무기공격력 + 장갑공격력 + STR × 0.5 |
| 궁수 / 해적   | 무기공격력 + 장갑공격력 + DEX × 0.5 |
| 도적          | 무기공격력 + 장갑공격력 + LUK × 0.5 |
| 마법사        | 무기마력 + 장갑마력 + INT × 0.5 |

---

## MainViewModel — 주요 상수

```kotlin
MOVE_SPEED             = 2f
AUTO_ATTACK_INTERVAL   = 1000ms
AI_TICK_INTERVAL       = 16ms
RESPAWN_DELAY          = 5000ms
RESPAWN_CHECK_INTERVAL = 1000ms
DAMAGE_NUMBER_DURATION = 800ms
PICKUP_RANGE           = 50f
PICKUP_DELAY           = 1500ms
COLLISION_RADIUS       = 10f
LUMINANCE_THRESHOLD    = 80f
PORTAL_RANGE           = 30f
PORTAL_COOLDOWN        = 2000ms
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

## 다음 작업 후보 (우선순위 순)

- [ ] NPC 시스템
- [ ] 직업 선택 화면
- [ ] 퀘스트 시스템 (슬라임 N마리 처치 등)
- [ ] 무기 강화 시스템
- [ ] 장비 창고
- [ ] 애니메이션 (공격 이펙트, 레벨업 연출)
