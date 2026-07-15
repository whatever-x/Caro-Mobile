package com.whatever.caro.core.model.learning

sealed interface StudySession {
    data class InProgress(
        val sessionId: Long,
        val studiedCardCount: Int,
        val totalCardCount: Int,
        val cards: List<StudyCard>,
    ) : StudySession

    data class Completed(
        val studiedCardCount: Int,
        val totalCardCount: Int,
    ) : StudySession

    data object RestDay : StudySession
}
