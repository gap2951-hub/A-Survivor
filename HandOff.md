# A-Survivor HandOff

## 프로젝트 개요
방향키로만 캐릭터를 조작하고, 몬스터가 사방에서 등장하며, 자동으로 전투가 진행되는 생존형 안드로이드 게임.

- **패키지명:** `com.a_survivor.app`
- **언어:** Kotlin + Jetpack Compose
- **minSdk:** 24 / **targetSdk:** 36
- **GitHub:** https://github.com/gap2951-hub/A-Survivor

---

## 게임 기획

### 핵심 루프
- 플레이어는 **가상 D-pad** 로만 이동
- 전투는 **자동** — 사거리 내 가장 가까운 적을 자동 공격
- 좀비가 **사방에서 지속 스폰** (생존 시간에 따라 점점 강해짐)
- 플레이어가 사망하면 런(세션) 종료

### 세션 내 성장
- 적 처치 → EXP 획득 → 레벨업
- 레벨업 시 3개 랜덤 업그레이드 중 1개 선택
  - 공격 속도 / 공격력 / 이동 속도 / 최대 체력 / 공격 범위 / 긴급 회복

### 메타 성장 (런 간 영구 유지)
- 런 종료 후 점수 기반 재화 획득 (미구현 - 추후 상점 예정)
- SharedPreferences 로 영구 저장
- 적용 항목: 기본 공격력 / 최대 체력 / 이동 속도 보너스

---

## 아키텍처

```
com.a_survivor.app/
├── MainActivity.kt
├── data/
│   ├── GameModels.kt        (Vector2, PlayerData, EnemyData, Projectile, GameState)
│   ├── Upgrade.kt
│   └── MetaStats.kt
├── game/
│   ├── GameViewModel.kt     (게임 루프 + 상태 관리, ~60fps)
│   └── systems/
│       ├── CombatSystem.kt      (자동 공격, 투사체, 접촉 피해)
│       ├── EnemySpawner.kt      (웨이브 스폰)
│       ├── ExperienceSystem.kt  (EXP / 레벨업)
│       └── UpgradeSystem.kt     (업그레이드 풀)
├── persistence/
│   └── MetaProgression.kt   (영구 저장/로드)
└── ui/
    ├── GameScreen.kt        (메인 스크린, 레이아웃 조합)
    ├── GameRenderer.kt      (Canvas 렌더링)
    ├── HudOverlay.kt        (HP/EXP 바, 레벨, 점수)
    ├── DPadControl.kt       (가상 조이스틱)
    ├── UpgradeSelectionUI.kt
    └── GameOverScreen.kt
```

---

## 현재 상태
- MVP 전체 구조 구현 완료 (5단계 모두)
- Android Studio 빌드 후 즉시 플레이 가능

---

## 작업 내역

| # | 작업 | 브랜치/커밋 | 상태 |
|---|------|------------|------|
| 1 | 프로젝트 초기 세팅 및 GitHub 연결 | `main` / 4f2d494 | ✅ 완료 |
| 2 | HandOff.md / README.md 추가 | `main` / 41d510f | ✅ 완료 |
| 3 | MVP 전체 게임 구현 (5단계) | `main` | ✅ 완료 |

---

## 다음 작업 목록
- [ ] 메타 상점 UI (재화로 영구 스탯 구매)
- [ ] 스프라이트 / 이미지 에셋 적용 (현재 도형으로 대체)
- [ ] 사운드 이펙트
- [ ] 다양한 적 종류 (Elite, Boss)
- [ ] 무기 다양화 (관통, 범위 공격 등)
- [ ] 스테이지 배경 타일맵

---

## 참고 사항
- 조작은 가상 D-pad (조이스틱 스타일) 만 사용
- 전투는 자동 (플레이어가 공격 버튼 불필요)
- 몬스터는 사방에서 지속적으로 스폰
- 생존 시간 60초마다 적 능력치 1단계씩 강화
