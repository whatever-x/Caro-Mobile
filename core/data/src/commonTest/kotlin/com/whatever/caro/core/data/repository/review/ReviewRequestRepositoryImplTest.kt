package com.whatever.caro.core.data.repository.review

import com.whatever.caro.core.datastore.datasource.LocalReviewRequestDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class ReviewRequestRepositoryImplTest : FunSpec() {
    init {
        test("저장소가 허용하지 않은 예약을 성공으로 바꾸지 않는다") {
            runTest {
                val source = RecordingReviewSource()
                val repository = ReviewRequestRepositoryImpl(source)

                repository.tryReserveRequest(1_777_000_123_456, 2_592_000_000) shouldBe false
                source.arguments shouldBe (1_777_000_123_456L to 2_592_000_000L)
            }
        }

        test("영속 저장 실패를 호출자에게 전달한다") {
            runTest {
                val repository = ReviewRequestRepositoryImpl(RecordingReviewSource(IllegalStateException("disk full")))

                shouldThrow<IllegalStateException> { repository.tryReserveRequest(1_000, 2_592_000_000) }
            }
        }

        test("영속 저장 취소를 성공이나 거부로 바꾸지 않는다") {
            runTest {
                val repository = ReviewRequestRepositoryImpl(RecordingReviewSource(CancellationException("cancelled")))

                shouldThrow<CancellationException> { repository.tryReserveRequest(1_000, 2_592_000_000) }
            }
        }
    }
}

private class RecordingReviewSource(
    private val failure: Throwable? = null,
) : LocalReviewRequestDataSource {
    var arguments: Pair<Long, Long>? = null

    override suspend fun tryReserveRequest(
        nowEpochMilliseconds: Long,
        minimumIntervalMilliseconds: Long,
    ): Boolean {
        failure?.let { throw it }
        arguments = nowEpochMilliseconds to minimumIntervalMilliseconds
        return false
    }
}
