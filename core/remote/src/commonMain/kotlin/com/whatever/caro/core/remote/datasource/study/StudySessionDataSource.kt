package com.whatever.caro.core.remote.datasource.study

import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest
import com.whatever.caro.core.remote.dto.studySession.response.DailyStudyResponse
import com.whatever.caro.core.remote.dto.studySession.response.EvaluationResponse

interface StudySessionDataSource {
    suspend fun startDaily(
        deckId: Long,
        idempotencyKey: String,
    ): DailyStudyResponse

    suspend fun evaluate(
        sessionId: Long,
        idempotencyKey: String,
        evaluations: List<EvaluatedCardRequest>,
    ): EvaluationResponse
}
