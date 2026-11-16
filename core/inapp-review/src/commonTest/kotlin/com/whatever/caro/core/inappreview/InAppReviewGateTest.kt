package com.whatever.caro.core.inappreview

import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InAppReviewGateTest : FunSpec() {
    init {
        test("서로 다른 gate가 첫 요청과 정확한 30일 경계를 공유한다") {
            runTest {
                val repository = RecordingReviewRepository()
                val clock = ReviewClock(1_000)
                var calls = 0

                suspend fun request() =
                    InAppReviewGate(repository, clock).requestIfAllowed {
                        calls++
                        true
                    }
                request()
                calls shouldBe 1
                clock.epochMilliseconds = 2_592_000_999
                request()
                calls shouldBe 1
                clock.epochMilliseconds = 2_592_001_000
                request()
                calls shouldBe 2
            }
        }

        test("30일이 경과해도 새 요청 이벤트가 없으면 API를 호출하지 않는다") {
            runTest {
                val repository = RecordingReviewRepository()
                val clock = ReviewClock(1_000)
                var calls = 0
                val gate = InAppReviewGate(repository, clock)
                gate.requestIfAllowed {
                    calls++
                    true
                }
                clock.epochMilliseconds += 2_592_000_000
                runCurrent()
                calls shouldBe 1
                repository.lastRequest shouldBe 1_000
                gate.requestIfAllowed {
                    calls++
                    true
                }
                calls shouldBe 2
            }
        }

        test("clock rollback이면 새 요청을 허용하지 않는다") {
            runTest {
                val repository = RecordingReviewRepository()
                val clock = ReviewClock(10_000)
                val gate = InAppReviewGate(repository, clock)
                gate.requestIfAllowed { true }
                clock.epochMilliseconds = 9_000
                var called = false
                gate.requestIfAllowed {
                    called = true
                    true
                }
                called shouldBe false
            }
        }

        test("동시 gate도 저장소의 원자적 예약을 공유한다") {
            runTest {
                val repository = RecordingReviewRepository()
                var calls = 0
                List(20) {
                    async {
                        InAppReviewGate(repository, ReviewClock(10_000)).requestIfAllowed {
                            calls++
                            true
                        }
                    }
                }.awaitAll()
                calls shouldBe 1
            }
        }

        test("플랫폼이 미호출을 반환하면 예약 취소 후 재요청을 허용한다") {
            runTest {
                val repository = RecordingReviewRepository()
                val gate = InAppReviewGate(repository, ReviewClock(10_000))
                gate.requestIfAllowed { false }
                repository.requestRecorded shouldBe false
                repository.cancellations shouldBe 1
                gate.requestIfAllowed { true }
                repository.requestRecorded shouldBe true
                repository.commits shouldBe 1
            }
        }

        test("예약 대기 중 취소되어도 저장 완료 뒤 예약을 복원하고 취소를 전파한다") {
            runTest {
                val release = CompletableDeferred<Unit>()
                val repository = RecordingReviewRepository(onReserve = { release.await() })
                val gate = InAppReviewGate(repository, ReviewClock(10_000))
                var called = false
                val job =
                    launch {
                        gate.requestIfAllowed {
                            called = true
                            true
                        }
                    }
                runCurrent()
                job.cancel()
                release.complete(Unit)
                job.join()
                job.isCancelled shouldBe true
                called shouldBe false
                repository.requestRecorded shouldBe false
                repository.cancellations shouldBe 1
            }
        }

        test("SDK 호출 시각을 확정하며 호출 중 취소되어도 확정 트랜잭션을 마친다") {
            runTest {
                val repository = RecordingReviewRepository()
                val clock = ReviewClock(1_000)
                repository.onReserve = { clock.epochMilliseconds = 2_000 }
                val releaseCommit = CompletableDeferred<Unit>()
                repository.onCommit = { releaseCommit.await() }
                val job = launch { InAppReviewGate(repository, clock).requestIfAllowed { true } }
                runCurrent()
                job.cancel()
                releaseCommit.complete(Unit)
                job.join()
                job.isCancelled shouldBe true
                repository.lastRequest shouldBe 2_000
                repository.commits shouldBe 1
                repository.cancellations shouldBe 0
            }
        }

        test("호출 경계에서 예외가 나면 중복 방지를 위해 확정한다") {
            runTest {
                val repository = RecordingReviewRepository()
                val gate = InAppReviewGate(repository, ReviewClock(10_000))
                shouldThrow<IllegalStateException> { gate.requestIfAllowed { error("SDK threw") } }
                repository.requestRecorded shouldBe true
                repository.commits shouldBe 1
            }
        }

        test("확정 저장 실패도 임시 기록을 취소하여 재요청하게 만들지 않는다") {
            runTest {
                val repository = RecordingReviewRepository()
                repository.onCommit = { error("disk full") }
                shouldThrow<IllegalStateException> {
                    InAppReviewGate(repository, ReviewClock(10_000)).requestIfAllowed { true }
                }
                repository.requestRecorded shouldBe true
                repository.cancellations shouldBe 0
            }
        }

        test("취소 복원 중에도 호출자 취소가 트랜잭션을 중단하지 않는다") {
            runTest {
                val releaseCancel = CompletableDeferred<Unit>()
                val repository = RecordingReviewRepository()
                repository.onCancel = { releaseCancel.await() }
                val job = launch { InAppReviewGate(repository, ReviewClock(10_000)).requestIfAllowed { false } }
                runCurrent()
                job.cancel()
                releaseCancel.complete(Unit)
                job.join()
                repository.requestRecorded shouldBe false
                repository.cancellations shouldBe 1
            }
        }

        test("예약 취소 저장 실패가 원래 코루틴 취소를 대체하지 않는다") {
            runTest {
                val release = CompletableDeferred<Unit>()
                val repository = RecordingReviewRepository(onReserve = { release.await() })
                repository.onCancel = { error("rollback failed") }
                var completion: Throwable? = null
                val job = launch { InAppReviewGate(repository).requestIfAllowed { true } }
                job.invokeOnCompletion { completion = it }
                runCurrent()
                job.cancel()
                release.complete(Unit)
                job.join()
                (completion is CancellationException) shouldBe true
                completion!!.suppressedExceptions.size shouldBe 1
            }
        }

        test("저장소 실패와 취소는 SDK 호출 없이 전파한다") {
            runTest {
                listOf(IllegalStateException("disk failure"), CancellationException("cancelled")).forEach { failure ->
                    val repository = RecordingReviewRepository(failure)
                    var called = false
                    shouldThrow<Exception> {
                        InAppReviewGate(repository, ReviewClock(10_000)).requestIfAllowed {
                            called = true
                            true
                        }
                    }
                    called shouldBe false
                    repository.commits shouldBe 0
                }
            }
        }
    }
}

internal class ReviewClock(
    var epochMilliseconds: Long,
) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMilliseconds)
}

internal class RecordingReviewRepository(
    private val failure: Throwable? = null,
    private val allowed: Boolean = true,
    var onReserve: suspend () -> Unit = {},
) : ReviewRequestRepository {
    private val mutex = Mutex()
    var lastRequest: Long? = null
        private set
    private var pendingId: String? = null
    private var previous: Long? = null
    var reservations = 0
    var commits = 0
    var cancellations = 0
    var onCommit: suspend () -> Unit = {}
    var onCancel: suspend () -> Unit = {}
    val requestRecorded: Boolean get() = lastRequest != null

    override suspend fun tryReserveRequest(
        reservationId: String,
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean {
        failure?.let { throw it }
        val reserved =
            mutex.withLock {
                reservations++
                val last = lastRequest
                val withinInterval =
                    last != null && (nowEpochMilliseconds < last || nowEpochMilliseconds - last < minimumIntervalMilliseconds)
                if (!allowed || withinInterval) {
                    false
                } else {
                    previous = last
                    pendingId = reservationId
                    lastRequest = nowEpochMilliseconds
                    true
                }
            }
        if (reserved) onReserve()
        return reserved
    }

    override suspend fun commitRequest(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long,
    ): Boolean {
        onCommit()
        return mutex.withLock {
            if (pendingId != reservationId) {
                false
            } else {
                lastRequest = attemptedAtEpochMilliseconds
                pendingId = null
                previous = null
                commits++
                true
            }
        }
    }

    override suspend fun cancelRequest(reservationId: String): Boolean {
        onCancel()
        return mutex.withLock {
            if (pendingId != reservationId) {
                false
            } else {
                lastRequest = previous
                pendingId = null
                previous = null
                cancellations++
                true
            }
        }
    }
}
