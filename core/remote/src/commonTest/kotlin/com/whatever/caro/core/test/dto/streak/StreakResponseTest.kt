package com.whatever.caro.core.test.dto.streak

import com.whatever.caro.core.remote.dto.streak.response.StreakResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class StreakResponseTest : FunSpec() {
    init {
        test("status가 포함된 streak 응답을 역직렬화한다") {
            val response =
                Json.decodeFromString<StreakResponse>(
                    """
                    {
                      "status": "ACTIVE",
                      "currentStreak": 5
                    }
                    """.trimIndent(),
                )

            response.status shouldBe StreakResponse.StatusDto.ACTIVE
            response.currentStreak shouldBe 5
        }
    }
}
