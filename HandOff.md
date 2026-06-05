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
│   ├── Equipment.kt              (장비 — 불변 data class)
│   ├── Scroll.kt                 (주문서 타입 / ScrollCatalog)
│   ├── EnhancementResult.kt      (강화 결과 sealed class)
│   ├── Player.kt                 (플레이어 — 불변 data class)
│   ├── PlayerJob.kt              (직업 enum + 초기 스탯 팩토리)
│   ├── PlayerStats.kt            (str / dex / int / luk)
│   ├── Weapon.kt                 (무기 + DefaultWeapon "낡은 검")
│   ├── GameWorld.kt              (월드 크기 + 좌표 유틸리티)
│   ├── Monster.kt                (몬스터 + slime() 팩토리 + distanceTo())
│   ├── CameraState.kt            (카메라 — 추적 / 좌표 변환)
│   └── DropTable.kt              (DropItem sealed class + SlimeDropTable)
├── service/
│   ├── EnhancementService.kt     (강화 확률 계산)
│   ├── CombatStatCalculator.kt   (직업별 공격력/마력 계산)
│   ├── MonsterSpawner.kt         (랜덤 스폰 / 최소 거리 보장)
│   ├── AutoAttackService.kt      (자동 공격 / 몬스터 HP 감소)
│   ├── LevelService.kt           (경험치 적용 / 레벨업 / SP 지급)
│   └── DropService.kt            (드랍 확률 롤링)
├── viewmodel/
│   └── MainViewModel.kt
└── res/drawable/
    ├── nogada_glove.png
    └── nogada_sword.png
```

---

## 화면 구조

```
Box (게임 화면 기본)
 ├── GameWorldView          ← 배경 (향후 맵 렌더링 자리)
 ├── GameHud (좌상단)        ← Lv. 직업명 + HP 바
 ├── ResultPanel (상단 중앙) ← 강화 결과 (있을 때만)
 ├── PanelOverlay > EquipmentWindow  ← 장비 버튼으로 토글
 ├── PanelOverlay > InventoryWindow  ← 인벤 버튼으로 토글
 ├── HudButton ×2 (우하단)  ← 장비 / 인벤
 ├── JoystickControl (좌하단)
 └── DragGhost
```

- 오버레이: 스크림 탭 or ✕ 버튼으로 닫기
- 강화 드래그: 장비창 + 인벤토리 둘 다 열려 있어야 인식

---

## 플레이어 시스템

### Player 모델

| 필드 | 초기값 |
|------|--------|
| level / exp | 1 / 0 |
| hp / maxHp | 100 / 100 |
| job | WARRIOR |
| stats | WARRIOR 초기 스탯 |
| availableStatPoint | 0 |
| weapon | DefaultWeapon |
| positionX / positionY | 0f / 0f |

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

## 월드 / 카메라 시스템

### GameWorld (1600 × 1200)

- `contains(x, y)`, `clampPosition(x, y)`

### CameraState

| 메서드 | 설명 |
|---|---|
| `toScreenOffset(worldX, worldY, w, h)` | 월드 → Offset (Canvas 직접 전달) |
| `toWorldX/Y(screenX, w)` | 스크린 → 월드 역변환 |
| `followPlayer(x, y)` | 플레이어 중심으로 이동 |
| `clampToWorld(world, w, h)` | 뷰포트 경계 제한 |

---

## 몬스터 / 스폰 시스템

### Monster (슬라임)

| 항목 | 값 |
|------|-----|
| hp / maxHp | 20 |
| speed | 1f |
| expReward | 5 |

### MonsterSpawner

- `spawnSlimes(world, count, minDistance=100f, margin=50f)`
- 최대 50회 재시도로 최소 거리 보장

---

## 자동 공격 시스템 (AutoAttackService)

| 항목 | 값 |
|------|-----|
| 공격 범위 | 120f |
| 공격 주기 | 1초 (viewModelScope 루프) |
| 타겟 선정 | 범위 내 가장 가까운 몬스터 |
| 데미지 | CombatStatCalculator 결과 (최소 1) |

```
AutoAttackResult
 ├── updatedMonsters   : HP 갱신된 생존 몬스터 목록
 ├── targetId          : 공격 대상 ID
 ├── damage            : 가한 데미지
 └── killedMonsters    : 이번 틱에 처치된 몬스터 목록
```

---

## 레벨 시스템 (LevelService)

- 필요 경험치: `level * 20`
- 레벨업 1회: SP +5 (`availableStatPoint += 5`)
- 연속 레벨업 지원 (while 루프)

| 레벨 | 필요 EXP | 슬라임 처치 수 |
|------|----------|--------------|
| 1 → 2 | 20 | 4 |
| 2 → 3 | 40 | 8 |
| 3 → 4 | 60 | 12 |

---

## 드랍 시스템 (DropService + DropTable)

### DropItem sealed class

```
DropItem
 ├── ScrollDrop(scrollType: ScrollType)
 └── EquipmentDrop(equipment: Equipment)
```

### SlimeDropTable

| 아이템 | 확률 |
|--------|------|
| 노가다 목장갑 | 5% |
| 장갑 공격력 100% | 20% |
| 장갑 공격력 60% | 10% |
| 장갑 공격력 10% | 3% |
| 백의 주문서 1% | 1% |

- 각 항목은 **독립 확률 판정** (중복 드랍 가능)
- `ScrollDrop` → 인벤토리 수량 +1
- `EquipmentDrop` → 장비 슬롯 비어 있으면 즉시 장착, 차 있으면 무시

---

## 전투 스탯 계산 (CombatStatCalculator)

| 직업 | 계산식 |
|------|--------|
| 전사 / 초보자 | 무기공격력 + 장갑공격력 + STR × 0.5 |
| 궁수 / 해적   | 무기공격력 + 장갑공격력 + DEX × 0.5 |
| 도적          | 무기공격력 + 장갑공격력 + LUK × 0.5 |
| 마법사        | 무기마력 + 장갑마력 + INT × 0.5 |

---

## 가상 조이스틱

```
JoystickControl → 방향 벡터 (-1~1)
  → 게임 루프 delay(16ms)
  → movePlayer(dirX, dirY) × MOVE_SPEED(3f)
  → GameWorld.clampPosition
```

---

## 주문서 강화 시스템

| 주문서 | 성공률 | 성공 시 |
|--------|--------|---------|
| 장갑 공격력 100% | 100% | 공격력 +1 |
| 장갑 공격력 60%  | 60%  | 공격력 +2 |
| 장갑 공격력 10%  | 10%  | 공격력 +3 |
| 백의 1% | 1% | 실패 횟수 -1 / 실패 시 장비 파괴 |
| 백의 3% | 3% | 실패 횟수 -1 / 실패 시 장비 파괴 |

---

## MainViewModel — UiState

```kotlin
UiState(
    equipment        : Equipment?,
    weapon           : Weapon?,
    inventory        : List<InventoryItem>,
    selectedScrollType,
    lastResult,
    player           : Player,
    world            : GameWorld,
    monsters         : List<Monster>
)
```

- `MOVE_SPEED = 3f`, `AUTO_ATTACK_INTERVAL = 1000ms`
- `init` 블록: 자동 공격 루프 + 슬라임 5마리 초기 스폰

---

## 작업 내역

| # | 작업 | 상태 |
|---|------|------|
| 1 | 강화 시스템 로직 구현 | ✅ |
| 2 | 장비창 UI — 슬롯 레이아웃 | ✅ |
| 3 | 인벤토리창 UI | ✅ |
| 4 | 드래그 앤 드롭 강화 | ✅ |
| 5 | 장갑 / 낡은 검 이미지 + 아이템 정보창 | ✅ |
| 6 | Player / PlayerJob / PlayerStats 모델 | ✅ |
| 7 | CombatStatCalculator 서비스 | ✅ |
| 8 | Weapon 모델 + DefaultWeapon | ✅ |
| 9 | GameWorld 모델 | ✅ |
| 10 | Monster 모델 + MonsterSpawner | ✅ |
| 11 | 가상 조이스틱 + 플레이어 이동 | ✅ |
| 12 | 기본 게임 화면 구조 개편 (오버레이 방식) | ✅ |
| 13 | CameraState — 좌표 변환 / 추적 | ✅ |
| 14 | AutoAttackService — 자동 공격 | ✅ |
| 15 | LevelService — 경험치 / 레벨업 / SP | ✅ |
| 16 | DropService + DropTable — 드랍 시스템 | ✅ |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 다음 작업 후보

- [ ] 플레이어 + 몬스터 Canvas 렌더링 (CameraState 활용)
- [ ] 몬스터 AI — 플레이어 추적 이동
- [ ] 전투 피격 시스템 (몬스터 → 플레이어 HP 감소)
- [ ] 직업 선택 화면
- [ ] 스탯 포인트 배분 UI
- [ ] 장비 창고 (EquipmentDrop 슬롯 점유 시 보관)
- [ ] 강화 내역 로그
- [ ] 무기 강화 시스템
- [ ] 애니메이션 (공격 이펙트, 레벨업 연출)
