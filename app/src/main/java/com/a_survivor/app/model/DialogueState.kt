package com.a_survivor.app.model

data class DialoguePage(
    val speaker: String,
    val text: String,
    val choices: List<String> = emptyList()
)

data class DialogueSession(
    val pages: List<DialoguePage>,
    val currentIndex: Int = 0
) {
    val currentPage get() = pages[currentIndex]
    val isLastPage get() = currentIndex >= pages.size - 1
}
