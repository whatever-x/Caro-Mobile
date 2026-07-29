package com.whatever.caro.feature.learning

internal data class LearningInteractionAvailability(
    val swipeEnabled: Boolean,
    val evaluationEnabled: Boolean,
)

internal fun learningInteractionAvailability(
    isSubmitting: Boolean,
    hasPendingRating: Boolean,
): LearningInteractionAvailability =
    LearningInteractionAvailability(
        swipeEnabled = !isSubmitting,
        evaluationEnabled = !isSubmitting && !hasPendingRating,
    )
