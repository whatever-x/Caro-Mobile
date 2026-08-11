package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.model.streak.StreakStatus
import com.whatever.caro.core.remote.dto.streak.response.StreakResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StreakMapperTest : FunSpec() {
    init {
        test("ACTIVE 응답은 현재 연속 학습 일수를 보존한다") {
            StreakResponse(
                status = StreakResponse.StatusDto.ACTIVE,
                currentStreak = 7,
            ).toStreakModel() shouldBe
                Streak(
                    status = StreakStatus.ACTIVE,
                    currentDays = 7,
                )
        }

        test("NOT_STARTED 와 BROKEN 은 응답 일수와 무관하게 0일이다") {
            StreakResponse(
                status = StreakResponse.StatusDto.NOT_STARTED,
                currentStreak = 8,
            ).toStreakModel().currentDays shouldBe 0

            StreakResponse(
                status = StreakResponse.StatusDto.BROKEN,
                currentStreak = 8,
            ).toStreakModel().currentDays shouldBe 0
        }

        test("nullable 상태와 잘못된 ACTIVE 일수는 안전한 기본값으로 정규화한다") {
            StreakResponse(
                status = null,
                currentStreak = null,
            ).toStreakModel() shouldBe
                Streak(
                    status = StreakStatus.NOT_STARTED,
                    currentDays = 0,
                )

            StreakResponse(
                status = StreakResponse.StatusDto.ACTIVE,
                currentStreak = -3,
            ).toStreakModel().currentDays shouldBe 0
        }
    }
}
