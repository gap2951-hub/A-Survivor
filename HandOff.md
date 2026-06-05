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
│   └── Weapon.kt                 (무기 데이터 + DefaultWeapon "낡은 검")
├── service/
│   ├── EnhancementService.kt     (강화 로직 / 확률 계산)
│   └── CombatStatCalculator.kt   (직업별 공격력/마력 계산)
├── viewmodel/
│   └── MainViewModel.kt          (UiState, 인벤토리 상태 관리)
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
| 요구 레벨 | 1 |

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
| positionX / positionY | Float | 0f |

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

- 사용 조건: 실패 횟수 ≥ 1

---

## 전투 스탯 계산 (CombatStatCalculator)

| 직업 | 계산식 |
|------|--------|
| 전사 / 초보자 | 무기공격력 + 장갑공격력 + STR × 0.5 |
| 궁수 / 해적   | 무기공격력 + 장갑공격력 + DEX × 0.5 |
| 도적          | 무기공격력 + 장갑공격력 + LUK × 0.5 |
| 마법사        | 무기마력 + 장갑마력 + INT × 0.5 (장갑 공격력 임시 합산) |

- `EquipmentStatProvider` 인터페이스로 장비 수치 추상화 → 무기/마력 장갑 추가 시 확장 용이

---

## 인벤토리 초기 구성

| 아이템 | 수량 |
|--------|------|
| 장갑 공격력 주문서 100% | 10 |
| 장갑 공격력 주문서 60%  | 10 |
| 장갑 공격력 주문서 10%  | 10 |
| 백의 주문서 1% | 10 |
| 백의 주문서 3% | 10 |

---

## UI 구성

### 장비창

사람 신체 형태 기반 슬롯 배치 (위→아래):

```
①  [모자]
②  [얼굴] [눈장식] [귀걸이]
③  [목걸이]
④  [어깨]  [상의]  [망토]
⑤  [🧤장갑] [하의] [⚔무기]    ← 장갑 = 강화 드롭 타겟 / 무기 = 낡은 검
⑥  [신발]  [벨트]
```

- **활성 슬롯:** 장갑(드래그 드롭 강화), 무기(탭=정보/꾹=해제)
- **잠금 슬롯:** 나머지 10개 — 어두운 배경 + "잠금" 레이블

### 아이템 정보 다이얼로그 (맵스토리 스타일)

두 아이템 모두 동일한 5섹션 구조:

| 섹션 | 내용 |
|------|------|
| ① 헤더 | 아이템명 (흰색 굵게) + "교환 불가" (주황) |
| ② 이미지 + 정보 | 96dp 이미지박스 + 우측 세부 정보 |
| ③ 직업 탭 | 초보자/전사/마법사/궁수/도적/해적 — 착용 가능 직업 주황 강조 |
| ④ 스탯 | `·` 불릿 형식, 중요 수치 주황 |
| ⑤ 설명 | 플레이버 텍스트 |

- 색상 테마: 쿨 다크 블루-그레이 (`TipBg = #262630`)

### 인벤토리창

- "인벤토리 열기/닫기" 버튼으로 토글
- 주문서 4열 그리드 표시
- **꾹 누르고 드래그** → 장갑 슬롯 위에서 손 떼면 강화 실행
- 드래그 중 고스트 이미지가 손가락 따라 이동
- 장갑 슬롯 위에 올라오면 파란 하이라이트

### 결과 메시지

| 결과 | 색상 |
|------|------|
| 성공 | 초록 |
| 실패 | 빨강 |
| 장비 파괴 | 분홍 |
| 오류/불가 | 주황 |

---

## 핵심 클래스

### EnhancementService
- `applyScroll(equipment, scroll): Pair<Equipment, EnhancementResult>`
- 일반 주문서 / 백의 주문서 로직 분리

### CombatStatCalculator
- `calculate(job, stats, equipment): CombatStats`
- `EquipmentStatProvider` 인터페이스로 장비 수치 추상화
- `CombatStats(attackPower, magicPower)` 반환

### MainViewModel
- `UiState(equipment, weapon, inventory, selectedScrollType, lastResult)`
- `selectScroll()` / `useSelectedScroll()` / `unequipEquipment()` / `resetEquipment()`
- `unequipWeapon()` / `resetWeapon()`

### DragDropState
- `scrollType: ScrollType?` / `position: Offset`
- `isDragging` 파생 프로퍼티

---

## 현재 상태

- 강화 시스템 로직 완성 (성공/실패/파괴 모두 동작)
- 장비창 + 인벤토리창 UI 구현 완료
- 드래그 앤 드롭 강화 동작
- 탭/꾹 누르기 인터랙션 구현
- 플레이어 / 직업 / 스탯 모델 완성 (6직업)
- 무기 시스템 완성 (낡은 검 기본 장착)
- 전투 스탯 계산 서비스 완성
- 아이템 정보창 맵스토리 스타일로 통일

---

## 작업 내역

| # | 작업 | 상태 |
|---|------|------|
| 1 | 강화 시스템 로직 구현 (Service / ViewModel / Model) | ✅ 완료 |
| 2 | 장비창 UI — 신체 형태 슬롯 레이아웃 | ✅ 완료 |
| 3 | 인벤토리창 UI — 토글 + 그리드 | ✅ 완료 |
| 4 | 드래그 앤 드롭 강화 | ✅ 완료 |
| 5 | 장갑 이미지 적용 (nogada_glove.png) | ✅ 완료 |
| 6 | 탭=정보 다이얼로그, 꾹누르기=해제/초기화 다이얼로그 | ✅ 완료 |
| 7 | Player 모델 추가 (level/exp/hp/stats/position) | ✅ 완료 |
| 8 | PlayerJob enum 추가 (6직업 + 직업별 초기 스탯) | ✅ 완료 |
| 9 | PlayerStats 모델 추가 | ✅ 완료 |
| 10 | CombatStatCalculator 서비스 추가 | ✅ 완료 |
| 11 | Weapon 모델 + DefaultWeapon "낡은 검" 추가 | ✅ 완료 |
| 12 | 장비창 무기 슬롯 활성화, 나머지 잠금 처리 | ✅ 완료 |
| 13 | 낡은 검 이미지 (nogada_sword.png) 추가 | ✅ 완료 |
| 14 | 아이템 정보창 맵스토리 스타일 UI로 통일 | ✅ 완료 |

---

## 컨벤션

- **커밋 메시지:** 제목·본문 모두 **한글**로 작성
  - 예: `feat: 강화 시스템 구현`, `fix: 드래그 좌표 오류 수정`

---

## 다음 작업 후보

- [ ] 다른 장비 슬롯 활성화 (모자, 무기 등)
- [ ] 주문서 종류 추가
- [ ] 강화 내역 로그 표시
- [ ] 애니메이션 (성공/실패 이펙트)
- [ ] 장비 교체 시스템
- [ ] Player를 ViewModel에 연결 (직업 선택 UI)
- [ ] 전투 화면 구현 (CombatStatCalculator 활용)
- [ ] 스탯 포인트 배분 UI
