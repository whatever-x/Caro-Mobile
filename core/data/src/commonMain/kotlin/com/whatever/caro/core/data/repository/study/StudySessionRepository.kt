package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudySession

interface StudySessionRepository {
    suspend fun startDaily(deckId: Long): StudySession

    suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
    )
}
