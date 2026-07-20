package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRatingCounts
import com.whatever.caro.core.model.learning.StudySession
import com.whatever.caro.core.remote.datasource.study.StudySessionDataSource
import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest

internal class StudySessionRepositoryImpl(
    private val source: StudySessionDataSource,
) : StudySessionRepository {
    override suspend fun startDaily(
        deckId: Long,
        idempotencyKey: String,
    ): StudySession = source.startDaily(deckId, idempotencyKey).toModel()

    override suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
        idempotencyKey: String,
    ): StudyRatingCounts {
        if (evaluations.isEmpty()) return StudyRatingCounts()
        val ratingCounts =
            source
                .evaluate(
                    sessionId,
                    idempotencyKey,
                    evaluations.map {
                        EvaluatedCardRequest(
                            it.cardId,
                            EvaluatedCardRequest.RatingDto.valueOf(it.rating.name),
                            it.timeMs,
                        )
                    },
                ).ratingCounts
        return StudyRatingCounts(
            again = ratingCounts?.again ?: 0,
            fair = ratingCounts?.fair ?: 0,
            easy = ratingCounts?.easy ?: 0,
        )
    }
}
