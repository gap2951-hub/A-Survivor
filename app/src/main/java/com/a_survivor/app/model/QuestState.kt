package com.a_survivor.app.model

enum class QuestStatus { NOT_STARTED, IN_PROGRESS, READY_TO_COMPLETE, COMPLETED }

enum class TutorialStep {
    LEARN_MOVEMENT,
    TALK_TO_CHUCHU,
    EXPLORE_TOWN,
    USE_PORTAL,
    LEARN_TAP_ATTACK,
    LEARN_AUTO_SWITCH,
    LEARN_MANUAL_SWITCH,
    KILL_MONSTER,
    PICKUP_ITEM,
    OPEN_INVENTORY,
    EQUIP_ITEM,
    RETURN_TO_TOWN,
    COMPLETED
}

data class QuestState(
    val status: QuestStatus = QuestStatus.NOT_STARTED,
    val killCount: Int = 0,
    val killGoal: Int = 5,
    val tutorialStep: TutorialStep = TutorialStep.TALK_TO_CHUCHU,
    val tutorialTravelDistance: Float = 0f,
    // 메인 퀘스트 체인 (MQ-001 ~ MQ-013)
    val mainQuestId: String = "",
    val mainQuestProgress: Int = 0,
    val mainQuestStatus: QuestStatus = QuestStatus.NOT_STARTED,
)
