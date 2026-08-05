package com.whatever.caro.feature.learning

import com.whatever.caro.core.model.learning.StudyRating

internal data class LearningInteractionAvailability(
    val swipeEnabled: Boolean,
    val evaluationEnabled: Boolean,
)

internal data class PendingEvaluationState(
    val rating: StudyRating? = null,
    private val hasObservedSubmission: Boolean = false,
) {
    val hasPendingRating: Boolean get() = rating != null

    fun select(rating: StudyRating): PendingEvaluationState = PendingEvaluationState(rating = rating)

    fun onSubmissionStateChanged(
        isSubmitting: Boolean,
        hasSubmissionError: Boolean,
    ): PendingEvaluationState =
        when {
            rating == null -> this
            isSubmitting -> copy(hasObservedSubmission = true)
            hasSubmissionError || hasObservedSubmission -> PendingEvaluationState()
            else -> this
        }
}

internal fun learningInteractionAvailability(
    isSubmitting: Boolean,
    hasPendingRating: Boolean,
): LearningInteractionAvailability =
    LearningInteractionAvailability(
        // 버튼 평가 중에는 스와이프 modifier 를 끄지 않는다.
        // enabled = false 는 modifier 가 카드를 원점으로 되돌리게 만들고, 그 애니메이션이
        // 같은 Animatable 을 가져가면서 버튼 퇴장 애니메이션을 취소한다.
        // 추가 입력 차단은 evaluationEnabled 가 담당한다.
        swipeEnabled = !isSubmitting,
        evaluationEnabled = !isSubmitting && !hasPendingRating,
    )
