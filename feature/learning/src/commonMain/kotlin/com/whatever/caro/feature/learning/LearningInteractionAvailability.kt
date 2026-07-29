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
        swipeEnabled = !isSubmitting && !hasPendingRating,
        evaluationEnabled = !isSubmitting && !hasPendingRating,
    )
