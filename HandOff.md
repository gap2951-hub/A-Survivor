# 메이플스토리 강화 시뮬레이터 HandOff

## 프로젝트 개요

메이플스토리의 주문서 강화 시스템을 참고한 강화 시뮬레이터.
서버/DB/로그인 없이 Android 단일 화면에서 강화 로직을 테스트하는 프로토타입.

- **패키지명:** `com.a_survivor.app`
- **언어:** Kotlin + Jetpack Compose
- **minSdk:** 24 / **targetSdk:** 36
- **GitHub:** https://github.com/gap2951-hub/A-Survivor
- **GameWorld:** 1484×691 (맵 이미지 비율과 동일)

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
│   ├── GameWorld.kt              (1600×1200)
│   ├── Monster.kt                (+ slime() + distanceTo())
│   ├── CameraState.kt            (좌표 변환 / 추적)
│   ├── DropTable.kt              (DropItem + SlimeDropTable)
│   └── GroundItem.kt             (바닥 드랍 아이템)
├── service/
│   ├── EnhancementService.kt
│   ├── CombatStatCalculator.kt
│   ├── MonsterSpawner.kt
│   ├── AutoAttackService.kt      (ATTACK_RANGE = 120f)
│   ├── LevelService.kt
│   └── DropService.kt
├── viewmodel/
│   └── MainViewModel.kt
└── res/drawable/
    ├── nogada_glove.png
    ├── nogada_sword.png
    ├── scroll_100.png
    ├── scroll_60.png
    ├── scroll_10.png
    └── map_field1.png
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
| 1 | `drawWorldBackground` — 맵 이미지(`map_field1.png`) 월드 전체에 렌더링, 외부는 검정 |
| 2 | `drawGroundItem` × N — 바닥 드랍 아이템 (글로우 → 이미지 → 이름 텍스트) |
| 3 | `drawAttackRange` — 공격 범위 원 (반투명 흰색, r=120) |
| 4 | `drawMonster` × N — 그림자 → 몸통 → 눈 → HP바 |
| 5 | `drawPlayer` — 그림자 → 몸통 → 테두리 → 하이라이트 |

### 시각 사양

| 대상 | 색 | 반지름 |
|------|-----|--------|
| 플레이어 | 주황 `#FFAA33` | 25f × zoom |
| 슬라임 | 초록 `#44BB44` | 12f × zoom |
| HP 바 | 빨강/초록 | 몬스터 위 자동 위치 |
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

- `loadBitmap(context, resId, maxSize=256)` — `inSampleSize`로 다운샘플링 후 `ARGB_8888` 로드
- `drawImage(filterQuality = FilterQuality.High)` — 고품질 축소 렌더링

---

## 플레이어 시스템

| 필드 | 초기값 |
|------|--------|
| level / exp | 1 / 0 |
| hp / maxHp | 100 / 100 |
| job | WARRIOR |
| availableStatPoint | 0 |
| weapon | DefaultWeapon |
| positionX / positionY | 742f / 346f (월드 중앙) |

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

- 좌하단 고정, 이동속도 3f, 월드 경계 clamp

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
init → 자동 공격 루프 + 슬라임 5마리 초기 스폰
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
| 9 | GameWorld 모델 | ✅ |
| 10 | Monster + MonsterSpawner | ✅ |
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
| 22 | 플레이어 시작 위치 월드 중앙(800, 600) 설정 | ✅ |
| 23 | 플레이어(25f) / 아이템(52f) 크기 확대 | ✅ |
| 24 | 비트맵 다운샘플링 + FilterQuality.High 적용 | ✅ |
| 25 | 메이플스토리 스타일 사냥터 맵 배경 (map_field1.png) | ✅ |
| 26 | GameWorld 크기를 맵 비율(1484×691)에 맞게 변경 | ✅ |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 다음 작업 후보

- [ ] 몬스터 AI — 플레이어 추적 이동
- [ ] 전투 피격 시스템 (몬스터 → 플레이어 HP 감소)
- [ ] 몬스터 리스폰 시스템
- [ ] 직업 선택 화면
- [ ] 장비 창고 (슬롯 점유 시 드랍 보관)
- [ ] 강화 내역 로그
- [ ] 무기 강화 시스템
- [ ] 애니메이션 (공격 이펙트, 레벨업 연출)
