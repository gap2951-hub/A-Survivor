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
├── MainActivity.kt               (전체 UI — Compose)
├── model/
│   ├── Equipment.kt              (장비 데이터 — 불변 data class)
│   ├── Scroll.kt                 (주문서 타입 / ScrollCatalog)
│   ├── EnhancementResult.kt      (강화 결과 sealed class)
│   ├── Player.kt                 (플레이어 데이터 — 불변 data class)
│   ├── PlayerJob.kt              (직업 enum + 직업별 초기 스탯 팩토리)
│   ├── PlayerStats.kt            (스탯 데이터 — str/dex/int/luk)
│   ├── Weapon.kt                 (무기 데이터 + DefaultWeapon "낡은 검")
│   ├── GameWorld.kt              (월드 크기 + 좌표 유틸리티)
│   ├── Monster.kt                (몬스터 데이터 + slime() 팩토리)
│   └── CameraState.kt            (카메라 — 플레이어 추적 / 좌표 변환)
├── service/
│   ├── EnhancementService.kt     (강화 로직 / 확률 계산)
│   ├── CombatStatCalculator.kt   (직업별 공격력/마력 계산)
│   └── MonsterSpawner.kt         (몬스터 랜덤 스폰 / 최소 거리 보장)
├── viewmodel/
│   └── MainViewModel.kt          (UiState, 인벤토리 + 플레이어 상태 관리)
└── res/drawable/
    ├── nogada_glove.png          (노가다 목장갑 픽셀아트 이미지)
    └── nogada_sword.png          (낡은 검 픽셀아트 이미지)
```

---

## 화면 구조

### 기본 화면 (게임 화면)

```
Box (fillMaxSize)
 ├── GameWorldView          ← 어두운 배경 (향후 맵 렌더링 자리)
 ├── GameHud (좌상단)        ← Lv. 직업명 + HP 바
 ├── ResultPanel (상단 중앙) ← 강화 결과 (있을 때만 표시)
 ├── PanelOverlay > EquipmentWindow   ← 장비 버튼 탭 시 오버레이
 ├── PanelOverlay > InventoryWindow   ← 인벤 버튼 탭 시 오버레이
 ├── HudButton ×2 (우하단)  ← 장비 / 인벤 토글 버튼
 ├── JoystickControl (좌하단)
 └── DragGhost (드래그 중)
```

- 장비창/인벤토리는 기본 **닫힌 상태**
- 오버레이 열림 → 스크림 탭 or ✕ 버튼으로 닫기
- 강화 드래그 드롭: 장비창 + 인벤토리 **둘 다 열려 있어야** 인식

---

## 플레이어 시스템

### Player 모델

| 필드 | 타입 | 초기값 |
|------|------|--------|
| level | Int | 1 |
| exp | Int | 0 |
| hp / maxHp | Int | 100 |
| job | PlayerJob | WARRIOR |
| stats | PlayerStats | WARRIOR 초기 스탯 |
| availableStatPoint | Int | 0 |
| weapon | Weapon | DefaultWeapon |
| positionX / positionY | Float | 0f (월드 좌표) |

### 직업(PlayerJob) 및 초기 스탯

| 직업 | 한국명 | STR | DEX | INT | LUK |
|------|--------|-----|-----|-----|-----|
| BEGINNER | 초보자 | 10 | 10 | 10 | 10 |
| WARRIOR  | 전사   | 20 | 5  | 4  | 4  |
| MAGE     | 마법사 | 4  | 4  | 20 | 5  |
| ARCHER   | 궁수   | 5  | 20 | 4  | 4  |
| THIEF    | 도적   | 5  | 10 | 4  | 15 |
| PIRATE   | 해적   | 10 | 16 | 4  | 6  |

---

## 월드 시스템

### GameWorld

| 필드/메서드 | 설명 |
|---|---|
| `width = 1600f` | 월드 가로 크기 |
| `height = 1200f` | 월드 세로 크기 |
| `contains(x, y)` | 좌표가 월드 범위 안인지 확인 |
| `clampPosition(x, y)` | 좌표를 경계 안으로 제한 |

---

## 카메라 시스템

### CameraState

| 필드/메서드 | 설명 |
|---|---|
| `x`, `y` | 카메라 중심의 월드 좌표 |
| `zoom` | 확대 배율 (기본 1f) |
| `toScreenX/Y(worldX, screenW)` | 월드 → 스크린 좌표 변환 |
| `toScreenOffset(worldX, worldY, screenW, screenH)` | 월드 → `Offset` (Canvas 직접 전달) |
| `toWorldX/Y(screenX, screenW)` | 스크린 → 월드 역변환 (터치 입력) |
| `followPlayer(playerX, playerY)` | 카메라를 플레이어 위치로 이동 |
| `clampToWorld(world, screenW, screenH)` | 뷰포트 경계 제한 |

**변환 공식:**
- `screenX = (worldX - cam.x) * zoom + screenWidth / 2`
- `worldX  = (screenX - screenWidth / 2) / zoom + cam.x`

---

## 몬스터 시스템

### Monster 모델

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Int | 인스턴스 식별자 |
| name | String | 몬스터 이름 |
| hp / maxHp | Int | 현재/최대 HP |
| positionX / positionY | Float | 월드 좌표 |
| speed | Float | 이동 속도 |
| expReward | Int | 처치 시 경험치 |

### 첫 번째 몬스터 — 슬라임 (hp=20, speed=1f, expReward=5)

### MonsterSpawner

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `minDistance` | `100f` | 몬스터 간 최소 거리 |
| `margin` | `50f` | 맵 가장자리 여백 |

---

## 가상 조이스틱 시스템

```
터치 드래그 → JoystickControl (방향 벡터 -1~1)
  → LaunchedEffect 게임 루프 (delay 16ms)
  → ViewModel.movePlayer(dirX, dirY)
  → 위치 += 방향 × MOVE_SPEED(3f)
  → GameWorld.clampPosition
  → Player.positionX/Y 업데이트
```

---

## 장비 시스템

### 노가다 목장갑

| 항목 | 값 |
|------|-----|
| 공격력 | 0 (강화로 증가) |
| 최대 업그레이드 횟수 | 5 |
| 착용 가능 직업 | 전 직업 |

### 낡은 검 (DefaultWeapon)

| 항목 | 값 |
|------|-----|
| 공격력 | 5 |
| 무기분류 | 한손검 |
| 최대 업그레이드 횟수 | 7 |
| 착용 가능 직업 | 전사 |

---

## 주문서 시스템

| 주문서 | 성공률 | 성공 시 |
|--------|--------|---------|
| 장갑 공격력 100% | 100% | 공격력 +1 |
| 장갑 공격력 60%  | 60%  | 공격력 +2 |
| 장갑 공격력 10%  | 10%  | 공격력 +3 |
| 백의 1% | 1% | 실패 횟수 -1 / 실패 시 파괴 |
| 백의 3% | 3% | 실패 횟수 -1 / 실패 시 파괴 |

---

## 전투 스탯 계산 (CombatStatCalculator)

| 직업 | 계산식 |
|------|--------|
| 전사 / 초보자 | 무기공격력 + 장갑공격력 + STR × 0.5 |
| 궁수 / 해적   | 무기공격력 + 장갑공격력 + DEX × 0.5 |
| 도적          | 무기공격력 + 장갑공격력 + LUK × 0.5 |
| 마법사        | 무기마력 + 장갑마력 + INT × 0.5 |

---

## 핵심 클래스

### MainViewModel
- `UiState(equipment, weapon, inventory, selectedScrollType, lastResult, player, world)`
- `MOVE_SPEED = 3f`
- `movePlayer(dirX, dirY)` — 방향 벡터 입력, 월드 경계 clamp 후 위치 갱신

---

## 작업 내역

| # | 작업 | 상태 |
|---|------|------|
| 1 | 강화 시스템 로직 구현 | ✅ |
| 2 | 장비창 UI — 신체 형태 슬롯 레이아웃 | ✅ |
| 3 | 인벤토리창 UI — 토글 + 그리드 | ✅ |
| 4 | 드래그 앤 드롭 강화 | ✅ |
| 5 | 장갑 / 낡은 검 이미지 적용 | ✅ |
| 6 | 아이템 정보창 맵스토리 스타일 통일 | ✅ |
| 7 | Player / PlayerJob(6직업) / PlayerStats 모델 추가 | ✅ |
| 8 | CombatStatCalculator 서비스 추가 | ✅ |
| 9 | Weapon 모델 + DefaultWeapon 추가 | ✅ |
| 10 | 장비창 무기 슬롯 활성화, 잠금 처리 | ✅ |
| 11 | GameWorld 모델 추가 (1600×1200) | ✅ |
| 12 | Monster 모델 + MonsterSpawner 추가 | ✅ |
| 13 | 가상 조이스틱 + 플레이어 이동 시스템 추가 | ✅ |
| 14 | 기본 게임 화면 구조 개편 (오버레이 방식) | ✅ |
| 15 | CameraState 모델 추가 (좌표 변환 / 플레이어 추적) | ✅ |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 다음 작업 후보

- [ ] 플레이어 + 몬스터 Canvas 렌더링 (CameraState 활용)
- [ ] 몬스터 AI — 플레이어 추적 이동
- [ ] 전투 시스템 (공격 / 피격 / HP 감소 / 사망)
- [ ] 경험치 획득 및 레벨업
- [ ] 직업 선택 화면
- [ ] 스탯 포인트 배분 UI
- [ ] 강화 내역 로그
- [ ] 애니메이션 (성공/실패 이펙트)
- [ ] 무기 강화 시스템 (장갑과 동일 방식)
