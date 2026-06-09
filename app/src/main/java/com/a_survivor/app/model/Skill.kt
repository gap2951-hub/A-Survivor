package com.a_survivor.app.model

enum class SkillType { MELEE_BURST, AOE, MULTI_SHOT }

data class Skill(
    val id: String,
    val name: String,
    val type: SkillType,
    val cooldownMs: Long,
    val range: Float,
    val damageMultiplier: Float,
    val aoeRadius: Float = 0f,
    val shotCount: Int = 1
)

object SkillRegistry {
    fun skillFor(job: PlayerJob): Skill = when (job) {
        PlayerJob.WARRIOR  -> Skill("warrior_skill",  "강타",    SkillType.MELEE_BURST, 8000L, 70f,  3.0f)
        PlayerJob.MAGE     -> Skill("mage_skill",     "파이어볼", SkillType.AOE,         6000L, 150f, 3.0f, aoeRadius = 80f)
        PlayerJob.ARCHER   -> Skill("archer_skill",   "연사",    SkillType.MULTI_SHOT,  5000L, 220f, 2.0f, shotCount = 3)
        PlayerJob.THIEF    -> Skill("thief_skill",    "연속표창", SkillType.MULTI_SHOT,  4000L, 180f, 1.5f, shotCount = 3)
        PlayerJob.PIRATE   -> Skill("pirate_skill",   "폭발탄",  SkillType.AOE,         7000L, 190f, 2.5f, aoeRadius = 70f)
        PlayerJob.BEGINNER -> Skill("beginner_skill", "기합",    SkillType.MELEE_BURST, 5000L, 60f,  1.5f)
    }
}
