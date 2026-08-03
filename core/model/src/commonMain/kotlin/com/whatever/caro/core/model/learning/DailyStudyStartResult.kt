package com.whatever.caro.core.model.learning

data class ActiveDailyStudySession(
    val sessionId: Long,
    val studiedCardCount: Int,
    val totalCardCount: Int,
    val cards: List<StudyCard>,
)

sealed interface DailyStudyStartResult {
    data class Started(
        val session: ActiveDailyStudySession,
    ) : DailyStudyStartResult

    data class Completed(
        val studiedCardCount: Int,
        val totalCardCount: Int,
    ) : DailyStudyStartResult

    data object RestDay : DailyStudyStartResult
}
