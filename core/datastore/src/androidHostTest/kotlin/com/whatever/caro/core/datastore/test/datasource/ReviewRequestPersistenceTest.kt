package com.whatever.caro.core.datastore.test.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.whatever.caro.core.datastore.datasource.LocalReviewRequestDataSourceImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.nio.file.Files

class ReviewRequestPersistenceTest : FunSpec() {
    init {
        test("프로세스의 저장 객체를 재구성해도 디스크에 기록한 요청 간격을 보존한다") {
            runTest {
                val directory = Files.createTempDirectory("review-request").toFile()
                val file = directory.resolve("review.preferences_pb")
                val firstJob = SupervisorJob()
                val secondJob = SupervisorJob()
                try {
                    val firstStore =
                        PreferenceDataStoreFactory.createWithPath(
                            scope = CoroutineScope(firstJob + Dispatchers.IO),
                            produceFile = { file.absolutePath.toPath() },
                        )
                    val firstSource = LocalReviewRequestDataSourceImpl(firstStore)
                    firstSource.tryReserveRequest("first", 10_000, 1_000) shouldBe true
                    firstSource.commitRequest("first", 10_100) shouldBe true
                    firstJob.cancelAndJoin()

                    val reopenedStore =
                        PreferenceDataStoreFactory.createWithPath(
                            scope = CoroutineScope(secondJob + Dispatchers.IO),
                            produceFile = { file.absolutePath.toPath() },
                        )
                    val reopenedSource = LocalReviewRequestDataSourceImpl(reopenedStore)
                    reopenedSource.tryReserveRequest("next", 11_099, 1_000) shouldBe false
                    reopenedSource.tryReserveRequest("next", 11_100, 1_000) shouldBe true
                } finally {
                    firstJob.cancelAndJoin()
                    secondJob.cancelAndJoin()
                    directory.deleteRecursively()
                }
            }
        }

        test("실제 DataStore의 동시 edit 트랜잭션에서 하나의 예약만 성공한다") {
            runTest {
                val directory = Files.createTempDirectory("review-concurrent").toFile()
                val job = SupervisorJob()
                try {
                    val store =
                        PreferenceDataStoreFactory.createWithPath(
                            scope = CoroutineScope(job + Dispatchers.IO),
                            produceFile = { directory.resolve("review.preferences_pb").absolutePath.toPath() },
                        )
                    val results =
                        List(32) {
                            async(Dispatchers.Default) {
                                LocalReviewRequestDataSourceImpl(store).tryReserveRequest("reservation-$it", 10_000, 1_000)
                            }
                        }.awaitAll()

                    results.count { it } shouldBe 1
                } finally {
                    job.cancelAndJoin()
                    directory.deleteRecursively()
                }
            }
        }
    }
}
