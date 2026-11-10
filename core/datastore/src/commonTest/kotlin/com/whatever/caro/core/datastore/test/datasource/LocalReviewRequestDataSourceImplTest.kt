package com.whatever.caro.core.datastore.test.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
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
                val source = LocalReviewRequestDataSourceImpl(store)
                source.tryReserveRequest("first", 10_000, 1_000) shouldBe true
                source.commitRequest("first", 10_000) shouldBe true
                LocalReviewRequestDataSourceImpl(store).tryReserveRequest("reservation", 10_999, 1_000) shouldBe false
                LocalReviewRequestDataSourceImpl(store).tryReserveRequest("reservation", 11_000, 1_000) shouldBe true
            }
        }

        test("시계가 뒤로 이동하면 예약을 차단하고 이전 시각을 보존한다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore())
                source.tryReserveRequest("reservation", 10_000, 1_000) shouldBe true
                source.tryReserveRequest("reservation", 9_000, 1_000) shouldBe false
                source.tryReserveRequest("reservation", 10_999, 1_000) shouldBe false
                source.tryReserveRequest("reservation", 11_000, 1_000) shouldBe true
            }
        }

        test("기존 버전의 마지막 요청 키는 유지하고 취소 시에도 복원한다") {
            runTest {
                val store = TransactionalPreferencesStore()
                store.edit { it[longPreferencesKey("last_in_app_review_request_epoch_milliseconds")] = 10_000 }
                val source = LocalReviewRequestDataSourceImpl(store)
                source.tryReserveRequest("blocked", 10_999, 1_000) shouldBe false
                source.tryReserveRequest("owner", 11_000, 1_000) shouldBe true
                source.cancelRequest("owner") shouldBe true
                source.tryReserveRequest("still-blocked", 10_999, 1_000) shouldBe false
                source.tryReserveRequest("next", 11_000, 1_000) shouldBe true
            }
        }

        test("서로 다른 DataSource의 동시 호출도 한 번만 예약한다") {
            runTest {
                val store = TransactionalPreferencesStore()
                val results =
                    List(32) {
                        async {
                            LocalReviewRequestDataSourceImpl(store).tryReserveRequest("reservation-$it", 10_000, 1_000)
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
                source.tryReserveRequest("reservation", 10_000, 1_000) shouldBe true
                source.commitRequest("reservation", 10_000) shouldBe true

                auth.clear()

                auth.fetchAccessToken() shouldBe null
                source.tryReserveRequest("reservation", 10_001, 1_000) shouldBe false
            }
        }

        test("취소는 이전 확정 시각을 복원하고 최초 예약은 기록을 제거한다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore())
                source.tryReserveRequest("first", 10_000, 1_000) shouldBe true
                source.cancelRequest("first") shouldBe true
                source.tryReserveRequest("second", 10_000, 1_000) shouldBe true
                source.commitRequest("second", 10_100) shouldBe true
                source.tryReserveRequest("third", 11_100, 1_000) shouldBe true
                source.cancelRequest("third") shouldBe true
                source.tryReserveRequest("fourth", 11_099, 1_000) shouldBe false
                source.tryReserveRequest("fifth", 11_100, 1_000) shouldBe true
            }
        }

        test("다른 예약 ID와 확정 이후의 늦은 취소는 기록을 바꾸지 않는다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore())
                source.tryReserveRequest("owner", 10_000, 1_000) shouldBe true
                source.cancelRequest("other") shouldBe false
                source.commitRequest("other", 20_000) shouldBe false
                source.commitRequest("owner", 10_100) shouldBe true
                source.cancelRequest("owner") shouldBe false
                source.commitRequest("owner", 20_000) shouldBe false
                source.tryReserveRequest("next", 11_099, 1_000) shouldBe false
                source.tryReserveRequest("next", 11_100, 1_000) shouldBe true
            }
        }

        test("프로세스가 남긴 임시 기록은 간격 이후 교체되며 오래된 취소가 새 예약을 되돌리지 않는다") {
            runTest {
                val store = TransactionalPreferencesStore()
                val previous = LocalReviewRequestDataSourceImpl(store)
                previous.tryReserveRequest("orphan", 10_000, 1_000) shouldBe true
                val restarted = LocalReviewRequestDataSourceImpl(store)
                restarted.tryReserveRequest("new", 10_999, 1_000) shouldBe false
                restarted.tryReserveRequest("new", 11_000, 1_000) shouldBe true
                previous.cancelRequest("orphan") shouldBe false
                previous.commitRequest("orphan", 12_000) shouldBe false
                restarted.cancelRequest("new") shouldBe true
                restarted.tryReserveRequest("retry", 10_999, 1_000) shouldBe false
                restarted.tryReserveRequest("retry", 11_000, 1_000) shouldBe true
            }
        }

        test("확정과 취소가 경합해도 하나의 소유권 변경만 성공한다") {
            runTest {
                repeat(20) { index ->
                    val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore())
                    val id = "race-$index"
                    source.tryReserveRequest(id, 10_000, 1_000) shouldBe true
                    val committed = async { source.commitRequest(id, 10_100) }
                    val cancelled = async { source.cancelRequest(id) }
                    listOf(committed.await(), cancelled.await()).count { it } shouldBe 1
                    source.tryReserveRequest("after", 10_101, 1_000) shouldBe !committed.await()
                }
            }
        }

        test("저장 실패를 성공 예약으로 취급하지 않는다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore(IOException("disk full")))

                shouldThrow<IOException> { source.tryReserveRequest("reservation", 10_000, 1_000) }
            }
        }

        test("저장 작업의 취소를 호출자에게 전파한다") {
            runTest {
                val source = LocalReviewRequestDataSourceImpl(TransactionalPreferencesStore(CancellationException("cancelled")))

                shouldThrow<CancellationException> { source.tryReserveRequest("reservation", 10_000, 1_000) }
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
