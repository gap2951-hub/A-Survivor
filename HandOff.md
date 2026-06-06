# A-Survivor HandOff

## 프로젝트 개요

메이플스토리 스타일의 픽셀아트 사냥터를 배경으로 한 안드로이드 생존형 게임.
주문서 강화 시스템, 몬스터 AI, 픽셀 충돌 등 핵심 시스템 구현 완료.

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
│   ├── GameWorld.kt              (1024×572)
│   ├── Monster.kt                (+ slime() + distanceTo() + MonsterState)
│   ├── MonsterState.kt           (IDLE / AGGRO / ATTACKING)
│   ├── CameraState.kt            (좌표 변환 / 추적)
│   ├── DropTable.kt              (DropItem + SlimeDropTable)
│   ├── GroundItem.kt             (바닥 드랍 아이템 + droppedAt 타임스탬프)
│   └── DamageNumber.kt           (데미지 숫자 floating 표시)
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
    ├── map_beginner.jpg           ← 초보자 사냥터 맵 배경 (가로 1024×572)
    ├── slime.png                  ← 슬라임 몬스터 이미지
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
 ├── PanelOverlay > StatWindow
 ├── PanelOverlay > EquipmentWindow
 ├── PanelOverlay > InventoryWindow
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
| 1 | `drawWorldBackground` — `map_beginner.jpg` 이미지를 월드 전체에 렌더링 |
| 2 | `drawGroundItem` × N — 바닥 드랍 아이템 (글로우 → 이미지 → 이름 텍스트) |
| 3 | `drawAttackRange` — 공격 범위 원 (반투명 흰색, r=60) |
| 4 | `drawMonster` × N — 그림자 → 슬라임 이미지 → HP 바 → 어그로 "!" |
| 5 | `drawPlayer` — 그림자 → 몸통 → 테두리 → 하이라이트 |
| 6 | `drawDamageNumber` × N — 데미지 숫자 (노랑: 플→몬, 빨강: 몬→플) |

### 시각 사양 (화면 비례 크기)

| 대상 | 표현 방식 | 크기 |
|------|-----------|------|
| 플레이어 | 주황 원 `#FFAA33` | `size.height * 0.026f` |
| 슬라임 (IDLE) | PNG 이미지 + 초록 HP 바 | `size.height * 0.088f` |
| 슬라임 (AGGRO/ATTACKING) | PNG 이미지 + 주황 HP 바 + 빨간 "!" | `size.height * 0.088f` |
| 바닥 아이템 | PNG 이미지 + 이름 텍스트 | `size.height * 0.048f` |
| 데미지 숫자 | 노랑 (플→몬) / 빨강 (몬→플) | `20f or 17f * (size.height/1080f)` |

> 스프라이트 크기는 `cam.zoom` 기반이 아닌 `size.height` 비례로 설정 (zoom 변화 시 크기 불변)

### 카메라

```kotlin
// 화면을 꽉 채우는 동적 줌 계산
val zoom = maxOf(size.width / world.width, size.height / world.height)
val cam = CameraState(zoom = zoom)
    .followPlayer(player.positionX, player.positionY)
    .clampToWorld(world, size.width, size.height)
```

- 플레이어 항상 화면 중앙 추적
- 월드 경계에서 clamp
- 줌은 화면/월드 비율로 자동 계산 (다크 바 없음)

### 비트맵 로딩

- `loadBitmap(context, resId, maxSize)` — `inSampleSize`로 다운샘플링 후 `ARGB_8888` 로드
- `drawImage(filterQuality = FilterQuality.High)` — 고품질 축소 렌더링

---

## 데미지 숫자 시스템

```kotlin
data class DamageNumber(
    val id: Int, val value: Int,
    val worldX: Float, val worldY: Float,
    val createdAt: Long,
    val isPlayerDamage: Boolean   // true = 몬스터→플레이어(빨강), false = 플레이어→몬스터(노랑)
)
```

- 800ms 동안 위로 55 월드 유닛 float + 페이드아웃
- `autoAttackTick`: 노랑 데미지 숫자 생성 (몬스터 위치)
- `monsterAiTick`: 빨강 데미지 숫자 생성 (플레이어 위치) + 만료 정리

---

## 맵 배경 시스템

### 월드 / 맵 설정

| 항목 | 값 |
|------|-----|
| 월드 크기 | 1024 × 572 |
| 원본 맵 이미지 | `map_beginner.jpg` (가로형 초보자 사냥터) |
| 플레이어 초기 위치 | (512f, 286f) — 월드 중앙 |

### 픽셀 충돌 시스템 (MainViewModel)

```kotlin
// AndroidViewModel — application.resources로 1/4 해상도 충돌 비트맵 로드
private val collisionBitmap: Bitmap? by lazy {
    BitmapFactory.decodeResource(application.resources, R.drawable.map_beginner,
        BitmapFactory.Options().apply { inSampleSize = 4; inPreferredConfig = ARGB_8888 })
}
```

- `isPixelBlocked(worldX, worldY, world)`: 픽셀 루미넌스 < **80** → 장애물로 판정
  - 루미넌스 = 0.299R + 0.587G + 0.114B
  - 임계값 80: 나무 트렁크(lum 16~77) 차단 / 잔디(lum 147) + 섹션 연결로(lum 86~128) 통과
- `isBlocked(worldX, worldY, world)`: 중심 + 상하좌우 10f 지점 5곳 중 하나라도 막히면 true
- `movePlayer`: X/Y 축 독립 판정 → 벽 슬라이딩 구현
- `MonsterSpawner.spawnSlimes(isBlocked = ...)` → 나무 위 스폰 방지
- `MonsterAiService.tick(isBlocked = ...)` → 몬스터도 나무 통과 불가

---

## 플레이어 시스템

| 필드 | 초기값 |
|------|--------|
| level / exp | 1 / 0 |
| hp / maxHp | 100 / 100 |
| job | WARRIOR |
| availableStatPoint | 0 |
| weapon | DefaultWeapon |
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

## 스탯 시스템

- `StatType`: STR / DEX / INT / LUK
- `allocateStat(type)`: SP 1 소모 → 해당 스탯 +1 → 다음 공격 틱부터 자동 반영
- **StatWindow** (우하단 "스탯" 버튼): 남은 SP + 스탯별 [+] 버튼

---

## 레벨 시스템

- 필요 EXP: `level × 20`
- 레벨업: SP +5
- 슬라임 처치 EXP: 5

---

## 자동 공격 (AutoAttackService)

| 항목 | 값 |
|------|-----|
| 공격 범위 | 60f (전사 근접 기준) |
| 공격 주기 | 1초 |
| 타겟 | 범위 내 최근접 몬스터 |
| 데미지 | CombatStatCalculator (최소 1) |

`AutoAttackResult`: `updatedMonsters`, `targetId`, `damage`, `killedMonsters`

---

## 몬스터 AI 시스템 (MonsterAiService)

### MonsterState

| 상태 | 설명 |
|------|------|
| IDLE | 기본 상태 — 아무 행동 없음 |
| AGGRO | 플레이어 추적 중 |
| ATTACKING | 공격 범위 내 공격 중 |

### 상태 전환 규칙

- **IDLE → AGGRO**: 플레이어 자동 공격에 피격 시
- **AGGRO → ATTACKING**: 플레이어와 거리 ≤ 35f
- **ATTACKING → AGGRO**: 플레이어와 거리 > 35f
- **AGGRO/ATTACKING → IDLE**: 플레이어와 거리 > 500f (어그로 해제)

### AI 수치

| 항목 | 값 |
|------|-----|
| 몬스터 이동 속도 | 1.2f / tick |
| AI 틱 간격 | 16ms |
| 공격 범위 | 35f |
| 공격 주기 | 1000ms |
| 공격 데미지 | 5 |
| 어그로 해제 범위 | 500f |

- 몬스터 이동 시 `isBlocked()` 동일 적용 (나무/풀숲 통과 불가, 벽 슬라이딩)

---

## 몬스터 리스폰 시스템

- `PendingRespawn(monsterId: Int, diedAt: Long)` — 처치된 몬스터 대기열 항목
- `pendingRespawns: List<PendingRespawn>` — UiState에 포함
- `autoAttackTick`: 몬스터 처치 시 `PendingRespawn(id, now)` 추가
- `respawnTick` (1초 간격): `now - diedAt >= 5000ms` 조건 충족 시 `MonsterSpawner.spawnSlimes(count=1)` 호출 → 유효 위치에 재스폰
- `nextMonsterId`: 초기 스폰 최대 ID + 1부터 시작, 재스폰마다 증가

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

### GroundItem (바닥 드랍)

```kotlin
data class GroundItem(
    val id: Int,
    val positionX: Float,
    val positionY: Float,
    val dropItem: DropItem,
    val droppedAt: Long = 0L    // 즉시 픽업 방지용 타임스탬프
)
```

- 몬스터 사망 위치에 스폰, 복수 드랍 시 20f 간격 배치
- 캔버스에 아이템 이미지(PNG) + 이름 텍스트 렌더링
- 드랍 후 **PICKUP_DELAY = 1500ms** 경과 후부터 픽업 가능
- 플레이어가 **PICKUP_RANGE = 50f** 이내 접근 시 자동 습득
- 픽업 체크: `movePlayer()` + `monsterAiTick()` 양쪽에서 실행

### 인벤토리 초기 수량

주문서 수량 초기값 **0** (사냥으로만 획득)

---

## 전투 스탯 계산

| 직업 | 계산식 |
|------|--------|
| 전사 / 초보자 | 무기공격력 + 장갑공격력 + STR × 0.5 |
| 궁수 / 해적   | 무기공격력 + 장갑공격력 + DEX × 0.5 |
| 도적          | 무기공격력 + 장갑공격력 + LUK × 0.5 |
| 마법사        | 무기마력 + 장갑마력 + INT × 0.5 |

---

## 가상 조이스틱

- 좌하단 고정, 이동속도 2f, 월드 경계 clamp + 픽셀 충돌 clamp

---

## 주문서 강화 시스템

| 주문서 | 성공률 | 성공 시 |
|--------|--------|---------|
| 100% | 100% | 공격력 +1 |
| 60%  | 60%  | 공격력 +2 |
| 10%  | 10%  | 공격력 +3 |
| 백의 1%/3% | 1%/3% | 실패 횟수 -1 / 실패 시 파괴 |

---

## MainViewModel — UiState

```kotlin
UiState(equipment, weapon, inventory, selectedScrollType, lastResult,
        player, world, monsters, groundItems, pendingRespawns, damageNumbers)

MOVE_SPEED             = 2f
AUTO_ATTACK_INTERVAL   = 1000ms
AI_TICK_INTERVAL       = 16ms
RESPAWN_DELAY          = 5000ms         // 처치 후 5초 뒤 리스폰
RESPAWN_CHECK_INTERVAL = 1000ms
DAMAGE_NUMBER_DURATION = 800ms
PICKUP_RANGE           = 50f
PICKUP_DELAY           = 1500ms         // 드랍 후 픽업 가능까지 딜레이
COLLISION_RADIUS       = 10f
LUMINANCE_THRESHOLD    = 80f            // 나무(lum<77) 차단, 잔디/연결로(lum≥86) 통과

init → 자동 공격 루프(1s) + 몬스터 AI 루프(16ms) + 리스폰 체크 루프(1s) + 슬라임 5마리 초기 스폰
```

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
| 9 | GameWorld 모델 (1024×572 가로형) | ✅ |
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
| 21 | 아이템 PNG 이미지 적용 (scroll_100/60/10, nogada_glove) | ✅ |
| 22 | 플레이어 시작 위치 (512f, 286f) — 월드 중앙 | ✅ |
| 23 | 스프라이트 크기 화면 비례 방식으로 전환 (size.height × 비율) | ✅ |
| 24 | 비트맵 다운샘플링 + FilterQuality.High 적용 | ✅ |
| 25 | 초보자 사냥터 맵 배경 (map_beginner.jpg, 1024×572 가로형) | ✅ |
| 26 | 픽셀 루미넌스 기반 충돌 시스템 (LUMINANCE_THRESHOLD=80) | ✅ |
| 27 | AndroidViewModel 전환 — Application 컨텍스트로 충돌 비트맵 로드 | ✅ |
| 28 | 벽 슬라이딩 이동 (X/Y 축 독립 충돌 판정) | ✅ |
| 29 | 슬라임 PNG 이미지 적용 (slime.png) | ✅ |
| 30 | MonsterState enum (IDLE / AGGRO / ATTACKING) | ✅ |
| 31 | Monster 모델에 state / lastAttackTime 추가 | ✅ |
| 32 | MonsterAiService — 추적 이동 / 공격 / 어그로 해제 | ✅ |
| 33 | autoAttackTick — 피격 시 AGGRO 전환 연동 | ✅ |
| 34 | monsterAiTick 루프 (16ms) — 플레이어 HP 감소 처리 | ✅ |
| 35 | AGGRO 상태 시각 표시 — 주황 HP바 + "!" | ✅ |
| 36 | 몬스터 리스폰 시스템 — 처치 후 5초 뒤 유효 위치에 재스폰 | ✅ |
| 37 | 데미지 숫자 표시 — 노랑(플→몬) / 빨강(몬→플) + fade-out | ✅ |
| 38 | 가로 화면 + 전체화면 몰입 모드 (sensorLandscape) | ✅ |
| 39 | 동적 줌 계산 — maxOf(screenW/worldW, screenH/worldH) | ✅ |
| 40 | 전사 공격 범위 조정 — ATTACK_RANGE 120f → 60f (근접 전투감) | ✅ |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 다음 작업 후보 (우선순위 순)

- [ ] 마을 맵 추가 (image/마을.jpg 이미지 존재)
- [ ] 포탈 시스템
- [ ] NPC 시스템
- [ ] 직업 선택 화면
- [ ] 무기 강화 시스템
- [ ] 장비 창고 (슬롯 점유 시 드랍 보관)
- [ ] 퀘스트 시스템
- [ ] 애니메이션 (공격 이펙트, 레벨업 연출)
