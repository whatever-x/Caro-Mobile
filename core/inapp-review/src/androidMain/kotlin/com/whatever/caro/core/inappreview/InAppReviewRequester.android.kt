package com.whatever.caro.core.inappreview

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
actual fun rememberInAppReviewRequester(): InAppReviewRequester {
    val activity = rememberUpdatedState(LocalActivity.current)
    val context = LocalContext.current.applicationContext
    val repository = koinInject<ReviewRequestRepository>()
    return remember(context, repository) {
        AndroidInAppReviewRequester(
            activity = { activity.value },
            reviewManager = ReviewManagerFactory.create(context),
            gate = InAppReviewGate(repository),
        )
    }
}

internal class AndroidInAppReviewRequester(
    private val activity: () -> Activity?,
    private val reviewManager: ReviewManager,
    private val gate: InAppReviewGate,
) : InAppReviewRequester {
    override suspend fun requestReview() =
        withContext(Dispatchers.Main.immediate) {
            try {
                currentCoroutineContext().ensureActive()
                if (activity()?.canRequestReview() != true) return@withContext
                val reviewInfo = reviewManager.requestReviewFlow().awaitCancellable()
                currentCoroutineContext().ensureActive()
                if (activity()?.canRequestReview() != true) return@withContext
                if (!gate.tryReserve()) return@withContext
                currentCoroutineContext().ensureActive()
                val currentActivity = activity()?.takeIf { it.canRequestReview() } ?: return@withContext
                reviewManager.launchReviewFlow(currentActivity, reviewInfo).awaitCancellable()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Napier.w("In-app review request failed", exception)
            }
        }
}

private fun Activity.canRequestReview(): Boolean =
    !isFinishing && !isDestroyed &&
        (this as? LifecycleOwner)?.lifecycle?.currentState == Lifecycle.State.RESUMED

private suspend fun <T> Task<T>.awaitCancellable(): T =
    suspendCancellableCoroutine { continuation ->
        // Activity-scoped listeners are removed at STOP and cannot complete a suspended caller.
        addOnCompleteListener(Executor { it.run() }) { task ->
            if (continuation.isActive) {
                when {
                    task.isCanceled -> continuation.cancel()
                    task.isSuccessful -> continuation.resume(task.result)
                    else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Review task failed"))
                }
            }
        }
    }
