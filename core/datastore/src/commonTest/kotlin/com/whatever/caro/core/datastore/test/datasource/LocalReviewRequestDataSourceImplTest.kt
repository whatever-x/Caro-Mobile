package com.whatever.caro.core.datastore.test.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSourceImpl
import com.whatever.caro.core.datastore.datasource.LocalReviewRequestDataSourceImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okio.IOException

class LocalReviewRequestDataSourceImplTest : FunSpec() {
    init {
        test("첫 예약을 저장하고 새 DataSource도 직전 예약을 공유한다") {
            runTest {
                val store = TransactionalPreferencesStore()
                LocalReviewRequestDataSourceImpl(store).tryReserveRequest(10_000, 1_000) shouldBe true
                LocalReviewRequestDataSourceImpl(store).tryReserveRequest(10_999, 1_000) shouldBe false
                LocalReviewRequestDataSourceImpl(store).tryReserveRequest(11_000, 1_000) shouldBe true
            }
        }

        test("시계가 뒤로 이동하면 예약을 차단하고 이전 시각을 보존한다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore())
                source.tryReserveRequest(10_000, 1_000) shouldBe true
                source.tryReserveRequest(9_000, 1_000) shouldBe false
                source.tryReserveRequest(10_999, 1_000) shouldBe false
                source.tryReserveRequest(11_000, 1_000) shouldBe true
            }
        }

        test("서로 다른 DataSource의 동시 호출도 한 번만 예약한다") {
            runTest {
                val store = TransactionalPreferencesStore()
                val results =
                    List(32) {
                        async {
                            LocalReviewRequestDataSourceImpl(store).tryReserveRequest(10_000, 1_000)
                        }
                    }.awaitAll()

                results.count { it } shouldBe 1
            }
        }

        test("로그아웃으로 인증 정보를 지워도 앱 전체 리뷰 예약은 보존한다") {
            runTest {
                val store = TransactionalPreferencesStore()
                val source = LocalReviewRequestDataSourceImpl(store)
                val auth = LocalAuthDataSourceImpl(store)
                auth.saveTokens("access", "refresh")
                source.tryReserveRequest(10_000, 1_000) shouldBe true

                auth.clear()

                auth.fetchAccessToken() shouldBe null
                source.tryReserveRequest(10_001, 1_000) shouldBe false
            }
        }

        test("저장 실패를 성공 예약으로 취급하지 않는다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore(IOException("disk full")))

                shouldThrow<IOException> { source.tryReserveRequest(10_000, 1_000) }
            }
        }

        test("저장 작업의 취소를 호출자에게 전파한다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore(CancellationException("cancelled")))

                shouldThrow<CancellationException> { source.tryReserveRequest(10_000, 1_000) }
            }
        }
    }
}

private class TransactionalPreferencesStore(
    private val failure: Throwable? = null,
) : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        // Let concurrent callers reach the transaction boundary before serializing writes.
        yield()
        return mutex.withLock {
            failure?.let { throw it }
            transform(state.value).also { state.value = it }
        }
    }
}
