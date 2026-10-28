package com.whatever.caro.core.inappreview

import android.app.Activity
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class AndroidInAppReviewRequesterTest {
    @Test
    fun `준비된 ReviewInfo와 활성 Activity가 있으면 예약 후 SDK를 호출한다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            val repository = RecordingReviewRepository()
            manager.completeInfo(activity)
            val requester = AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000)))

            requester.requestReview()

            repository.reservations shouldBe 1
            manager.launchedActivity shouldBe activity
            controller.pause().stop().destroy()
        }

    @Test
    fun `ReviewInfo를 기다리는 동안 취소되면 늦은 성공 응답이 리뷰를 열지 않는다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            val repository = RecordingReviewRepository()
            val requester = AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000)))
            val job = launch { requester.requestReview() }
            runCurrent()

            job.cancelAndJoin()
            manager.completeInfo(activity)
            runCurrent()

            job.isCancelled shouldBe true
            repository.reservations shouldBe 0
            manager.launchedActivity shouldBe null
            controller.pause().stop().destroy()
        }

    @Test
    fun `ReviewInfo 준비 중 Activity가 멈추면 예약하거나 SDK를 열지 않는다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            val repository = RecordingReviewRepository()
            val requester = AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000)))
            val job = launch { requester.requestReview() }
            runCurrent()

            controller.pause().stop()
            manager.completeInfo(activity)
            runCurrent()

            job.isCompleted shouldBe true
            repository.reservations shouldBe 0
            manager.launchedActivity shouldBe null
            controller.destroy()
        }

    @Test
    fun `finishing Activity에는 리뷰를 열지 않는다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            val repository = RecordingReviewRepository()
            manager.completeInfo(activity)
            activity.finish()

            AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000))).requestReview()

            repository.reservations shouldBe 0
            manager.launchedActivity shouldBe null
            controller.pause().stop().destroy()
        }

    @Test
    fun `destroyed Activity에는 리뷰를 열지 않는다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            val repository = RecordingReviewRepository()
            manager.completeInfo(activity)
            controller.pause().stop().destroy()

            AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000))).requestReview()

            repository.reservations shouldBe 0
            manager.launchedActivity shouldBe null
        }

    @Test
    fun `저장 실패로 예약하지 못하면 SDK를 열지 않는다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            manager.completeInfo(activity)
            val repository = RecordingReviewRepository(IllegalStateException("disk failure"))

            AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000))).requestReview()

            manager.launchedActivity shouldBe null
            controller.pause().stop().destroy()
        }

    @Test
    fun `공용 간격이 허용하지 않은 요청은 SDK를 열지 않는다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            manager.completeInfo(activity)
            val repository = RecordingReviewRepository(allowed = false)

            AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000))).requestReview()

            repository.reservations shouldBe 1
            manager.launchedActivity shouldBe null
            controller.pause().stop().destroy()
        }

    @Test
    fun `예약 처리 중 Activity가 멈추면 SDK를 열기 직전에 다시 차단한다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            manager.completeInfo(activity)
            val repository =
                object : ReviewRequestRepository {
                    override suspend fun tryReserveRequest(
                        nowEpochMilliseconds: Long,
                        minimumIntervalMilliseconds: Long,
                    ): Boolean {
                        controller.pause().stop()
                        return true
                    }
                }

            AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000))).requestReview()

            manager.launchedActivity shouldBe null
            controller.destroy()
        }

    @Test
    fun `저장소 취소를 호출자에게 전파한다`() =
        runReviewTest {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val activity = controller.get()
            val manager = RecordingReviewManager()
            manager.completeInfo(activity)
            val repository = RecordingReviewRepository(CancellationException("cancelled"))
            val requester = AndroidInAppReviewRequester({ activity }, manager, InAppReviewGate(repository, ReviewClock(1_000)))
            val job = launch { requester.requestReview() }

            runCurrent()

            job.isCancelled shouldBe true
            manager.launchedActivity shouldBe null
            controller.pause().stop().destroy()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runReviewTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

private class RecordingReviewManager : ReviewManager {
    private val info = TaskCompletionSource<ReviewInfo>()
    var launchedActivity: Activity? = null

    fun completeInfo(activity: Activity) {
        info.setResult(FakeReviewManager(activity).requestReviewFlow().result)
    }

    override fun requestReviewFlow(): Task<ReviewInfo> = info.task

    override fun launchReviewFlow(
        activity: Activity,
        reviewInfo: ReviewInfo,
    ): Task<Void> {
        launchedActivity = activity
        return Tasks.forResult(null)
    }
}

private class RecordingReviewRepository(
    private val failure: Throwable? = null,
    private val allowed: Boolean = true,
) : ReviewRequestRepository {
    var reservations = 0

    override suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean {
        failure?.let { throw it }
        reservations++
        return allowed
    }
}
