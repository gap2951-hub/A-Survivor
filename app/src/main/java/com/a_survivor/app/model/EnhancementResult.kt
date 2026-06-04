package com.a_survivor.app.model

sealed class EnhancementResult {
    abstract val message: String

    data class Success(override val message: String) : EnhancementResult()
    data class Failure(override val message: String) : EnhancementResult()
    data class Destroyed(override val message: String) : EnhancementResult()
    data class Error(override val message: String) : EnhancementResult()
}
