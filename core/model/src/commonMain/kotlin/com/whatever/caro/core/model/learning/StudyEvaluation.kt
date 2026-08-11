package com.whatever.caro.core.model.learning

data class StudyEvaluation(
    val cardId: Long,
    val rating: StudyRating,
    val timeMs: Int,
)
