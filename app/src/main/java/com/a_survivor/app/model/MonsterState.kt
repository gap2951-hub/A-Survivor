package com.a_survivor.app.model

enum class MonsterState {
    IDLE,       // 기본 상태 — 아무 행동 없음
    AGGRO,      // 플레이어 추적 중
    ATTACKING   // 플레이어 공격 범위 내 공격 중
}
