package com.whatever.caro.core.inappreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.uikit.LocalUIViewController
import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController

private val LocalRequestStoreReview = staticCompositionLocalOf<((UIViewController) -> Unit)?> { null }

@Composable
fun ProvideIosInAppReviewRequester(
    onRequestStoreReview: (UIViewController) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRequestStoreReview provides onRequestStoreReview, content = content)
}

@Composable
actual fun rememberInAppReviewRequester(): InAppReviewRequester {
    val controller = rememberUpdatedState(LocalUIViewController.current)
    val requestStoreReview = rememberUpdatedState(LocalRequestStoreReview.current)
    val repository = koinInject<ReviewRequestRepository>()
    return remember(repository) {
        IosInAppReviewRequester(
            controller = { controller.value },
            requestStoreReview = { requestStoreReview.value },
            gate = InAppReviewGate(repository),
        )
    }
}

internal class IosInAppReviewRequester(
    private val controller: () -> UIViewController,
    private val requestStoreReview: () -> ((UIViewController) -> Unit)?,
    private val gate: InAppReviewGate,
) : InAppReviewRequester {
    override suspend fun requestReview() =
        withContext(Dispatchers.Main.immediate) {
            try {
                currentCoroutineContext().ensureActive()
                if (requestStoreReview() == null || !controller().canRequestReview()) return@withContext
                if (!gate.tryReserve()) return@withContext
                currentCoroutineContext().ensureActive()
                val currentController = controller()
                if (currentController.canRequestReview()) {
                    requestStoreReview()?.invoke(currentController)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Napier.w("In-app review request failed", exception)
            }
        }
}

private fun UIViewController.canRequestReview(): Boolean =
    view.window?.windowScene?.activationState == UISceneActivationStateForegroundActive
