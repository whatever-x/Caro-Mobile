package com.whatever.caro.core.inappreview

import com.whatever.caro.core.data.repository.review.ReviewRequestRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant

class InAppReviewGateTest : FunSpec() {
    init {
        test("서로 다른 gate가 첫 요청과 정확한 30일 경계를 공유한다") {
            runTest {
                val repository = ReservingRepository()
                val clock = ReviewClock(1_000)
                InAppReviewGate(repository, clock).tryReserve() shouldBe true

                clock.epochMilliseconds = 2_592_000_999
                InAppReviewGate(repository, clock).tryReserve() shouldBe false

                clock.epochMilliseconds = 2_592_001_000
                InAppReviewGate(repository, clock).tryReserve() shouldBe true
            }
        }

        test("clock rollback이면 새 요청을 허용하지 않는다") {
            runTest {
                val repository = ReservingRepository()
                val clock = ReviewClock(10_000)
                val gate = InAppReviewGate(repository, clock)
                gate.tryReserve() shouldBe true
                clock.epochMilliseconds = 9_000
                gate.tryReserve() shouldBe false
            }
        }

        test("동시 gate도 저장소의 원자적 예약을 공유한다") {
            runTest {
                val repository = ReservingRepository()
                val results =
                    List(20) {
                        async { InAppReviewGate(repository, ReviewClock(10_000)).tryReserve() }
                    }.awaitAll()

                results.count { it } shouldBe 1
            }
        }

        test("저장소가 실패하면 예약 성공을 반환하지 않는다") {
            runTest {
                val gate = InAppReviewGate(FailingRepository(IllegalStateException("disk failure")), ReviewClock(10_000))

                shouldThrow<IllegalStateException> { gate.tryReserve() }
            }
        }

        test("저장소 취소를 호출자에게 전파한다") {
            runTest {
                val gate = InAppReviewGate(FailingRepository(CancellationException("cancelled")), ReviewClock(10_000))

                shouldThrow<CancellationException> { gate.tryReserve() }
            }
        }
    }
}

internal class ReviewClock(
    var epochMilliseconds: Long,
) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMilliseconds)
}

private class ReservingRepository : ReviewRequestRepository {
    private val mutex = Mutex()
    private var lastRequest: Long? = null

    override suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean =
        mutex.withLock {
            val previous = lastRequest
            if (previous != null && (nowEpochMilliseconds < previous || nowEpochMilliseconds - previous < minimumIntervalMilliseconds)) {
                false
            } else {
                lastRequest = nowEpochMilliseconds
                true
            }
        }
}

private class FailingRepository(
    private val failure: Throwable,
) : ReviewRequestRepository {
    override suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean = throw failure
}
