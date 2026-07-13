package com.whatever.caro.feature.learning.mapper

import com.whatever.caro.core.model.study.StudyRating
import com.whatever.caro.core.ui.swipe.SwipeDirection

internal fun SwipeDirection.toRating(): StudyRating =
    when (this) {
        SwipeDirection.LEFT -> StudyRating.EASY
        SwipeDirection.UP -> StudyRating.FAIR
        SwipeDirection.RIGHT -> StudyRating.AGAIN
    }

internal fun StudyRating.toSwipeDirection(): SwipeDirection =
    when (this) {
        StudyRating.EASY -> SwipeDirection.LEFT
        StudyRating.FAIR -> SwipeDirection.UP
        StudyRating.AGAIN -> SwipeDirection.RIGHT
    }
