# 메이플스토리 강화 시뮬레이터 HandOff

## 프로젝트 개요

메이플스토리의 주문서 강화 시스템을 참고한 강화 시뮬레이터.
서버/DB/로그인 없이 Android 단일 화면에서 강화 로직을 테스트하는 프로토타입.

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
│   ├── GameWorld.kt              (1144×2048)
│   ├── Monster.kt                (+ slime() + distanceTo())
│   ├── CameraState.kt            (좌표 변환 / 추적)
│   ├── DropTable.kt              (DropItem + SlimeDropTable)
│   └── GroundItem.kt             (바닥 드랍 아이템)
├── service/
│   ├── EnhancementService.kt
│   ├── CombatStatCalculator.kt
│   ├── MonsterSpawner.kt         (isBlocked 람다 파라미터)
│   ├── AutoAttackService.kt      (ATTACK_RANGE = 120f)
│   ├── LevelService.kt
│   └── DropService.kt
├── viewmodel/
│   └── MainViewModel.kt          (AndroidViewModel — 충돌 비트맵 보유)
└── res/drawable/
    ├── map_beginner.jpg           ← 초보자 사냥터 맵 배경
    ├── slime.png                  ← 슬라임 몬스터 이미지 (※ 흰색 배경 제거 필요)
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

## 렌더링 시스템 (GameCanvas)

### 렌더 순서

| 레이어 | 내용 |
|--------|------|
| 1 | `drawWorldBackground` — `map_beginner.jpg` 이미지를 월드 전체에 렌더링 |
| 2 | `drawGroundItem` × N — 바닥 드랍 아이템 (글로우 → 이미지 → 이름 텍스트) |
| 3 | `drawAttackRange` — 공격 범위 원 (반투명 흰색, r=120) |
| 4 | `drawMonster` × N — 그림자 → 슬라임 이미지(96f×zoom) → HP 바 |
| 5 | `drawPlayer` — 그림자 → 몸통 → 테두리 → 하이라이트 |

### 시각 사양

| 대상 | 표현 방식 | 크기 |
|------|-----------|------|
| 플레이어 | 주황 원 `#FFAA33` | 25f × zoom |
| 슬라임 | PNG 이미지 (slime.png) | 96f × zoom |
| HP 바 | 빨강/초록 rect | 몬스터 이미지 위 자동 위치 |
| 바닥 아이템 | PNG 이미지 | 52f × zoom |

### 카메라

```kotlin
CameraState()
  .followPlayer(player.positionX, player.positionY)
  .clampToWorld(world, screenW, screenH)
```

- 플레이어 항상 화면 중앙 추적
- 월드 경계에서 clamp

### 비트맵 로딩

- `loadBitmap(context, resId, maxSize)` — `inSampleSize`로 다운샘플링 후 `ARGB_8888` 로드
- `drawImage(filterQuality = FilterQuality.High)` — 고품질 축소 렌더링
- 맵 비트맵: `maxSize=2048`, 아이템/슬라임: `maxSize=256`

---

## 맵 배경 시스템

### 월드 / 맵 설정

| 항목 | 값 |
|------|-----|
| 월드 크기 | 1144 × 2048 |
| 원본 맵 이미지 | `map_beginner.jpg` (초보자 사냥터, 픽셀아트) |
| 플레이어 초기 위치 | (572f, 1300f) — 중앙 X, 하단 개활지 |

### 픽셀 충돌 시스템 (MainViewModel)

```kotlin
// AndroidViewModel로 변경 — application.resources로 비트맵 로드
private val collisionBitmap: Bitmap? by lazy {
    BitmapFactory.decodeResource(application.resources, R.drawable.map_beginner,
        BitmapFactory.Options().apply { inSampleSize = 4; inPreferredConfig = ARGB_8888 })
}
```

- `isPixelBlocked(worldX, worldY, world)`: 픽셀 루미넌스 < 130 → 나무/풀숲으로 판정
  - 루미넌스 = 0.299R + 0.587G + 0.114B
- `isBlocked(worldX, worldY, world)`: 중심 + 상하좌우 22f 지점 5곳 중 하나라도 막히면 true
- `movePlayer`: X/Y 축 독립 판정 → 벽 슬라이딩 구현
- `MonsterSpawner.spawnSlimes(isBlocked = { x, y -> isBlocked(x, y, DefaultWorld) })` → 나무 위 스폰 방지

---

## 플레이어 시스템

| 필드 | 초기값 |
|------|--------|
| level / exp | 1 / 0 |
| hp / maxHp | 100 / 100 |
| job | WARRIOR |
| availableStatPoint | 0 |
| weapon | DefaultWeapon |
| positionX / positionY | 572f / 1300f |

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
| 공격 범위 | 120f |
| 공격 주기 | 1초 |
| 타겟 | 범위 내 최근접 몬스터 |
| 데미지 | CombatStatCalculator (최소 1) |

`AutoAttackResult`: `updatedMonsters`, `targetId`, `damage`, `killedMonsters`

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
    val dropItem: DropItem
)
```

- 몬스터 사망 위치에 스폰, 복수 드랍 시 20f 간격 배치
- 캔버스에 아이템 이미지(PNG) + 이름 텍스트 렌더링
- 플레이어가 **PICKUP_RANGE = 50f** 이내 접근 시 자동 습득
- 픽업 체크: `movePlayer()` + `autoAttackTick()` 양쪽에서 실행

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

- 좌하단 고정, 이동속도 3f, 월드 경계 clamp + 픽셀 충돌 clamp

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
        player, world, monsters, groundItems)
MOVE_SPEED = 3f / AUTO_ATTACK_INTERVAL = 1000ms / PICKUP_RANGE = 50f
COLLISION_RADIUS = 22f / LUMINANCE_THRESHOLD = 130f
init → 자동 공격 루프 + 슬라임 5마리 초기 스폰 (isBlocked 적용)
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
| 9 | GameWorld 모델 (1144×2048) | ✅ |
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
| 20 | 자동 픽업 시스템 (PICKUP_RANGE = 50f) | ✅ |
| 21 | 아이템 PNG 이미지 적용 (scroll_100/60/10, nogada_glove) | ✅ |
| 22 | 플레이어 시작 위치 (572f, 1300f) 설정 | ✅ |
| 23 | 플레이어(25f) / 아이템(52f) 크기 확대 | ✅ |
| 24 | 비트맵 다운샘플링 + FilterQuality.High 적용 | ✅ |
| 25 | 초보자 사냥터 맵 배경 (map_beginner.jpg) 적용 | ✅ |
| 26 | 픽셀 루미넌스 기반 충돌 시스템 (나무/풀숲 통과 불가) | ✅ |
| 27 | AndroidViewModel 전환 — Application 컨텍스트로 충돌 비트맵 로드 | ✅ |
| 28 | 벽 슬라이딩 이동 (X/Y 축 독립 충돌 판정) | ✅ |
| 29 | 슬라임 PNG 이미지 적용 (slime.png, 96f×zoom) | ✅ |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 다음 작업 후보

- [ ] slime.png 흰색 배경 제거 후 재적용 (현재 배경 있음)
- [ ] 몬스터 AI — 플레이어 추적 이동
- [ ] 전투 피격 시스템 (몬스터 → 플레이어 HP 감소)
- [ ] 몬스터 리스폰 시스템
- [ ] 직업 선택 화면
- [ ] 장비 창고 (슬롯 점유 시 드랍 보관)
- [ ] 강화 내역 로그
- [ ] 무기 강화 시스템
- [ ] 애니메이션 (공격 이펙트, 레벨업 연출)
