package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudySession
import com.whatever.caro.core.remote.datasource.study.StudySessionDataSource
import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class StudySessionRepositoryImpl(
    private val source: StudySessionDataSource,
    private val idempotencyKey: () -> String = ::newUuid,
) : StudySessionRepository {
    override suspend fun startDaily(deckId: Long): StudySession = source.startDaily(deckId, idempotencyKey()).toModel()

    override suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
    ) = source.evaluate(
        sessionId,
        idempotencyKey(),
        evaluations.map { EvaluatedCardRequest(it.cardId, EvaluatedCardRequest.RatingDto.valueOf(it.rating.name), it.timeMs) },
    )
}

@OptIn(ExperimentalUuidApi::class)
private fun newUuid(): String = Uuid.random().toString()
