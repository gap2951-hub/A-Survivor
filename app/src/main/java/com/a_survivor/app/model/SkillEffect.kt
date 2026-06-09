package com.a_survivor.app.model

enum class SkillEffectType { SLASH_BURST, EXPLOSION }

data class SkillEffect(
    val id: Int,
    val type: SkillEffectType,
    val worldX: Float,
    val worldY: Float,
    val radius: Float,
    val startedAt: Long
) {
    companion object { const val DURATION = 500L }
    fun isExpired(now: Long) = now - startedAt > DURATION
    fun progress(now: Long) = ((now - startedAt).toFloat() / DURATION).coerceIn(0f, 1f)
}
