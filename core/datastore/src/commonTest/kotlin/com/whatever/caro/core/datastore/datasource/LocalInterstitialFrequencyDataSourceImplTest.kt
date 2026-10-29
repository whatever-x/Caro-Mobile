package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okio.IOException

class LocalInterstitialFrequencyDataSourceImplTest : FunSpec() {
    init {
        test("같은 기기의 동시 예약은 한 개만 허용한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                val reservations =
                    List(10) {
                        async {
                            LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-27")
                        }
                    }.awaitAll()

                reservations.count { it != null } shouldBe 1
            }
        }

        test("표시를 확정하기 전 source를 다시 만들어도 당일 예약을 유지한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-27").shouldNotBeNull()

                LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-27") shouldBe null
            }
        }

        test("다른 token으로 확정하거나 반납해도 현재 예약을 유지한다") {
            runTest {
                val source = LocalInterstitialFrequencyDataSourceImpl(InterstitialMemoryDataStore())
                val token = source.reserve("2026-10-27").shouldNotBeNull()

                source.confirmShown("another-token", "2026-10-27")
                source.release("another-token")
                source.reserve("2026-10-27") shouldBe null

                source.release(token)
                source.reserve("2026-10-27").shouldNotBeNull()
            }
        }

        test("표시 전 실패 예약을 반납하면 재예약하고 이전 token으로는 반납할 수 없다") {
            runTest {
                val source = LocalInterstitialFrequencyDataSourceImpl(InterstitialMemoryDataStore())
                val firstToken = source.reserve("2026-10-27").shouldNotBeNull()
                source.release(firstToken)
                val nextToken = source.reserve("2026-10-27").shouldNotBeNull()
                nextToken shouldNotBe firstToken

                source.release(firstToken)
                source.reserve("2026-10-27") shouldBe null
            }
        }

        test("표시 확정은 반복해도 안전하고 이후 반납으로 당일 제한을 해제할 수 없다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                val source = LocalInterstitialFrequencyDataSourceImpl(store)
                val token = source.reserve("2026-10-27").shouldNotBeNull()

                source.confirmShown(token, "2026-10-27")
                source.confirmShown(token, "2026-10-27")
                source.release(token)

                LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-27") shouldBe null
            }
        }

        test("다음 날 예약을 허용하고 이전 날의 callback은 새 예약에 영향을 주지 않는다") {
            runTest {
                val source = LocalInterstitialFrequencyDataSourceImpl(InterstitialMemoryDataStore())
                val oldToken = source.reserve("2026-10-27").shouldNotBeNull()
                source.confirmShown(oldToken, "2026-10-27")
                val nextToken = source.reserve("2026-10-28").shouldNotBeNull()
                nextToken shouldNotBe oldToken

                source.confirmShown(oldToken, "2026-10-27")
                source.release(oldToken)
                source.reserve("2026-10-28") shouldBe null

                source.release(nextToken)
                source.reserve("2026-10-28").shouldNotBeNull()
            }
        }

        test("인증 정보를 지워도 예약과 표시 확정 기록은 기기에 유지한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                val source = LocalInterstitialFrequencyDataSourceImpl(store)
                val auth = LocalAuthDataSourceImpl(store)
                auth.saveTokens("access", "refresh")
                auth.saveRegistrationComplete(true)
                val token = source.reserve("2026-10-27").shouldNotBeNull()

                auth.clear()
                auth.fetchAccessToken() shouldBe null
                source.reserve("2026-10-27") shouldBe null

                source.confirmShown(token, "2026-10-27")
                auth.clear()
                LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-27") shouldBe null
            }
        }

        test("예약 뒤 자정을 지나 표시됐으면 실제 표시 날짜를 소비하고 그다음 날 허용한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                val source = LocalInterstitialFrequencyDataSourceImpl(store)
                val token = source.reserve("2026-10-27").shouldNotBeNull()

                source.confirmShown(token, "2026-10-28")

                LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-28") shouldBe null
                source.reserve("2026-10-29").shouldNotBeNull()
            }
        }

        test("자정 직후 표시 후 앱이 종료됐을 수 있는 미확정 예약은 다음 날도 차단한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                LocalInterstitialFrequencyDataSourceImpl(store).reserve("2026-10-27").shouldNotBeNull()
                val recreated = LocalInterstitialFrequencyDataSourceImpl(store)

                recreated.reserve("2026-10-28") shouldBe null
                recreated.reserve("2026-10-29").shouldNotBeNull()
            }
        }

        test("연말 미확정 예약은 달과 연도가 바뀌어도 다음 날만 추가 차단한다") {
            runTest {
                val source = LocalInterstitialFrequencyDataSourceImpl(InterstitialMemoryDataStore())
                source.reserve("2026-12-31").shouldNotBeNull()

                source.reserve("2027-01-01") shouldBe null
                source.reserve("2027-01-02").shouldNotBeNull()
            }
        }

        test("확정된 노출 날짜보다 과거 날짜로 돌아가도 소비 기록을 덮어쓰지 않는다") {
            runTest {
                val source = LocalInterstitialFrequencyDataSourceImpl(InterstitialMemoryDataStore())
                val token = source.reserve("2026-10-27").shouldNotBeNull()
                source.confirmShown(token, "2026-10-27")

                source.reserve("2026-10-26") shouldBe null
                source.reserve("2026-10-27") shouldBe null
                source.reserve("2026-10-28").shouldNotBeNull()
            }
        }

        test("저장 날짜가 잘못됐으면 예약을 허용하거나 기존 값을 덮어쓰지 않는다") {
            runTest {
                val corrupted =
                    mutablePreferencesOf(
                        stringPreferencesKey("interstitial_consumed_local_date") to "not-a-date",
                        stringPreferencesKey("interstitial_reservation_id") to "pending-token",
                    )
                val store = InterstitialMemoryDataStore(corrupted)
                val source = LocalInterstitialFrequencyDataSourceImpl(store)

                shouldThrow<IllegalArgumentException> { source.reserve("2026-10-28") }
                store.data.value shouldBe corrupted
            }
        }

        test("날짜가 없는 미확정 token은 새 예약으로 대체하지 않는다") {
            runTest {
                val corrupted =
                    mutablePreferencesOf(
                        stringPreferencesKey("interstitial_reservation_id") to "pending-token",
                    )
                val store = InterstitialMemoryDataStore(corrupted)
                val source = LocalInterstitialFrequencyDataSourceImpl(store)

                shouldThrow<IllegalStateException> { source.reserve("2026-10-28") }
                store.data.value shouldBe corrupted
            }
        }

        test("저장 실패는 예약을 만들지 않고 오류를 전파한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                val source = LocalInterstitialFrequencyDataSourceImpl(store)
                store.failure = IOException("disk full")

                shouldThrow<IOException> { source.reserve("2026-10-27") }

                store.failure = null
                source.reserve("2026-10-27").shouldNotBeNull()
            }
        }

        test("표시 확정과 반납의 저장 실패 및 취소는 기존 예약을 유지한다") {
            runTest {
                val store = InterstitialMemoryDataStore()
                val source = LocalInterstitialFrequencyDataSourceImpl(store)
                val token = source.reserve("2026-10-27").shouldNotBeNull()
                store.failure = IOException("disk full")
                shouldThrow<IOException> { source.confirmShown(token, "2026-10-27") }
                shouldThrow<IOException> { source.release(token) }

                store.failure = CancellationException("cancel write")
                shouldThrow<CancellationException> { source.release(token) }
                store.failure = null
                source.reserve("2026-10-27") shouldBe null
                source.release(token)
                source.reserve("2026-10-27").shouldNotBeNull()
            }
        }
    }
}

private class InterstitialMemoryDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    override val data = MutableStateFlow(initial)
    var failure: Throwable? = null
    private val mutex = Mutex()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock {
            yield()
            failure?.let { throw it }
            transform(data.value).also { data.value = it }
        }
}
