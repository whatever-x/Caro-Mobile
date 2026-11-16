@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.whatever.caro.core.inappreview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosInAppReviewRequesterTest {
    @Test
    fun `a controller without an active scene never reserves or invokes the bridge`() =
        runReviewTest {
            val repository = RecordingReviewRepository()
            val controller = UIViewController()
            var calls = 0
            IosInAppReviewRequester({ controller }, {
                {
                    calls++
                    true
                }
            }, InAppReviewGate(repository)).requestReview()
            assertEquals(0, repository.reservations)
            assertEquals(0, calls)
        }

    @Test
    fun `scene deactivation during reservation cancels without invoking the bridge`() =
        runReviewTest {
            val controller = UIViewController()
            var active = true
            var calls = 0
            val repository = RecordingReviewRepository(onReserve = { active = false })
            IosInAppReviewRequester(
                { controller },
                {
                    {
                        calls++
                        true
                    }
                },
                InAppReviewGate(repository),
                canRequestReview = { active },
            ).requestReview()
            assertEquals(0, calls)
            assertEquals(1, repository.cancellations)
            assertFalse(repository.requestRecorded)
        }

    @Test
    fun `Swift false means no StoreKit invocation and releases the interval`() =
        runReviewTest {
            val controller = UIViewController()
            val repository = RecordingReviewRepository()
            IosInAppReviewRequester(
                { controller },
                { { false } },
                InAppReviewGate(repository),
                canRequestReview = { true },
            ).requestReview()
            assertEquals(1, repository.cancellations)
            assertFalse(repository.requestRecorded)
        }

    @Test
    fun `a disappearing bridge cancels the reservation`() =
        runReviewTest {
            val controller = UIViewController()
            var bridge: ((UIViewController) -> Boolean)? = { true }
            val repository = RecordingReviewRepository(onReserve = { bridge = null })
            IosInAppReviewRequester(
                { controller },
                { bridge },
                InAppReviewGate(repository),
                canRequestReview = { true },
            ).requestReview()
            assertEquals(1, repository.cancellations)
            assertFalse(repository.requestRecorded)
        }

    @Test
    fun `StoreKit invocation keeps the record without any UI feedback`() =
        runReviewTest {
            val controller = UIViewController()
            val repository = RecordingReviewRepository()
            var calls = 0
            val requester =
                IosInAppReviewRequester(
                    { controller },
                    {
                        {
                            calls++
                            true
                        }
                    },
                    InAppReviewGate(repository),
                    canRequestReview = { true },
                )
            requester.requestReview()
            requester.requestReview()
            assertEquals(1, calls)
            assertEquals(1, repository.commits)
            assertTrue(repository.requestRecorded)
        }

    @Test
    fun `an exception at the bridge boundary conservatively keeps the interval`() =
        runReviewTest {
            val controller = UIViewController()
            val repository = RecordingReviewRepository()
            IosInAppReviewRequester(
                { controller },
                { { error("bridge failure") } },
                InAppReviewGate(repository),
                canRequestReview = { true },
            ).requestReview()
            assertEquals(1, repository.commits)
            assertTrue(repository.requestRecorded)
        }

    @Test
    fun `cancellation during reservation cancels the record and never calls StoreKit`() =
        runReviewTest {
            val release = CompletableDeferred<Unit>()
            val controller = UIViewController()
            val repository = RecordingReviewRepository(onReserve = { release.await() })
            var calls = 0
            val requester =
                IosInAppReviewRequester(
                    { controller },
                    {
                        {
                            calls++
                            true
                        }
                    },
                    InAppReviewGate(repository),
                    canRequestReview = { true },
                )
            val job = launch { requester.requestReview() }
            runCurrent()
            job.cancel()
            release.complete(Unit)
            job.join()
            assertTrue(job.isCancelled)
            assertEquals(0, calls)
            assertFalse(repository.requestRecorded)
        }
}

private fun runReviewTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
