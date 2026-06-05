package com.a_survivor.app.model

data class Weapon(
    val name: String,
    val attackPower: Int,
    val magicPower: Int,
    val strBonus: Int = 0,
    val reqLevel: Int = 1,
    val reqStr: Int = 0,
    val reqDex: Int = 0,
    val reqInt: Int = 0,
    val reqLuk: Int = 0,
    val weaponType: String = "한손검",
    val attackSpeed: String = "보통",
    val availableJobs: List<PlayerJob> = listOf(PlayerJob.WARRIOR),
    val maxUpgradeCount: Int = 7,
    val scissorCount: Int = 10,
    val description: String = ""
)

val DefaultWeapon = Weapon(
    name = "낡은 검",
    attackPower = 5,
    magicPower = 0,
    strBonus = 0,
    reqLevel = 1,
    reqStr = 0,
    reqDex = 0,
    reqInt = 0,
    reqLuk = 0,
    weaponType = "한손검",
    attackSpeed = "보통",
    availableJobs = listOf(PlayerJob.WARRIOR),
    maxUpgradeCount = 7,
    scissorCount = 10,
    description = "오래되어 여기저기 닿고 녹이 슨 검이다.\n그래도 아직은 사용할 수 있을 것 같다."
)
