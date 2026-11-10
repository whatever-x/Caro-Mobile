package com.whatever.caro.feature.setting.platform

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.setting_report_bug_url
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.inappreview.InAppReviewRequester
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.profile.MyInfo
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.route.SettingRoute
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.job
import org.jetbrains.compose.resources.stringResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class InquiryRouteAndroidTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `late successful URL completion after host return requests review once and disposal cancels it`() {
        val viewModel = createViewModel()
        val completion = CompletableDeferred<Boolean>()
        val visible = mutableStateOf(true)
        val requester = RecordingRequester()
        var openedUrl: String? = null
        var expectedUrl: String? = null
        var disposed = false
        mount(viewModel, visible, requester, onExpectedUrl = { expectedUrl = it }, onDisposed = { disposed = true }) { url ->
            openedUrl = url
            completion.await()
        }
        composeRule.runOnIdle { viewModel.intent(SettingIntent.ClickReportBug) }
        composeRule.waitUntil { openedUrl != null }
        openedUrl shouldBe expectedUrl
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        requester.requests shouldBe 0

        composeRule.runOnIdle { completion.complete(true) }
        composeRule.waitUntil { requester.requests == 1 }
        composeRule.runOnIdle { viewModel.intent(SettingIntent.AppResumed) }
        composeRule.waitForIdle()
        requester.requests shouldBe 1
        composeRule.runOnIdle { visible.value = false }
        composeRule.waitForIdle()
        composeRule.waitUntil { requester.cancelled }
        disposed shouldBe true
        requester.job?.isCancelled shouldBe true
    }

    @Test
    fun `failed URL completion after host return never requests review`() {
        val viewModel = createViewModel()
        val completion = CompletableDeferred<Boolean>()
        val requester = RecordingRequester()
        var opening = false
        mount(viewModel, mutableStateOf(true), requester) {
            opening = true
            completion.await()
        }
        composeRule.runOnIdle { viewModel.intent(SettingIntent.ClickReportBug) }
        composeRule.waitUntil { opening }
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.runOnIdle { completion.complete(false) }
        composeRule.waitForIdle()

        requester.requests shouldBe 0
    }

    @Test
    fun `leaving the route cancels pending URL work before a late successful callback`() {
        val viewModel = createViewModel()
        val completion = CompletableDeferred<Boolean>()
        val requester = RecordingRequester()
        val visible = mutableStateOf(true)
        var opening = false
        var cancelled = false
        var disposed = false
        var openingJob: Job? = null
        mount(viewModel, visible, requester, onDisposed = { disposed = true }) {
            opening = true
            openingJob = currentCoroutineContext().job
            try {
                completion.await()
            } finally {
                cancelled = true
            }
        }
        composeRule.runOnIdle { viewModel.intent(SettingIntent.ClickReportBug) }
        composeRule.waitUntil { opening }

        composeRule.runOnIdle { visible.value = false }
        composeRule.waitForIdle()
        composeRule.waitUntil { cancelled }
        disposed shouldBe true
        openingJob?.isCancelled shouldBe true
        composeRule.runOnIdle {
            completion.complete(true)
            viewModel.intent(SettingIntent.AppBackgrounded)
            viewModel.intent(SettingIntent.AppResumed)
        }
        composeRule.waitForIdle()

        requester.requests shouldBe 0
    }

    @Test
    fun `accepted inquiry and real background survive host recreation with the same ViewModel`() {
        val viewModel = createViewModel()
        val requester = RecordingRequester()
        var openings = 0
        var disposals = 0
        val opener: suspend (String) -> Boolean = {
            openings++
            true
        }
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            composeRule.runOnUiThread {
                controller.get().setContent {
                    CaroTheme {
                        DisposableEffect(Unit) { onDispose { disposals++ } }
                        SettingRoute(viewModel, NoNavigation, NoSnackbar, opener, requester)
                    }
                }
            }
            composeRule.waitForIdle()
            composeRule.runOnIdle { viewModel.intent(SettingIntent.ClickReportBug) }
            composeRule.waitUntil { openings == 1 }
            composeRule.waitForIdle()
            val previousActivity = controller.get()
            val previousHostEvents = mutableListOf<Lifecycle.Event>()
            composeRule.runOnUiThread {
                previousActivity.lifecycle.addObserver(LifecycleEventObserver { _, event -> previousHostEvents += event })
                previousHostEvents.clear()
                controller.pause().stop()
            }

            // ActivityScenario.recreate resumes a stopped old host first. Use the controller's
            // STOPPED path so recreation itself does not fabricate an application return.
            composeRule.runOnUiThread {
                controller.recreate()
                controller.get().setContent {
                    CaroTheme {
                        DisposableEffect(Unit) { onDispose { disposals++ } }
                        SettingRoute(viewModel, NoNavigation, NoSnackbar, opener, requester)
                    }
                }
                controller.restart().start().resume()
            }
            composeRule.waitForIdle()

            (controller.get() === previousActivity) shouldBe false
            previousHostEvents.count { it == Lifecycle.Event.ON_RESUME } shouldBe 0
            previousHostEvents.count { it == Lifecycle.Event.ON_STOP } shouldBe 1
            disposals shouldBe 1
            openings shouldBe 1
            composeRule.waitUntil { requester.requests == 1 }
            composeRule.runOnUiThread {
                controller
                    .pause()
                    .stop()
                    .restart()
                    .start()
                    .resume()
            }
            composeRule.waitForIdle()
            requester.requests shouldBe 1
        } finally {
            composeRule.runOnUiThread { controller.pause().stop().destroy() }
        }
    }

    @Test
    fun `configuration alone is not background and retained inquiry waits for a real return`() {
        val viewModel = createViewModel()
        val requester = RecordingRequester()
        var openings = 0
        var disposals = 0
        val opener: suspend (String) -> Boolean = {
            openings++
            true
        }
        mount(viewModel, mutableStateOf(true), requester, onDisposed = { disposals++ }, opener = opener)
        composeRule.runOnIdle { viewModel.intent(SettingIntent.ClickReportBug) }
        composeRule.waitUntil { openings == 1 }
        composeRule.waitForIdle()
        val previousActivity = composeRule.activity

        composeRule.activityRule.scenario.recreate()
        reattach(viewModel, requester, onDisposed = { disposals++ }, opener = opener)
        composeRule.waitForIdle()

        (composeRule.activity === previousActivity) shouldBe false
        disposals shouldBe 1
        requester.requests shouldBe 0
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        openings shouldBe 1
        composeRule.waitUntil { requester.requests == 1 }
    }

    private fun reattach(
        viewModel: SettingViewModel,
        requester: InAppReviewRequester,
        onDisposed: () -> Unit,
        opener: suspend (String) -> Boolean,
    ) {
        // MainActivity also attaches new Compose content in onCreate after configuration recreation.
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                CaroTheme {
                    DisposableEffect(Unit) { onDispose(onDisposed) }
                    SettingRoute(viewModel, NoNavigation, NoSnackbar, opener, requester)
                }
            }
        }
    }

    private fun mount(
        viewModel: SettingViewModel,
        visible: MutableState<Boolean>,
        requester: InAppReviewRequester,
        onExpectedUrl: (String) -> Unit = {},
        onDisposed: () -> Unit = {},
        opener: suspend (String) -> Boolean,
    ) {
        composeRule.setContent {
            onExpectedUrl(stringResource(Res.string.setting_report_bug_url))
            CaroTheme {
                if (visible.value) {
                    DisposableEffect(Unit) { onDispose(onDisposed) }
                    SettingRoute(viewModel, NoNavigation, NoSnackbar, opener, requester)
                }
            }
        }
    }
}

private fun createViewModel(): SettingViewModel =
    SettingViewModel(
        authRepository = mock<AuthRepository>(),
        profileRepository =
            mock<ProfileRepository> {
                everySuspend { getMyInfo() } returns MyInfo("", "", SocialLoginType.NONE)
            },
        exceptionFilter = ExceptionFilter.None,
        fcmTokenRepository =
            mock<FcmTokenRepository> {
                every { observeNotificationEnabled() } returns MutableStateFlow(false)
                every { updateNotificationPermission(any()) } returns Unit
            },
    )

private class RecordingRequester : InAppReviewRequester {
    var requests = 0
    var cancelled = false
    var job: Job? = null

    override suspend fun requestReview() {
        job = currentCoroutineContext().job
        requests++
        try {
            awaitCancellation()
        } finally {
            cancelled = true
        }
    }
}

private object NoNavigation : NavigationDispatcher {
    override val commands = emptyFlow<NavCommand>()

    override suspend fun emit(command: NavCommand) = Unit
}

private object NoSnackbar : SnackbarController {
    override val messages = emptyFlow<SnackBarMessage>()

    override suspend fun show(message: SnackBarMessage) = Unit
}
