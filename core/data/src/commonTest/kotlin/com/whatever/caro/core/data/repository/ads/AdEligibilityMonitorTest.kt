package com.whatever.caro.core.data.repository.ads

import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.datastore.datasource.AuthTokens
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.profile.MyInfo
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AdEligibilityMonitorTest :
    FunSpec({
        fun info(adult: Boolean?) = MyInfo("nickname", "", SocialLoginType.GOOGLE, adult)

        test("성인만 광고를 허용하고 미성년, 미확인, 로그아웃은 차단한다") {
            runTest {
                val token = MutableStateFlow<AuthTokens?>(null)
                var adult: Boolean? = true
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns token
                        everySuspend { fetchTokens() } calls { token.value ?: AuthTokens(null, null) }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls { info(adult) }
                    }
                val gate = AdEligibilityMonitor(auth, profile, AdEligibilityGate(), backgroundScope)
                runCurrent()
                gate.state.value.allowed shouldBe false
                token.value = AuthTokens("adult", "refresh", 1)
                runCurrent()
                gate.state.value.allowed shouldBe true
                adult = false
                token.value = AuthTokens("minor", "refresh", 2)
                runCurrent()
                gate.state.value.allowed shouldBe false
                adult = null
                token.value = AuthTokens("unknown", "refresh", 3)
                runCurrent()
                gate.state.value.allowed shouldBe false
                token.value = null
                runCurrent()
                gate.state.value.allowed shouldBe false
            }
        }

        test("계정 전환 중 늦게 도착한 이전 성인 판정은 허용되지 않고 전환 후 재확인한다") {
            runTest {
                val token = MutableStateFlow<AuthTokens?>(AuthTokens("adult", "refresh", 1))
                val pending = CompletableDeferred<MyInfo>()
                var calls = 0
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns token
                        everySuspend { fetchTokens() } calls { token.value!! }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls {
                            calls++
                            if (calls == 1) pending.await() else info(true)
                        }
                    }
                val access = AdEligibilityGate()
                val gate = AdEligibilityMonitor(auth, profile, access, backgroundScope)
                runCurrent()
                access.beginTransition()
                pending.complete(info(true))
                runCurrent()
                gate.state.value.allowed shouldBe false
                access.endTransition()
                runCurrent()
                gate.state.value.allowed shouldBe true
            }
        }

        test("늦게 전달된 이전 토큰은 조회 기회를 사용하지 않는다") {
            runTest {
                val old = AuthTokens("old", "refresh-old", 1)
                val current = AuthTokens("new", "refresh-new", 1)
                val sessionEvents = MutableStateFlow<AuthTokens?>(old)
                var calls = 0
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns sessionEvents
                        everySuspend { fetchTokens() } calls { current }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls {
                            calls++
                            info(true)
                        }
                    }
                val gate = AdEligibilityMonitor(auth, profile, AdEligibilityGate(), backgroundScope)
                runCurrent()
                calls shouldBe 0
                sessionEvents.value = current
                runCurrent()
                calls shouldBe 1
                gate.state.value.allowed shouldBe true
            }
        }

        test("정상 성인은 같은 로그인 세션에서 여러 번 토큰을 갱신해도 광고를 다시 허용한다") {
            runTest {
                val token = MutableStateFlow<AuthTokens?>(AuthTokens("adult", "refresh", 1))
                val access = AdEligibilityGate()
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns token
                        everySuspend { fetchTokens() } calls { token.value!! }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls { info(true) }
                    }
                val gate = AdEligibilityMonitor(auth, profile, access, backgroundScope)
                runCurrent()
                gate.state.value.allowed shouldBe true
                repeat(3) { i ->
                    access.beginTransition()
                    token.value = AuthTokens("adult-$i", "refresh-$i", 1)
                    access.endTransition()
                    runCurrent()
                    gate.state.value.allowed shouldBe true
                }
            }
        }

        test("조회 실패 중 토큰이 반복 갱신돼도 성인 확인이 무한 반복되지 않는다") {
            runTest {
                val token = MutableStateFlow<AuthTokens?>(AuthTokens("adult", "refresh", 1))
                val access = AdEligibilityGate()
                var calls = 0
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns token
                        everySuspend { fetchTokens() } calls { token.value!! }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls {
                            calls++
                            access.beginTransition()
                            token.value = AuthTokens("adult-$calls", "refresh-$calls", 1)
                            access.endTransition()
                            error("profile remains unauthorized")
                        }
                    }
                val gate = AdEligibilityMonitor(auth, profile, access, backgroundScope)
                runCurrent()
                calls shouldBe 2
                gate.state.value.allowed shouldBe false
            }
        }

        test("일시적 조회 실패 후 재확인하면 성인 광고를 허용한다") {
            runTest {
                val token = MutableStateFlow<AuthTokens?>(AuthTokens("adult", "refresh", 1))
                var calls = 0
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns token
                        everySuspend { fetchTokens() } calls { token.value!! }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls {
                            calls++
                            if (calls == 1) error("offline") else info(true)
                        }
                    }
                val gate = AdEligibilityMonitor(auth, profile, AdEligibilityGate(), backgroundScope)
                runCurrent()
                gate.state.value.allowed shouldBe false
                gate.recheck()
                runCurrent()
                gate.state.value.allowed shouldBe true
            }
        }

        test("세션 전환 뒤 이전 성인 응답이 늦게 도착해도 광고를 허용하지 않는다") {
            runTest {
                val token = MutableStateFlow<AuthTokens?>(AuthTokens("adult", "refresh", 1))
                val pending = CompletableDeferred<MyInfo>()
                val auth =
                    mock<LocalAuthDataSource> {
                        every { observeRegisteredSession() } returns token
                        everySuspend { fetchTokens() } calls { token.value ?: AuthTokens(null, null) }
                    }
                val profile =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls { withContext(NonCancellable) { pending.await() } }
                    }
                val gate = AdEligibilityMonitor(auth, profile, AdEligibilityGate(), backgroundScope)
                runCurrent()
                gate.revoke()
                token.value = null
                pending.complete(info(true))
                runCurrent()
                gate.state.value.allowed shouldBe false
            }
        }
    })
