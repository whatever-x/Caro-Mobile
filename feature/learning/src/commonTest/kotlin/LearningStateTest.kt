package com.whatever.caro.feature.learning

import com.whatever.caro.feature.learning.mvi.LearningState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class LearningStateTest :
    FunSpec({
        test("초기 로딩 중에는 학습 본문을 노출하지 않는다") {
            LearningState(isLoading = true).isLoadedContentVisible.shouldBeFalse()
        }

        test("초기 로딩이 끝나면 학습 본문을 노출한다") {
            LearningState(isLoading = false).isLoadedContentVisible.shouldBeTrue()
        }
    })
