# A-Survivor HandOff

## 프로젝트 개요
방향키로만 캐릭터를 조작하고, 몬스터가 사방에서 등장하며, 자동으로 전투가 진행되는 생존형 안드로이드 게임.

- **패키지명:** `com.a_survivor.app`
- **언어:** Kotlin + Jetpack Compose
- **minSdk:** 24 / **targetSdk:** 36
- **GitHub:** https://github.com/gap2951-hub/A-Survivor

---

## 현재 상태
- Android Studio 빈 프로젝트 생성 완료
- Git 초기화 및 GitHub 원격 저장소 연결 완료
- 게임 로직 없음 — `MainActivity`에 기본 Hello World 상태

---

## 작업 내역

| # | 작업 | 브랜치/커밋 | 상태 |
|---|------|------------|------|
| 1 | 프로젝트 초기 세팅 및 GitHub 연결 | `main` / 4f2d494 | ✅ 완료 |

---

## 다음 작업 목록
- [ ] 게임 화면 구성 (GameView / Canvas)
- [ ] 플레이어 캐릭터 구현 (방향키 조작)
- [ ] 몬스터 생성 및 이동 로직
- [ ] 자동 전투 시스템
- [ ] 충돌 감지 및 HP 시스템
- [ ] 점수 / 생존 시간 UI

---

## 참고 사항
- 조작은 방향키(D-pad)만 사용
- 전투는 자동 (플레이어가 공격 버튼 불필요)
- 몬스터는 사방에서 지속적으로 스폰
