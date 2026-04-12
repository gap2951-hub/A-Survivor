package com.a_survivor.app.data

data class Upgrade(
    val id: String,
    val name: String,
    val description: String,
    val effect: (PlayerData) -> PlayerData
)
