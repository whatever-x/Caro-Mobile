package com.whatever.caro.feature.learning

import com.whatever.caro.core.model.learning.StudyRating
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LearningInteractionPolicyTest : FunSpec() {
    init {
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

        test("버튼 평가 애니메이션 중에는 모든 추가 평가 입력을 막는다") {
            learningInteractionAvailability(
                isSubmitting = false,
                hasPendingRating = true,
            ) shouldBe
                LearningInteractionAvailability(
                    swipeEnabled = false,
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

        test("제출 실패 후 제출 상태가 종료되면 평가 버튼을 다시 활성화한다") {
            var pendingEvaluation = PendingEvaluationState().select(StudyRating.EASY)

            pendingEvaluation =
                pendingEvaluation.onSubmissionStateChanged(
                    isSubmitting = true,
                    hasSubmissionError = false,
                )
            pendingEvaluation =
                pendingEvaluation.onSubmissionStateChanged(
                    isSubmitting = false,
                    hasSubmissionError = true,
                )
            pendingEvaluation =
                pendingEvaluation.onSubmissionStateChanged(
                    isSubmitting = false,
                    hasSubmissionError = false,
                )

            pendingEvaluation.rating shouldBe null
            learningInteractionAvailability(
                isSubmitting = false,
                hasPendingRating = pendingEvaluation.hasPendingRating,
            ).evaluationEnabled shouldBe true
        }
    }
}
