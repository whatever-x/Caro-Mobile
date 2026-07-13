package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.study.StudyEvaluation
import com.whatever.caro.core.model.study.StudySession

interface StudySessionRepository {
    suspend fun startDaily(deckId: Long): StudySession

    suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
    )
}
