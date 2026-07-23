package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.learning.StudyRatingCounts
import com.whatever.caro.core.model.learning.StudySession
import com.whatever.caro.core.remote.datasource.study.StudySessionDataSource
import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest
import com.whatever.caro.core.remote.dto.studySession.response.EvaluationResponse
import com.whatever.caro.core.remote.dto.studySession.response.InProgress
import com.whatever.caro.core.remote.dto.studySession.response.RatingCountsDto
import com.whatever.caro.core.remote.dto.studySession.response.StudyCardItemDto
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StudySessionRepositoryImplTest :
    FunSpec({
        test("daily session response is mapped to domain cards") {
            val source =
                mock<StudySessionDataSource> {
                    everySuspend { startDaily(any(), any()) } returns
                        InProgress(
                            sessionId = 7L,
                            studiedCardCount = 2,
                            totalCardCount = 4,
                            cards = listOf(StudyCardItemDto(11L, mapOf("front" to "Run", "back" to "달리다"))),
                        )
                }

            val result = StudySessionRepositoryImpl(source).startDaily(3L, "key")

            result shouldBe
                StudySession.InProgress(
                    sessionId = 7L,
                    studiedCardCount = 2,
                    totalCardCount = 4,
                    cards =
                        listOf(
                            StudyCard(11L, "Run", "달리다"),
                        ),
                )

            verifySuspend { source.startDaily(3L, "key") }
        }

        test("rating names match the API contract") {
            StudyRating.entries.map { it.name } shouldBe listOf("AGAIN", "FAIR", "EASY")
        }

        test("evaluation time is preserved in the API request") {
            val source =
                mock<StudySessionDataSource> {
                    everySuspend { evaluate(any(), any(), any()) } returns evaluationResponse()
                }
            val repository = StudySessionRepositoryImpl(source)

            repository.submit(7L, listOf(StudyEvaluation(11L, StudyRating.EASY, 1_200)), "key")

            verifySuspend {
                source.evaluate(
                    7L,
                    "key",
                    listOf(EvaluatedCardRequest(11L, EvaluatedCardRequest.RatingDto.EASY, 1_200)),
                )
            }
        }

        test("evaluation response rating counts are mapped to the domain result") {
            val source =
                mock<StudySessionDataSource> {
                    everySuspend { evaluate(any(), any(), any()) } returns
                        evaluationResponse(
                            RatingCountsDto(
                                again = 2,
                                fair = 3,
                                easy = 5,
                            ),
                        )
                }
            val repository = StudySessionRepositoryImpl(source)

            val result = repository.submit(7L, listOf(StudyEvaluation(11L, StudyRating.EASY, 1_200)), "key")

            result shouldBe StudyRatingCounts(again = 2, fair = 3, easy = 5)
        }
    })

private fun evaluationResponse(ratingCounts: RatingCountsDto? = null) =
    EvaluationResponse(
        evaluatedCardIds = emptyList(),
        failedCardIds = emptyList(),
        sessionStatus = EvaluationResponse.SessionStatusDto.ACTIVE,
        ratingCounts = ratingCounts,
    )
