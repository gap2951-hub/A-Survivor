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
│   └── Monster.kt                (몬스터 데이터 + slime() 팩토리)
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

## 장비 시스템

### 노가다 목장갑

| 항목 | 초기값 |
|------|--------|
| 공격력 | 0 |
| 최대 업그레이드 횟수 | 5 |
| 남은 업그레이드 횟수 | 5 |
| 실패 횟수 | 0 |
| 파괴 여부 | false |
| 착용 가능 직업 | 전 직업 |

### 낡은 검 (DefaultWeapon)

| 항목 | 값 |
|------|-----|
| 공격력 | 5 |
| 마력 | 0 |
| 무기분류 | 한손검 |
| 공격속도 | 보통 |
| 최대 업그레이드 횟수 | 7 |
| 가위 사용 가능 횟수 | 10 |
| 착용 가능 직업 | 전사 |

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
| `clampPosition(x, y)` | 좌표를 경계 안으로 제한, `Pair<Float, Float>` 반환 |

- `DefaultWorld = GameWorld()` 기본 인스턴스
- `Player.positionX / positionY` 가 월드 좌표계와 직접 호환

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

### 첫 번째 몬스터 — 슬라임

| 항목 | 값 |
|------|-----|
| hp / maxHp | 20 |
| speed | 1f |
| expReward | 5 |

- `slime(id, positionX, positionY)` 팩토리 함수로 생성
- `Monster.distanceTo(x, y)` 확장 함수 제공

### MonsterSpawner

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `count` | — | 생성할 몬스터 수 |
| `minDistance` | `100f` | 몬스터 간 최소 거리 |
| `margin` | `50f` | 맵 가장자리 여백 |
| `random` | `Random.Default` | 테스트 시 시드 고정 가능 |

- 후보 좌표 랜덤 생성 → 기존 몬스터와 거리 비교 → 최대 50회 재시도
- 50회 안에 자리 못 찾으면 해당 몬스터 건너뜀

---

## 가상 조이스틱 시스템

### 동작 구조

```
터치 드래그
  → JoystickControl (방향 벡터 -1~1)
  → 게임 루프 LaunchedEffect(delay 16ms, ~60fps)
  → ViewModel.movePlayer(dirX, dirY)
  → 위치 += 방향 × MOVE_SPEED(3f)
  → GameWorld.clampPosition (월드 경계 제한)
  → Player.positionX/Y 업데이트
```

### JoystickControl Composable

- 화면 **왼쪽 하단** 고정 오버레이
- Canvas 기반: 베이스 원(반투명) + 썸 원(밝은)
- 썸은 베이스 반경 안으로 벡터 clamping
- 드래그 종료 시 썸 중앙 복귀, 방향 (0,0) 전달

---

## 주문서 시스템

### 일반 주문서

| 주문서 | 성공률 | 성공 시 |
|--------|--------|---------|
| 장갑 공격력 100% | 100% | 공격력 +1 |
| 장갑 공격력 60%  | 60%  | 공격력 +2 |
| 장갑 공격력 10%  | 10%  | 공격력 +3 |

- 성공/실패 무관하게 사용 즉시 남은 횟수 -1
- 실패 시 실패 횟수 +1

### 백의 주문서

| 주문서 | 성공률 | 성공 시 | 실패 시 |
|--------|--------|---------|---------|
| 백의 1% | 1% | 실패 횟수 -1, 남은 횟수 +1 | 장비 파괴 |
| 백의 3% | 3% | 실패 횟수 -1, 남은 횟수 +1 | 장비 파괴 |

---

## 전투 스탯 계산 (CombatStatCalculator)

| 직업 | 계산식 |
|------|--------|
| 전사 / 초보자 | 무기공격력 + 장갑공격력 + STR × 0.5 |
| 궁수 / 해적   | 무기공격력 + 장갑공격력 + DEX × 0.5 |
| 도적          | 무기공격력 + 장갑공격력 + LUK × 0.5 |
| 마법사        | 무기마력 + 장갑마력 + INT × 0.5 (장갑 공격력 임시 합산) |

---

## UI 구성

### 장비창

```
①  [모자]
②  [얼굴] [눈장식] [귀걸이]
③  [목걸이]
④  [어깨]  [상의]  [망토]
⑤  [🧤장갑] [하의] [⚔무기]
⑥  [신발]  [벨트]
```

- **활성 슬롯:** 장갑(드래그 드롭 강화), 무기(탭=정보/꾹=해제)
- **잠금 슬롯:** 나머지 10개

### 아이템 정보 다이얼로그 (맵스토리 스타일)

| 섹션 | 내용 |
|------|------|
| ① 헤더 | 아이템명 + "교환 불가" |
| ② 이미지 + 정보 | 96dp 이미지박스 + 세부 정보 |
| ③ 직업 탭 | 6개 직업, 착용 가능 직업 주황 강조 |
| ④ 스탯 | `·` 불릿 형식 |
| ⑤ 설명 | 플레이버 텍스트 |

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
| 5 | 장갑 이미지 적용 | ✅ |
| 6 | 탭=정보 / 꾹=해제·초기화 다이얼로그 | ✅ |
| 7 | Player 모델 추가 | ✅ |
| 8 | PlayerJob enum 추가 (6직업) | ✅ |
| 9 | PlayerStats 모델 추가 | ✅ |
| 10 | CombatStatCalculator 서비스 추가 | ✅ |
| 11 | Weapon 모델 + DefaultWeapon 추가 | ✅ |
| 12 | 장비창 무기 슬롯 활성화, 잠금 처리 | ✅ |
| 13 | 낡은 검 이미지 추가 | ✅ |
| 14 | 아이템 정보창 맵스토리 스타일 통일 | ✅ |
| 15 | GameWorld 모델 추가 (1600×1200) | ✅ |
| 16 | Monster 모델 + slime() 팩토리 추가 | ✅ |
| 17 | MonsterSpawner 서비스 추가 (최소 거리 보장) | ✅ |
| 18 | 가상 조이스틱 + 플레이어 이동 시스템 추가 | ✅ |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성

---

## 다음 작업 후보

- [ ] 게임 화면 분리 (강화 시뮬레이터 ↔ 게임 플레이)
- [ ] 몬스터 렌더링 (월드 좌표 → 화면 좌표 변환)
- [ ] 카메라 시스템 (플레이어 중심 뷰포트)
- [ ] 플레이어 렌더링
- [ ] 몬스터 AI — 플레이어 추적
- [ ] 전투 시스템 (공격 / 피격 / HP 감소)
- [ ] Player ↔ ViewModel 연결 (직업 선택 UI)
- [ ] 스탯 포인트 배분 UI
- [ ] 강화 내역 로그
- [ ] 애니메이션 (성공/실패 이펙트)
