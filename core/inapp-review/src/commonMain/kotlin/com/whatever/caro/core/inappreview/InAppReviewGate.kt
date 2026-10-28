package com.whatever.caro.core.inappreview

import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal class InAppReviewGate(
    private val repository: ReviewRequestRepository,
    private val clock: Clock = Clock.System,
) {
    suspend fun tryReserve(): Boolean {
        currentCoroutineContext().ensureActive()
        return repository.tryReserveRequest(clock.now().toEpochMilliseconds(), REQUEST_INTERVAL.inWholeMilliseconds)
    }

    private companion object {
        val REQUEST_INTERVAL = 30.days
    }
}
