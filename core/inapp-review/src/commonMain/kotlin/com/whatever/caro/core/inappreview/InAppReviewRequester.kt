package com.whatever.caro.core.inappreview

import androidx.compose.runtime.Composable

interface InAppReviewRequester {
    /** Attempts a store request; returning does not confirm that a review was displayed or submitted. */
    suspend fun requestReview()
}

@Composable
expect fun rememberInAppReviewRequester(): InAppReviewRequester
