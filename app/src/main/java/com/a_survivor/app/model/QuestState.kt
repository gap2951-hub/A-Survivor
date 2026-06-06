package com.a_survivor.app.model

enum class QuestStatus { NOT_STARTED, IN_PROGRESS, READY_TO_COMPLETE, COMPLETED }

data class QuestState(
    val status: QuestStatus = QuestStatus.NOT_STARTED,
    val slimeKillCount: Int = 0,
    val slimeKillGoal: Int = 5
)
