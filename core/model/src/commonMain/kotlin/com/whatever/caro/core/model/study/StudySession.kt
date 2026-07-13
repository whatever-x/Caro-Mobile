package com.whatever.caro.core.model.study

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

data class StudyCard(
    val id: Long,
    val front: String,
    val back: String,
)

data class StudyEvaluation(
    val cardId: Long,
    val rating: StudyRating,
    val timeMs: Int,
)

enum class StudyRating { AGAIN, FAIR, EASY }
