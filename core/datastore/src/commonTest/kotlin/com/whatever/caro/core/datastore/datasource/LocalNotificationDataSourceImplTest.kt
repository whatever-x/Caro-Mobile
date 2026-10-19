package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okio.IOException

class LocalNotificationDataSourceImplTest : FunSpec() {
    init {
        test("기기 선택은 미설정과 OFF를 구분하고 인증 삭제 및 재생성 후 유지한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                val source = LocalNotificationDataSourceImpl(store)
                source.observeNotificationEnabled().first() shouldBe null
                source.saveNotificationEnabled(false)
                LocalAuthDataSourceImpl(store).clear()
                LocalNotificationDataSourceImpl(store).observeNotificationEnabled().first() shouldBe false
                source.saveNotificationEnabled(true)
                source.observeNotificationEnabled().first() shouldBe true
            }
        }

        test("마지막 등록 토큰은 재생성 후 조회하고 삭제 후 정리한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                val source = LocalNotificationDataSourceImpl(store)
                source.fetchRegisteredToken() shouldBe null
                source.saveRegisteredToken("registered")
                LocalNotificationDataSourceImpl(store).fetchRegisteredToken() shouldBe "registered"
                source.saveRegisteredToken(null)
                source.fetchRegisteredToken() shouldBe null
            }
        }

        test("선택 저장 실패는 이전 OFF를 보존한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                val source = LocalNotificationDataSourceImpl(store)
                source.saveNotificationEnabled(false)
                store.failure = IOException("disk full")
                shouldThrow<IOException> { source.saveNotificationEnabled(true) }
                source.observeNotificationEnabled().first() shouldBe false
            }
        }

        test("최초 요청을 소비하면 source 재생성 후에도 다시 요청하지 않는다") {
            runTest {
                val store = NotificationMemoryDataStore()
                LocalNotificationDataSourceImpl(store).consumeInitialPermissionRequest() shouldBe true
                LocalNotificationDataSourceImpl(store).consumeInitialPermissionRequest() shouldBe false
            }
        }

        test("인증 정보를 지워도 최초 요청 기록은 유지한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                val auth = LocalAuthDataSourceImpl(store)
                auth.saveTokens("access", "refresh")
                auth.saveRegistrationComplete(true)
                val source = LocalNotificationDataSourceImpl(store)
                source.consumeInitialPermissionRequest() shouldBe true

                auth.clear()

                auth.fetchAccessToken() shouldBe null
                source.consumeInitialPermissionRequest() shouldBe false
            }
        }

        test("동시 최초 요청은 하나만 허용한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                val results =
                    List(10) {
                        async { LocalNotificationDataSourceImpl(store).consumeInitialPermissionRequest() }
                    }.awaitAll()

                results.count { it } shouldBe 1
            }
        }

        test("최초 요청 기록 저장 실패는 전파하고 다음 시도는 허용한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                val source = LocalNotificationDataSourceImpl(store)
                store.failure = IOException("disk full")

                shouldThrow<IOException> { source.consumeInitialPermissionRequest() }

                store.failure = null
                source.consumeInitialPermissionRequest() shouldBe true
            }
        }

        test("최초 요청 기록 저장 취소는 전파한다") {
            runTest {
                val store = NotificationMemoryDataStore()
                store.failure = CancellationException("cancel write")

                shouldThrow<CancellationException> {
                    LocalNotificationDataSourceImpl(store).consumeInitialPermissionRequest()
                }
            }
        }
    }
}

private class NotificationMemoryDataStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    var failure: Throwable? = null
    private val mutex = Mutex()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock {
            yield()
            failure?.let { throw it }
            transform(data.value).also { data.value = it }
        }
}
