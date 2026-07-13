package com.whatever.caro.core.remote.datasource.study

import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest
import com.whatever.caro.core.remote.dto.studySession.response.DailyStudyResponse

interface StudySessionDataSource {
    suspend fun startDaily(
        deckId: Long,
        idempotencyKey: String,
    ): DailyStudyResponse

    suspend fun evaluate(
        sessionId: Long,
        idempotencyKey: String,
        evaluations: List<EvaluatedCardRequest>,
    )
}
