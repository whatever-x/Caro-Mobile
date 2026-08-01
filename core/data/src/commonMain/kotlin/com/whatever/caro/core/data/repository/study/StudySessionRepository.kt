package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.learning.DailyStudyStartResult
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRatingCounts

interface StudySessionRepository {
    suspend fun startDaily(
        deckId: Long,
        idempotencyKey: String,
    ): DailyStudyStartResult

    suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
        idempotencyKey: String,
    ): StudyRatingCounts
}
