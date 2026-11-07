package com.whatever.caro.core.inappreview

import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class InAppReviewGate(
    private val repository: ReviewRequestRepository,
    private val clock: Clock = Clock.System,
) {
    /**
     * [request] must synchronously invoke the SDK or return false when it did not invoke it.
     * A throw at this boundary cannot prove non-invocation, so it retains the interval.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun requestIfAllowed(request: () -> Boolean) {
        currentCoroutineContext().ensureActive()
        val reservationId = Uuid.random().toString()
        // NonCancellable keeps the same dispatcher, so cancellation cannot discard the claim on return.
        val reserved =
            withContext(NonCancellable) {
                repository.tryReserveRequest(reservationId, clock.now().toEpochMilliseconds(), REQUEST_INTERVAL.inWholeMilliseconds)
            }
        if (!reserved) {
            currentCoroutineContext().ensureActive()
            return
        }
        var attemptedAt: Long? = null
        // Capture even cancellation until cleanup finishes; getOrThrow below always propagates it.
        val requestResult =
            runCatching {
                currentCoroutineContext().ensureActive()
                attemptedAt = clock.now().toEpochMilliseconds()
                if (!request()) attemptedAt = null
            }
        try {
            withContext(NonCancellable) {
                val timestamp = attemptedAt
                if (timestamp == null) {
                    repository.cancelRequest(reservationId)
                } else {
                    repository.commitRequest(reservationId, timestamp)
                }
            }
        } catch (finalizationFailure: Throwable) {
            val failure = requestResult.exceptionOrNull() ?: throw finalizationFailure
            failure.addSuppressed(finalizationFailure)
        }
        requestResult.getOrThrow()
        currentCoroutineContext().ensureActive()
    }

    private companion object {
        val REQUEST_INTERVAL = 30.days
    }
}
