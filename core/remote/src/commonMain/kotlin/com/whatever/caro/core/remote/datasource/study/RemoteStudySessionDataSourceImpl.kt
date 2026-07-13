package com.whatever.caro.core.remote.datasource.study

import com.whatever.caro.core.remote.api.StudySessionApi
import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest
import com.whatever.caro.core.remote.dto.studySession.request.StartDailyStudyRequest

internal class RemoteStudySessionDataSourceImpl(
    private val api: StudySessionApi,
) : StudySessionDataSource {
    override suspend fun startDaily(
        deckId: Long,
        idempotencyKey: String,
    ) = api.requestStartDailyStudy(idempotencyKey, StartDailyStudyRequest(deckId))

    override suspend fun evaluate(
        sessionId: Long,
        idempotencyKey: String,
        evaluations: List<EvaluatedCardRequest>,
    ) {
        api.requestEvaluate(sessionId, idempotencyKey, evaluations)
    }
}
