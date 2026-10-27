package com.whatever.caro.core.ads

import com.whatever.caro.core.data.repository.ads.InterstitialFrequencyRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInterstitialAdControllerTest : FunSpec() {
    init {
        test("19장 완료는 준비된 광고와 당일 빈도를 소비하지 않는다") {
            runTest {
                val provider = TestInterstitialProvider()
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(19)

                provider.takeReadyCalls shouldBe 0
                provider.showCalls shouldBe 0
                frequency.requestedDates shouldBe emptyList()
            }
        }

        test("20장 완료는 준비된 광고를 한 번 표시하고 같은 날 후속 완료는 당일 제한을 따른다") {
            runTest {
                val provider = TestInterstitialProvider()
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)
                provider.ready = true
                controller.showIfEligible(30)

                provider.showCalls shouldBe 1
                frequency.confirmed shouldBe listOf("reservation-1")
                frequency.released shouldBe emptyList()
                frequency.consumedDate shouldBe TODAY
            }
        }

        test("광고가 준비되지 않았으면 당일 예약을 만들지 않는다") {
            runTest {
                val provider = TestInterstitialProvider().apply { ready = false }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)

                provider.showCalls shouldBe 0
                frequency.requestedDates shouldBe emptyList()
                frequency.consumedDate shouldBe null
            }
        }

        test("같은 기기에서 이미 소비한 날짜는 새 controller에서도 표시하지 않는다") {
            runTest {
                val frequency = TestInterstitialFrequency()
                val first = DefaultInterstitialAdController(TestInterstitialProvider(), frequency) { TODAY }
                first.showIfEligible(20)
                val provider = TestInterstitialProvider()

                DefaultInterstitialAdController(provider, frequency) { TODAY }.showIfEligible(20)

                provider.showCalls shouldBe 0
                frequency.confirmed shouldBe listOf("reservation-1")
                frequency.released shouldBe emptyList()
            }
        }

        test("광고 표시가 끝나기 전 동시 완료 요청은 두 번째 예약과 표시를 만들지 않는다") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val dismiss = CompletableDeferred<Unit>()
                val provider =
                    TestInterstitialProvider().apply {
                        showAction = { onShown ->
                            onShown()
                            started.complete(Unit)
                            dismiss.await()
                            true
                        }
                    }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }
                val first = launch { controller.showIfEligible(20) }
                started.await()

                controller.showIfEligible(20)
                provider.takeReadyCalls shouldBe 1
                frequency.requestedDates shouldBe listOf(TODAY)

                dismiss.complete(Unit)
                first.join()
                provider.showCalls shouldBe 1
                frequency.confirmed shouldBe listOf("reservation-1")
            }
        }

        test("SDK가 표시 전 실패를 확정하면 예약을 반납해 같은 날 다시 시도할 수 있다") {
            runTest {
                val provider = TestInterstitialProvider().apply { showAction = { false } }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)

                frequency.released shouldBe listOf("reservation-1")
                frequency.confirmed shouldBe emptyList()
                frequency.consumedDate shouldBe null

                provider.ready = true
                provider.showAction = { onShown ->
                    onShown()
                    true
                }
                controller.showIfEligible(20)
                frequency.confirmed shouldBe listOf("reservation-2")
                provider.showCalls shouldBe 2
            }
        }

        test("실제 노출 callback 또는 성공한 표시 결과가 있으면 당일 소비를 확정한다") {
            runTest {
                val provider = TestInterstitialProvider().apply { showAction = { true } }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)
                frequency.confirmed shouldBe listOf("reservation-1")
                frequency.released shouldBe emptyList()

                val callbackProvider =
                    TestInterstitialProvider().apply {
                        showAction = { onShown ->
                            onShown()
                            false
                        }
                    }
                val callbackFrequency = TestInterstitialFrequency()
                DefaultInterstitialAdController(callbackProvider, callbackFrequency) { TODAY }.showIfEligible(20)

                callbackFrequency.confirmed shouldBe listOf("reservation-1")
                callbackFrequency.released shouldBe emptyList()
            }
        }

        test("SDK 결과가 불확실한 예외는 학습 완료로 돌아가되 예약을 유지한다") {
            runTest {
                val provider =
                    TestInterstitialProvider().apply {
                        showAction = { error("presentation result lost") }
                    }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)
                provider.ready = true
                controller.showIfEligible(20)

                provider.showCalls shouldBe 1
                frequency.consumedDate shouldBe TODAY
                frequency.confirmed shouldBe emptyList()
                frequency.released shouldBe emptyList()
            }
        }

        test("노출 callback 뒤 SDK 예외가 나도 예약을 반납하지 않고 표시를 확정한다") {
            runTest {
                val provider =
                    TestInterstitialProvider().apply {
                        showAction = { onShown ->
                            onShown()
                            error("dismiss callback lost")
                        }
                    }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)

                frequency.confirmed shouldBe listOf("reservation-1")
                frequency.released shouldBe emptyList()
                frequency.consumedDate shouldBe TODAY
            }
        }

        test("표시 중 취소는 전파하고 미확정 예약의 다음 날을 차단한 뒤 다시 허용한다") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val provider =
                    TestInterstitialProvider().apply {
                        showAction = {
                            started.complete(Unit)
                            awaitCancellation()
                        }
                    }
                val frequency = TestInterstitialFrequency()
                var date = TODAY
                val controller = DefaultInterstitialAdController(provider, frequency) { date }
                var returnedNormally = false
                val showing =
                    launch {
                        controller.showIfEligible(20)
                        returnedNormally = true
                    }
                started.await()

                showing.cancelAndJoin()

                returnedNormally shouldBe false
                frequency.consumedDate shouldBe TODAY
                frequency.confirmed shouldBe emptyList()
                frequency.released shouldBe emptyList()

                date = "2026-10-28"
                provider.ready = true
                controller.showIfEligible(20)
                frequency.requestedDates shouldBe listOf(TODAY, "2026-10-28")
                provider.showCalls shouldBe 1

                date = "2026-10-29"
                provider.ready = true
                provider.showAction = { onShown ->
                    onShown()
                    true
                }
                controller.showIfEligible(20)
                frequency.confirmed shouldBe listOf("reservation-2")
            }
        }

        test("예약 반환 전에 날짜가 바뀌면 표시하지 않고 예약을 반납한다") {
            runTest {
                var date = TODAY
                val provider = TestInterstitialProvider()
                val frequency =
                    TestInterstitialFrequency().apply {
                        afterReserve = { date = "2026-10-28" }
                    }
                val controller = DefaultInterstitialAdController(provider, frequency) { date }

                controller.showIfEligible(20)

                provider.showCalls shouldBe 0
                frequency.requestedDates shouldBe listOf(TODAY)
                frequency.released shouldBe listOf("reservation-1")
                frequency.confirmed shouldBe emptyList()
                frequency.consumedDate shouldBe null
            }
        }

        test("SDK 최초 노출 callback의 현지 날짜를 닫힘 시점과 구분해 저장한다") {
            runTest {
                var date = TODAY
                val provider =
                    TestInterstitialProvider().apply {
                        showAction = { onShown ->
                            date = "2026-10-28"
                            onShown()
                            date = "2026-10-29"
                            onShown()
                            true
                        }
                    }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { date }

                controller.showIfEligible(20)

                frequency.confirmedDates shouldBe listOf("2026-10-28")
                frequency.consumedDate shouldBe "2026-10-28"
                frequency.released shouldBe emptyList()
            }
        }

        test("privacy 창을 표시하는 동안 전면광고를 가져오거나 예약하지 않는다") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val dismiss = CompletableDeferred<Unit>()
                val provider =
                    TestInterstitialProvider().apply {
                        privacyAction = {
                            started.complete(Unit)
                            dismiss.await()
                        }
                    }
                val frequency = TestInterstitialFrequency()
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }
                val privacy = launch { controller.showPrivacyOptions() }
                started.await()

                controller.showIfEligible(20)

                provider.takeReadyCalls shouldBe 0
                frequency.requestedDates shouldBe emptyList()
                dismiss.complete(Unit)
                privacy.join()

                controller.showIfEligible(20)
                provider.showCalls shouldBe 1
            }
        }

        test("초기화 중 개인정보 설정 요청은 초기화가 끝난 뒤 처리한다") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val finish = CompletableDeferred<Unit>()
                val provider =
                    TestInterstitialProvider().apply {
                        initializeAction = {
                            started.complete(Unit)
                            finish.await()
                            AdInitializationResult.INITIALIZED
                        }
                    }
                val controller = DefaultInterstitialAdController(provider, TestInterstitialFrequency()) { TODAY }
                val initializing = launch { controller.initialize() }
                started.await()
                val privacy = launch { controller.showPrivacyOptions() }
                runCurrent()
                controller.showPrivacyOptions()
                provider.privacyCalls shouldBe 0

                finish.complete(Unit)
                initializing.join()
                privacy.join()
                provider.privacyCalls shouldBe 1
            }
        }

        test("전면광고 표시 중에는 privacy 창을 겹쳐 띄우지 않는다") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val dismiss = CompletableDeferred<Unit>()
                val provider =
                    TestInterstitialProvider().apply {
                        showAction = { onShown ->
                            onShown()
                            started.complete(Unit)
                            dismiss.await()
                            true
                        }
                    }
                val controller = DefaultInterstitialAdController(provider, TestInterstitialFrequency()) { TODAY }
                val showing = launch { controller.showIfEligible(20) }
                started.await()

                val privacy = launch { controller.showPrivacyOptions() }
                runCurrent()
                provider.privacyCalls shouldBe 0

                dismiss.complete(Unit)
                showing.join()
                privacy.join()
                provider.privacyCalls shouldBe 1
            }
        }

        test("빈도 저장 오류는 표시하지 않고 학습으로 돌아가며 다음 시도의 잠금을 막지 않는다") {
            runTest {
                val provider = TestInterstitialProvider()
                val frequency = TestInterstitialFrequency().apply { reserveFailure = IllegalStateException("disk full") }
                val controller = DefaultInterstitialAdController(provider, frequency) { TODAY }

                controller.showIfEligible(20)
                provider.showCalls shouldBe 0
                frequency.consumedDate shouldBe null

                frequency.reserveFailure = null
                provider.ready = true
                controller.showIfEligible(20)
                provider.showCalls shouldBe 1
            }
        }
    }
}

private const val TODAY = "2026-10-27"

private class TestInterstitialProvider : NativeInterstitialProvider {
    override val privacyOptionsRequired = MutableStateFlow(false)
    var ready = true
    var takeReadyCalls = 0
    var showCalls = 0
    var privacyCalls = 0
    var showAction: suspend (onShown: () -> Unit) -> Boolean = { onShown ->
        onShown()
        true
    }
    var privacyAction: suspend () -> Unit = {}
    var initializeAction: suspend () -> AdInitializationResult = { AdInitializationResult.INITIALIZED }

    override suspend fun initialize() = initializeAction()

    override fun preload() = Unit

    override fun takeReadyAd(): ReadyInterstitial? {
        takeReadyCalls++
        if (!ready) return null
        ready = false
        val action = showAction
        return ReadyInterstitial { onShown ->
            showCalls++
            action(onShown)
        }
    }

    override suspend fun showPrivacyOptions() {
        privacyCalls++
        privacyAction()
    }
}

private class TestInterstitialFrequency : InterstitialFrequencyRepository {
    var consumedDate: String? = null
    private var reservation: String? = null
    private var nextReservationId = 0
    var reserveFailure: Exception? = null
    var afterReserve: () -> Unit = {}
    val requestedDates = mutableListOf<String>()
    val confirmed = mutableListOf<String>()
    val confirmedDates = mutableListOf<String>()
    val released = mutableListOf<String>()

    override suspend fun reserve(localDate: String): String? {
        requestedDates += localDate
        reserveFailure?.let { throw it }
        val consumed = consumedDate?.let { LocalDate.parse(it) }
        val blockedThrough = if (reservation != null) consumed?.plus(DatePeriod(days = 1)) else consumed
        if (blockedThrough != null && LocalDate.parse(localDate) <= blockedThrough) return null
        consumedDate = localDate
        return "reservation-${++nextReservationId}".also {
            reservation = it
            afterReserve()
        }
    }

    override suspend fun confirmShown(
        reservationId: String,
        localDate: String,
    ) {
        confirmed += reservationId
        confirmedDates += localDate
        if (reservation == reservationId) {
            consumedDate = localDate
            reservation = null
        }
    }

    override suspend fun release(reservationId: String) {
        released += reservationId
        if (reservation == reservationId) {
            reservation = null
            consumedDate = null
        }
    }
}
