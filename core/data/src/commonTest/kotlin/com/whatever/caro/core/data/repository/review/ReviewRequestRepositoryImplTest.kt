package com.whatever.caro.core.data.repository.review

import com.whatever.caro.core.datastore.datasource.LocalReviewRequestDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class ReviewRequestRepositoryImplTest : FunSpec() {
    init {
        test("예약의 식별자와 시각 간격 및 거부 결과를 그대로 위임한다") {
            runTest {
                val source = RecordingReviewSource()
                val repository = ReviewRequestRepositoryImpl(source)
                repository.tryReserveRequest("id", 1_777_000_123_456, 2_592_000_000) shouldBe false
                source.arguments shouldBe Triple("id", 1_777_000_123_456L, 2_592_000_000L)
            }
        }

        test("확정과 취소는 같은 식별자로 위임하고 소유권 검사 결과를 보존한다") {
            runTest {
                val source = RecordingReviewSource()
                val repository = ReviewRequestRepositoryImpl(source)
                repository.commitRequest("commit", 2_000) shouldBe true
                source.committed shouldBe ("commit" to 2_000L)
                repository.cancelRequest("cancel") shouldBe false
                source.cancelled shouldBe "cancel"
            }
        }

        test("예약 확정 취소의 저장 실패를 호출자에게 전달한다") {
            runTest {
                val repository = ReviewRequestRepositoryImpl(RecordingReviewSource(IllegalStateException("disk full")))
                shouldThrow<IllegalStateException> { repository.tryReserveRequest("id", 1_000, 2_592_000_000) }
                shouldThrow<IllegalStateException> { repository.commitRequest("id", 1_000) }
                shouldThrow<IllegalStateException> { repository.cancelRequest("id") }
            }
        }

        test("예약 확정 취소의 저장 취소를 성공이나 거부로 바꾸지 않는다") {
            runTest {
                val repository = ReviewRequestRepositoryImpl(RecordingReviewSource(CancellationException("cancelled")))
                shouldThrow<CancellationException> { repository.tryReserveRequest("id", 1_000, 2_592_000_000) }
                shouldThrow<CancellationException> { repository.commitRequest("id", 1_000) }
                shouldThrow<CancellationException> { repository.cancelRequest("id") }
            }
        }
    }
}

private class RecordingReviewSource(
    private val failure: Throwable? = null,
) : LocalReviewRequestDataSource {
    var arguments: Triple<String, Long, Long>? = null
    var committed: Pair<String, Long>? = null
    var cancelled: String? = null

    override suspend fun tryReserveRequest(
        reservationId: String,
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean {
        failure?.let { throw it }
        arguments = Triple(reservationId, nowEpochMilliseconds, minimumIntervalMilliseconds)
        return false
    }

    override suspend fun commitRequest(
        reservationId: String,
        attemptedAtEpochMilliseconds: Long,
    ): Boolean {
        failure?.let { throw it }
        committed = reservationId to attemptedAtEpochMilliseconds
        return true
    }

    override suspend fun cancelRequest(reservationId: String): Boolean {
        failure?.let { throw it }
        cancelled = reservationId
        return false
    }
}
