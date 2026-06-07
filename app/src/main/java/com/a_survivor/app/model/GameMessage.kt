package com.a_survivor.app.model

enum class MessageType { EXP, MONEY, ITEM }

data class GameMessage(
    val id: Long,
    val text: String,
    val type: MessageType
)
