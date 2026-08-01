package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.learning.ActiveDailyStudySession
import com.whatever.caro.core.model.learning.DailyStudyStartResult
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.remote.dto.studySession.response.Completed
import com.whatever.caro.core.remote.dto.studySession.response.InProgress
import com.whatever.caro.core.remote.dto.studySession.response.RestDay
import com.whatever.caro.core.remote.dto.studySession.response.StudyCardItemDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DailyStudyStartResultMapperTest : FunSpec() {
    init {
        test("진행 중 응답을 활성 일일 학습 결과로 매핑한다") {
            val response =
                InProgress(
                    sessionId = 7L,
                    studiedCardCount = 2,
                    totalCardCount = 4,
                    cards = listOf(StudyCardItemDto(11L, mapOf("front" to "Run", "back" to "달리다"))),
                )

            response.toDailyStudyStartResult() shouldBe
                DailyStudyStartResult.Started(
                    ActiveDailyStudySession(
                        sessionId = 7L,
                        studiedCardCount = 2,
                        totalCardCount = 4,
                        cards = listOf(StudyCard(11L, "Run", "달리다")),
                    ),
                )
        }

        test("완료 응답을 완료된 일일 학습 결과로 매핑한다") {
            Completed(
                sessionId = 7L,
                studiedCardCount = 4,
                totalCardCount = 4,
            ).toDailyStudyStartResult() shouldBe
                DailyStudyStartResult.Completed(
                    studiedCardCount = 4,
                    totalCardCount = 4,
                )
        }

        test("휴식일 응답을 휴식일 결과로 매핑한다") {
            RestDay.toDailyStudyStartResult() shouldBe DailyStudyStartResult.RestDay
        }
    }
}
