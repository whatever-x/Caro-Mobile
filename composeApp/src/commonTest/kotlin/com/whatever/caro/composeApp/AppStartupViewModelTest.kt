package com.whatever.caro.composeApp

import com.whatever.caro.core.ads.AdInitializationResult
import com.whatever.caro.core.ads.InterstitialAdController
import com.whatever.caro.core.data.repository.ads.AdEligibilityState
import com.whatever.caro.core.viewmodel.ExceptionFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()
        beforeTest { Dispatchers.setMain(dispatcher) }
        afterTest { Dispatchers.resetMain() }

        test("성인 확인 전에는 초기화하지 않고 확인 후 초기화한다") {
            runTest(dispatcher) {
                val eligibility = MutableStateFlow(AdEligibilityState())
                val ads = FakeAdsController()
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, eligibility)
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 0
                eligibility.value = AdEligibilityState(generation = 1, allowed = true)
                advanceUntilIdle()
                ads.calls shouldBe 1
                eligibility.value = AdEligibilityState(generation = 2, allowed = false)
                advanceUntilIdle()
                viewModel.state.value.adEligible shouldBe false
            }
        }

        test("백그라운드에서 알림 처리가 끝나면 홈 복귀 후 초기화한다") {
            runTest(dispatcher) {
                val ads = FakeAdsController()
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                advanceUntilIdle()
                ads.calls shouldBe 0

                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 1
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("알림 처리가 끝나기 전과 초기화 진행 중 중복 요청은 실행하지 않는다") {
            runTest(dispatcher) {
                val completion = CompletableDeferred<AdInitializationResult>()
                val ads = FakeAdsController { completion.await() }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                runCurrent()
                ads.calls shouldBe 0

                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                runCurrent()
                ads.calls shouldBe 1
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZING
                completion.complete(AdInitializationResult.INITIALIZED)
                advanceUntilIdle()
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("초기화가 보류되기 전 호스트가 다시 연결됐다면 한 번 재시도한다") {
            runTest(dispatcher) {
                val completion = CompletableDeferred<AdInitializationResult>()
                val ads = FakeAdsController { if (calls == 1) completion.await() else AdInitializationResult.INITIALIZED }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                runCurrent()
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                runCurrent()
                completion.complete(AdInitializationResult.DEFERRED)
                advanceUntilIdle()
                ads.calls shouldBe 2
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("실패는 반복하지 않고 다음 호스트 연결에서 재시도한다") {
            runTest(dispatcher) {
                val ads =
                    FakeAdsController {
                        if (calls ==
                            1
                        ) {
                            AdInitializationResult.RETRYABLE_FAILURE
                        } else {
                            AdInitializationResult.INITIALIZED
                        }
                    }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                viewModel.state.value.adInitialization shouldBe AdInitialization.RETRYABLE_FAILURE
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 1
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 2
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("초기화 실패 시 호스트 재연결에도 동의 창을 즉시 반복하지 않는다") {
            runTest(dispatcher) {
                val completion = CompletableDeferred<AdInitializationResult>()
                val ads = FakeAdsController { if (calls == 1) completion.await() else AdInitializationResult.INITIALIZED }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                runCurrent()
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                runCurrent()
                completion.complete(AdInitializationResult.RETRYABLE_FAILURE)
                advanceUntilIdle()
                ads.calls shouldBe 1
                viewModel.state.value.adInitialization shouldBe AdInitialization.RETRYABLE_FAILURE
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 2
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("동의 창이 열려 있으면 닫힌 뒤 초기화를 시작한다") {
            runTest(dispatcher) {
                val ads = FakeAdsController()
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(available = true, formShowing = true))
                advanceUntilIdle()
                ads.calls shouldBe 0
                viewModel.intent(AppStartupIntent.AdHostChanged(available = true, formShowing = false))
                advanceUntilIdle()
                ads.calls shouldBe 1
            }
        }

        test("동의 창을 닫은 결과가 실패여도 창을 곧바로 다시 열지 않는다") {
            runTest(dispatcher) {
                val completion = CompletableDeferred<AdInitializationResult>()
                val ads = FakeAdsController { completion.await() }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(available = true))
                runCurrent()
                viewModel.intent(AppStartupIntent.AdHostChanged(available = true, formShowing = true))
                runCurrent()
                completion.complete(AdInitializationResult.RETRYABLE_FAILURE)
                runCurrent()
                viewModel.intent(AppStartupIntent.AdHostChanged(available = true, formShowing = false))
                advanceUntilIdle()
                ads.calls shouldBe 1
                viewModel.state.value.adInitialization shouldBe AdInitialization.RETRYABLE_FAILURE
            }
        }

        test("초기화 취소 뒤에는 다음 호스트 연결을 기다린다") {
            runTest(dispatcher) {
                val ads = FakeAdsController { if (calls == 1) throw CancellationException() else AdInitializationResult.INITIALIZED }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                viewModel.state.value.adInitialization shouldBe AdInitialization.DEFERRED
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 2
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("초기화 예외는 실패 상태로 남고 다음 호스트 연결에서 재시도한다") {
            runTest(dispatcher) {
                val ads = FakeAdsController { if (calls == 1) error("SDK error") else AdInitializationResult.INITIALIZED }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                viewModel.state.value.adInitialization shouldBe AdInitialization.RETRYABLE_FAILURE
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 2
                viewModel.state.value.adInitialization shouldBe AdInitialization.INITIALIZED
            }
        }

        test("설정이 없는 광고는 복귀할 때 다시 초기화하지 않는다") {
            runTest(dispatcher) {
                val ads = FakeAdsController { AdInitializationResult.DISABLED }
                val viewModel = AppStartupViewModel(ads, ExceptionFilter.None, MutableStateFlow(AdEligibilityState(allowed = true)))
                viewModel.intent(AppStartupIntent.NotificationFinished)
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                viewModel.intent(AppStartupIntent.AdHostChanged(false))
                viewModel.intent(AppStartupIntent.AdHostChanged(true))
                advanceUntilIdle()
                ads.calls shouldBe 1
                viewModel.state.value.adInitialization shouldBe AdInitialization.DISABLED
            }
        }
    }
}

private class FakeAdsController(
    private val initializeAction: suspend FakeAdsController.() -> AdInitializationResult = { AdInitializationResult.INITIALIZED },
) : InterstitialAdController {
    var calls = 0
    override val privacyOptionsRequired = MutableStateFlow(false)

    override suspend fun initialize(): AdInitializationResult {
        calls++
        return initializeAction()
    }

    override fun preload() = Unit

    override suspend fun showIfEligible(evaluatedCardCount: Int) = Unit

    override suspend fun showPrivacyOptions() = Unit
}
