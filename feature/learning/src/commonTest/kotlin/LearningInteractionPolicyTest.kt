package com.whatever.caro.feature.learning

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LearningInteractionPolicyTest :
    FunSpec({
        test("카드 면과 무관한 기본 평가 입력은 모두 허용한다") {
            learningInteractionAvailability(
                isSubmitting = false,
                hasPendingRating = false,
            ) shouldBe
                LearningInteractionAvailability(
                    swipeEnabled = true,
                    evaluationEnabled = true,
                )
        }

        test("버튼 평가 애니메이션 중에는 버튼 중복 입력만 막는다") {
            learningInteractionAvailability(
                isSubmitting = false,
                hasPendingRating = true,
            ) shouldBe
                LearningInteractionAvailability(
                    swipeEnabled = true,
                    evaluationEnabled = false,
                )
        }

        test("평가 제출 중에는 모든 평가 입력을 막는다") {
            learningInteractionAvailability(
                isSubmitting = true,
                hasPendingRating = false,
            ) shouldBe
                LearningInteractionAvailability(
                    swipeEnabled = false,
                    evaluationEnabled = false,
                )
        }
    })
