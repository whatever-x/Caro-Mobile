package com.whatever.caro.core.data.repository.fcm

import com.whatever.caro.core.data.repository.auth.AuthRepositoryImpl
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.datastore.datasource.LocalNotificationDataSource
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.remote.datasource.auth.AuthDataSource
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.datasource.notification.NotificationDataSource
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import com.whatever.caro.core.remote.dto.notification.response.TestPushResponse
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FcmTokenRepositoryImplTest : FunSpec() {
    init {
        test("기기 OFF는 계정 변경과 토큰 갱신 후에도 자동등록을 차단한다") {
            runTest {
                val preferences = MemoryNotificationPreferences(false)
                val session = MutableStateFlow<String?>("first-user")
                val tokens = MutableStateFlow("first-token")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(session, requests, preferences = preferences)
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                session.value = "second-user"
                tokens.value = "second-token"
                repository.resumeRegistration()
                repository.updateNotificationPermission(true)
                runCurrent()
                requests shouldBe emptyList()
                preferences.observeNotificationEnabled().first() shouldBe false
            }
        }

        test("OFF 후 같은 토큰 ON은 등록 성공 후 선택을 저장한다") {
            runTest {
                val preferences = MemoryNotificationPreferences()
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val deleted = mutableListOf<String>()
                val repository = repository(MutableStateFlow("access"), requests, deleted, preferences = preferences)
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                repository.setNotificationEnabled(false)
                preferences.observeNotificationEnabled().first() shouldBe false
                deleted shouldBe listOf("token")
                repository.setNotificationEnabled(true)
                runCurrent()
                requests.map { it.token } shouldBe listOf("token", "token")
                preferences.observeNotificationEnabled().first() shouldBe true
                repository.observeNotificationEnabled().first() shouldBe true
            }
        }

        test("기기 OFF 삭제 실패는 이전 ON 선택과 재등록 가능 상태를 보존한다") {
            runTest {
                val preferences = MemoryNotificationPreferences(true)
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                var failDelete = true
                val tokens = MutableStateFlow("first")
                val repository =
                    repository(MutableStateFlow("access"), requests, preferences = preferences, beforeDelete = {
                        if (failDelete) error("delete failed")
                    })
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                shouldThrow<IllegalStateException> { repository.setNotificationEnabled(false) }
                preferences.observeNotificationEnabled().first() shouldBe true
                failDelete = false
                tokens.value = "second"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first", "second")
            }
        }

        test("권한 없는 동안 등록하지 않고 복원 후 저장된 ON만 등록한다") {
            runTest {
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(MutableStateFlow("access"), requests)
                repository.updateNotificationPermission(false)
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                requests shouldBe emptyList()
                repository.updateNotificationPermission(true)
                runCurrent()
                requests.map { it.token } shouldBe listOf("token")
            }
        }

        test("토큰 대기 중 로그아웃하면 ON을 확정하거나 선택을 저장하지 않는다") {
            runTest {
                val preferences = MemoryNotificationPreferences(false)
                val session = MutableStateFlow<String?>("access")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val tokens = MutableStateFlow("")
                val repository = repository(session, requests, preferences = preferences)
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                val change =
                    async {
                        shouldThrow<IllegalStateException> { repository.setNotificationEnabled(true) }
                    }
                runCurrent()
                repository.unregisterToken()
                session.value = null
                tokens.value = "new-token"
                runCurrent()
                change.await()
                requests shouldBe emptyList()
                preferences.observeNotificationEnabled().first() shouldBe false
            }
        }

        test("토큰이 발급되지 않으면 제한 시간 후 실패하고 OFF를 유지한다") {
            runTest {
                val preferences = MemoryNotificationPreferences(false)
                val repository = repository(MutableStateFlow("access"), mutableListOf(), preferences = preferences)
                shouldThrow<IllegalStateException> { repository.setNotificationEnabled(true) }
                preferences.observeNotificationEnabled().first() shouldBe false
            }
        }

        test("등록 실패는 OFF 선택을 보존하고 앱 복귀는 실패한 자동등록을 재시도한다") {
            runTest {
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                var fail = true
                val repository = repository(MutableStateFlow("access"), requests) { if (fail) error("offline") }
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                repository.observeNotificationEnabled().first() shouldBe false
                fail = false
                repository.updateNotificationPermission(true)
                runCurrent()
                repository.observeNotificationEnabled().first() shouldBe true
                requests.size shouldBe 1
            }
        }

        test("ON 등록 뒤 선택 저장 실패는 다음 복귀에서 기존 OFF에 맞춰 삭제한다") {
            runTest {
                val preferences = MemoryNotificationPreferences(false)
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val deleted = mutableListOf<String>()
                val repository = repository(MutableStateFlow("access"), requests, deleted, preferences = preferences)
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                preferences.failSaveChoice = true
                shouldThrow<IllegalStateException> { repository.setNotificationEnabled(true) }
                preferences.observeNotificationEnabled().first() shouldBe false
                repository.updateNotificationPermission(true)
                runCurrent()
                deleted shouldBe listOf("token")
                repository.observeNotificationEnabled().first() shouldBe false
            }
        }

        test("OFF 삭제 뒤 선택 저장 실패는 다음 복귀에서 기존 ON을 다시 등록한다") {
            runTest {
                val preferences = MemoryNotificationPreferences(true)
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(MutableStateFlow("access"), requests, preferences = preferences)
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                preferences.failSaveChoice = true
                shouldThrow<IllegalStateException> { repository.setNotificationEnabled(false) }
                preferences.observeNotificationEnabled().first() shouldBe true
                repository.updateNotificationPermission(true)
                runCurrent()
                requests.map { it.token } shouldBe listOf("token", "token")
                repository.observeNotificationEnabled().first() shouldBe true
            }
        }

        test("저장된 ON은 앱 재시작 자동등록 실패에도 화면에서 유지한다") {
            runTest {
                val preferences = MemoryNotificationPreferences(true)
                val repository = repository(MutableStateFlow("access"), mutableListOf(), preferences = preferences) { error("offline") }
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                repository.observeNotificationEnabled().first() shouldBe true
            }
        }

        test("성공한 토큰은 같은 권한으로 반복 복귀해도 다시 등록하지 않는다") {
            runTest {
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(MutableStateFlow("access"), requests)
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                repeat(3) {
                    repository.updateNotificationPermission(true)
                    runCurrent()
                }
                requests.map { it.token } shouldBe listOf("token")
            }
        }

        listOf("소셜 로그인", "가입 완료").forEach { loginPath ->
            test("로그아웃의 null을 수집하지 않아도 $loginPath 후 FCM 등록을 재개한다") {
                runTest {
                    val session = MutableStateFlow<String?>("old-access")
                    var savedAccess = "old-access"
                    val requests = mutableListOf<RegisterDeviceTokenRequest>()
                    val local =
                        mock<LocalAuthDataSource> {
                            every { observeRegisteredAccessToken() } returns session
                            everySuspend { fetchAccessToken() } calls { savedAccess }
                            everySuspend { clear() } calls { session.value = null }
                            everySuspend { saveTokens("new-access", "refresh") } calls { savedAccess = "new-access" }
                            everySuspend { saveRegistrationComplete(true) } calls { session.value = savedAccess }
                        }
                    val remote =
                        mock<NotificationDataSource> {
                            everySuspend { registerToken(any()) } calls { requests.add(it.arg(0)) }
                            everySuspend { unregisterToken(any()) } returns Unit
                        }
                    val fcm =
                        FcmTokenRepositoryImpl(
                            remote,
                            local,
                            MemoryNotificationPreferences(),
                        ).also { it.updateNotificationPermission(true) }
                    val auth =
                        AuthRepositoryImpl(
                            remoteAuthDataSource =
                                mock<AuthDataSource> {
                                    everySuspend { logout() } returns Unit
                                    everySuspend { completeRegistration(any()) } returns TokenResponse("new-access", "refresh")
                                },
                            remoteNonAuthDataSource =
                                mock<NonAuthDataSource> {
                                    everySuspend { socialLogin(any()) } returns SocialLoginResponse("new-access", "refresh", true)
                                },
                            localAuthDataSource = local,
                            fcmTokenRepository = fcm,
                        )
                    backgroundScope.launch { fcm.syncTokens(MutableStateFlow("fcm-token")) }
                    runCurrent()
                    requests.map { it.token } shouldBe listOf("fcm-token")

                    auth.logout()
                    if (loginPath == "소셜 로그인") {
                        auth.loginWithSocial(SocialLoginType.GOOGLE, "id-token")
                    } else {
                        auth.completeRegistration("caro", true)
                    }
                    runCurrent()

                    requests.map { it.token } shouldBe listOf("fcm-token", "fcm-token")
                }
            }
        }

        test("토큰 갱신 응답이 실패해도 기존 토큰과 새 토큰 모두 삭제한다") {
            runTest {
                val tokens = MutableStateFlow("first")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val deleted = mutableListOf<String>()
                var fail = false
                val repository =
                    repository(MutableStateFlow<String?>("access"), requests, deleted = deleted) {
                        if (fail) error("response lost")
                    }
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                fail = true
                tokens.value = "second"
                runCurrent()
                repository.unregisterToken()
                deleted shouldBe listOf("first", "second")
            }
        }

        test("진행 중인 등록이 끝난 뒤 삭제하고 로그아웃 중 토큰 갱신을 막는다") {
            runTest {
                val session = MutableStateFlow<String?>("access")
                val tokens = MutableStateFlow("first")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val deleted = mutableListOf<String>()
                val registered = CompletableDeferred<Unit>()
                val repository = repository(session, requests, deleted = deleted) { registered.await() }
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                val logout = launch { repository.unregisterToken() }
                runCurrent()
                deleted shouldBe emptyList()
                registered.complete(Unit)
                logout.join()
                deleted shouldBe listOf("first")
                tokens.value = "second"
                session.value = "refreshed"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first")
            }
        }

        test("삭제 실패 후에도 로그아웃 중 재등록하지 않는다") {
            runTest {
                val session = MutableStateFlow<String?>("access")
                val tokens = MutableStateFlow("first")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(session, requests, beforeDelete = { error("offline") })
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                shouldThrow<IllegalStateException> { repository.unregisterToken() }
                tokens.value = "second"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first")
            }
        }

        test("로그아웃은 서버에 등록한 토큰을 삭제하고 세션이 끝날 때까지 재등록을 막는다") {
            runTest {
                val session = MutableStateFlow<String?>("access")
                val tokens = MutableStateFlow("first")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val deleted = mutableListOf<String>()
                val repository = repository(session, requests, deleted = deleted)
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                repository.unregisterToken()
                deleted shouldBe listOf("first")
                tokens.value = "second"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first")
                session.value = null
                runCurrent()
                session.value = "new-access"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first", "second")
            }
        }

        test("등록된 토큰이 없으면 서버 삭제를 호출하지 않는다") {
            runTest {
                val deleted = mutableListOf<String>()
                repository(MutableStateFlow<String?>("access"), mutableListOf(), deleted = deleted).unregisterToken()
                deleted.size shouldBe 0
            }
        }

        test("만료된 인증 토큰이 갱신되면 같은 FCM 토큰을 재등록한다") {
            runTest {
                val session = MutableStateFlow<String?>("expired")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                var fail = true
                val repository =
                    repository(session, requests) {
                        if (fail) throw IllegalStateException("unauthorized")
                    }
                backgroundScope.launch { repository.syncTokens(MutableStateFlow("fcm-token")) }
                runCurrent()
                requests.size shouldBe 0
                fail = false
                session.value = "refreshed"
                runCurrent()
                requests.map { it.token } shouldBe listOf("fcm-token")
            }
        }

        test("세션 관찰의 CancellationException은 전파한다") {
            runTest {
                val session = flow<String?> { throw CancellationException("cancel observation") }
                shouldThrow<CancellationException> {
                    repository(session, mutableListOf()).syncTokens(flowOf("token"))
                }
            }
        }

        test("세션 관찰 실패는 앱 코루틴으로 전파하지 않는다") {
            runTest {
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val session = flow<String?> { throw IllegalStateException("observe failed") }
                shouldNotThrowAny { repository(session, requests).syncTokens(flowOf("token")) }
                requests.size shouldBe 0
            }
        }

        test("로그인 전 토큰을 보관하고 로그인 성공 시 등록한다") {
            runTest {
                val session = MutableStateFlow<String?>(null)
                val tokens = MutableStateFlow("fcm-token")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(session, requests)
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                requests.size shouldBe 0
                session.value = "access"
                runCurrent()
                requests.map { it.token } shouldBe listOf("fcm-token")
            }
        }

        test("로그인 상태의 앱 시작과 토큰 갱신 시 등록하고 빈 토큰은 무시한다") {
            runTest {
                val tokens = MutableStateFlow("")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(MutableStateFlow<String?>("access"), requests)
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                requests.size shouldBe 0
                tokens.value = "first"
                runCurrent()
                tokens.value = "second"
                runCurrent()
                tokens.value = "second"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first", "second")
            }
        }

        test("로그아웃 상태에서는 등록하지 않고 재로그인 시 같은 토큰을 재등록한다") {
            runTest {
                val session = MutableStateFlow<String?>("access")
                val tokens = MutableStateFlow("first")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                val repository = repository(session, requests)
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                session.value = null
                runCurrent()
                tokens.value = "second"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first")
                session.value = "access"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first", "second")
                session.value = null
                runCurrent()
                session.value = "access"
                runCurrent()
                requests.map { it.token } shouldBe listOf("first", "second", "second")
            }
        }

        test("등록 실패 후 수집을 유지하고 다음 앱 실행 시 같은 토큰을 재시도한다") {
            runTest {
                val tokens = MutableStateFlow("first")
                val requests = mutableListOf<RegisterDeviceTokenRequest>()
                var fail = true
                val repository =
                    repository(MutableStateFlow<String?>("access"), requests) {
                        if (fail) throw IllegalStateException("network failed")
                    }
                val job = backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                job.isActive shouldBe true
                fail = false
                tokens.value = "second"
                runCurrent()
                requests.map { it.token } shouldBe listOf("second")
                job.cancel()
                backgroundScope.launch { repository.syncTokens(tokens) }
                runCurrent()
                requests.map { it.token } shouldBe listOf("second", "second")
            }
        }

        test("코루틴 취소는 흡수하지 않는다") {
            runTest {
                val repository =
                    repository(MutableStateFlow<String?>("access"), mutableListOf()) {
                        throw CancellationException("cancel sync")
                    }
                val job = backgroundScope.launch { repository.syncTokens(MutableStateFlow("token")) }
                runCurrent()
                job.isCancelled shouldBe true
            }
        }
    }

    private fun repository(
        session: Flow<String?>,
        requests: MutableList<RegisterDeviceTokenRequest>,
        deleted: MutableList<String> = mutableListOf(),
        beforeDelete: suspend () -> Unit = {},
        preferences: MemoryNotificationPreferences = MemoryNotificationPreferences(),
        beforeRegister: suspend () -> Unit = {},
    ): FcmTokenRepositoryImpl {
        val local =
            mock<LocalAuthDataSource> {
                every { observeRegisteredAccessToken() } returns session
                everySuspend { fetchAccessToken() } calls { (session as? StateFlow<String?>)?.value }
            }
        val remote =
            object : NotificationDataSource {
                override suspend fun registerToken(request: RegisterDeviceTokenRequest) {
                    beforeRegister()
                    requests.add(request)
                }

                override suspend fun unregisterToken(token: String) {
                    beforeDelete()
                    deleted.add(token)
                }

                override suspend fun sendTestPush(): TestPushResponse = error("unused")
            }
        return FcmTokenRepositoryImpl(remote, local, preferences).also { it.updateNotificationPermission(true) }
    }
}

private class MemoryNotificationPreferences(
    initial: Boolean? = null,
) : LocalNotificationDataSource {
    var failSaveChoice = false
    private val enabled = MutableStateFlow(initial)
    private var registeredToken: String? = null

    override suspend fun consumeInitialPermissionRequest(): Boolean = error("unused")

    override fun observeNotificationEnabled(): Flow<Boolean?> = enabled

    override suspend fun saveNotificationEnabled(enabled: Boolean) {
        check(!failSaveChoice) { "storage failed" }
        this.enabled.value = enabled
    }

    override suspend fun fetchRegisteredToken(): String? = registeredToken

    override suspend fun saveRegisteredToken(token: String?) {
        registeredToken = token
    }
}
