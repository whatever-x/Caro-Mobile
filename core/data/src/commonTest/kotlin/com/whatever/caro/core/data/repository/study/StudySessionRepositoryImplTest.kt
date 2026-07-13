package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.study.StudyRating
import com.whatever.caro.core.model.study.StudySession
import com.whatever.caro.core.remote.datasource.study.StudySessionDataSource
import com.whatever.caro.core.remote.dto.studySession.response.InProgress
import com.whatever.caro.core.remote.dto.studySession.response.StudyCardItemDto
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
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

            val result = StudySessionRepositoryImpl(source, idempotencyKey = { "key" }).startDaily(3L)

            result shouldBe
                StudySession.InProgress(
                    sessionId = 7L,
                    studiedCardCount = 2,
                    totalCardCount = 4,
                    cards =
                        listOf(
                            com.whatever.caro.core.model.study
                                .StudyCard(11L, "Run", "달리다"),
                        ),
                )
        }

        test("rating names match the API contract") {
            StudyRating.entries.map { it.name } shouldBe listOf("AGAIN", "FAIR", "EASY")
        }
    })
