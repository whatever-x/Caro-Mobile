package com.whatever.caro.core.data.repository.streak

import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.model.streak.StreakStatus
import com.whatever.caro.core.remote.datasource.streak.StreakDataSource
import com.whatever.caro.core.remote.dto.streak.response.StreakResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class StreakRepositoryImplTest : FunSpec() {
    init {
        test("getStreak은 remote 응답을 domain 모델로 반환한다") {
            runTest {
                val dataSource =
                    mock<StreakDataSource> {
                        everySuspend { getStreak() } returns
                            StreakResponse(
                                status = StreakResponse.StatusDto.ACTIVE,
                                currentStreak = 4,
                            )
                    }
                val repository = StreakRepositoryImpl(dataSource)

                repository.getStreak() shouldBe
                    Streak(
                        status = StreakStatus.ACTIVE,
                        currentDays = 4,
                    )
                verifySuspend { dataSource.getStreak() }
            }
        }
    }
}
