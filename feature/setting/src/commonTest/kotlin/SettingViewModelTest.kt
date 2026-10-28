import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.profile.MyInfo
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import com.whatever.caro.feature.setting.mvi.SettingState
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private data class SettingFixture(
    val viewModel: SettingViewModel,
    val authRepository: AuthRepository,
    val profileRepository: ProfileRepository,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        fun createViewModel(
            authRepository: AuthRepository =
                mock<AuthRepository> {
                    everySuspend { logout() } returns Unit
                    everySuspend { withdraw() } returns Unit
                },
            profileRepository: ProfileRepository =
                mock<ProfileRepository> {
                    everySuspend { getMyInfo() } returns
                        MyInfo(
                            nickname = "",
                            email = "",
                            socialLoginType = SocialLoginType.NONE,
                        )
                },
            exceptionFilter: ExceptionFilter = ExceptionFilter.None,
        ): SettingFixture {
            val viewModel =
                SettingViewModel(
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    exceptionFilter = exceptionFilter,
                    fcmTokenRepository =
                        mock<FcmTokenRepository> {
                            every { observeNotificationEnabled() } returns MutableStateFlow(false)
                            every { updateNotificationPermission(any()) } returns Unit
                        },
                )
            return SettingFixture(
                viewModel = viewModel,
                authRepository = authRepository,
                profileRepository = profileRepository,
            )
        }

        test("알림 ON 중복 입력은 권한 요청 하나만 발생시키고 거절 시 OFF 안내를 표시한다") {
            runTest {
                val confirmed = MutableStateFlow(false)
                val repository =
                    mock<FcmTokenRepository> {
                        every { observeNotificationEnabled() } returns confirmed
                        every { updateNotificationPermission(any()) } returns Unit
                    }
                val viewModel = SettingViewModel(mock(), mock(), ExceptionFilter.None, repository)
                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickNotificationToggle)
                    viewModel.intent(SettingIntent.ClickNotificationToggle)
                    runCurrent()
                    awaitItem() shouldBe SettingSideEffect.RequestNotificationPermission
                    expectNoEvents()
                    viewModel.state.value.isNotificationLoading shouldBe true
                    viewModel.intent(SettingIntent.NotificationPermissionResult(false))
                    runCurrent()
                    viewModel.state.value.notificationEnabled shouldBe false
                    viewModel.state.value.notificationPermissionDialogVisible shouldBe true
                    viewModel.state.value.isNotificationLoading shouldBe false
                }
            }
        }

        test("알림 API 실패는 직전 상태와 재조작 가능 상태를 유지한다") {
            runTest {
                val repository =
                    mock<FcmTokenRepository> {
                        every { observeNotificationEnabled() } returns MutableStateFlow(true)
                        everySuspend { setNotificationEnabled(false) } throws IllegalStateException("offline")
                    }
                val viewModel = SettingViewModel(mock(), mock(), ExceptionFilter.None, repository)
                runCurrent()
                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickNotificationToggle)
                    runCurrent()
                    awaitItem() shouldBe SettingSideEffect.ShowSnackbar(SnackbarType.NOTIFICATION_CHANGE_ERROR)
                    viewModel.state.value.notificationEnabled shouldBe true
                    viewModel.state.value.isNotificationLoading shouldBe false
                }
            }
        }

        test("안내 취소 후 OS 권한 복귀만으로 알림을 켜지 않는다") {
            runTest {
                val repository =
                    mock<FcmTokenRepository> {
                        every { observeNotificationEnabled() } returns MutableStateFlow(false)
                        every { updateNotificationPermission(any()) } returns Unit
                    }
                val viewModel = SettingViewModel(mock(), mock(), ExceptionFilter.None, repository)
                viewModel.intent(SettingIntent.ClickNotificationToggle)
                runCurrent()
                viewModel.intent(SettingIntent.NotificationPermissionResult(false))
                runCurrent()
                viewModel.intent(SettingIntent.DismissNotificationPermissionDialog)
                viewModel.intent(SettingIntent.NotificationPermissionRefreshed(true))
                runCurrent()
                viewModel.state.value.notificationPermissionDialogVisible shouldBe false
                viewModel.state.value.notificationEnabled shouldBe false
                verifySuspend(exactly(0)) { repository.setNotificationEnabled(any()) }
            }
        }

        test("권한 요청이 취소되면 알림 로딩을 해제하고 다시 조작할 수 있다") {
            runTest {
                val repository =
                    mock<FcmTokenRepository> {
                        every { observeNotificationEnabled() } returns MutableStateFlow(false)
                        every { updateNotificationPermission(any()) } returns Unit
                    }
                val viewModel = SettingViewModel(mock(), mock(), ExceptionFilter.None, repository)
                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickNotificationToggle)
                    runCurrent()
                    awaitItem() shouldBe SettingSideEffect.RequestNotificationPermission
                    viewModel.intent(SettingIntent.DismissNotificationPermissionDialog)
                    runCurrent()
                    viewModel.state.value.isNotificationLoading shouldBe false
                    viewModel.intent(SettingIntent.ClickNotificationToggle)
                    runCurrent()
                    awaitItem() shouldBe SettingSideEffect.RequestNotificationPermission
                }
            }
        }

        test("앱 설정에서 허용하고 복귀하면 대기한 ON을 한번만 처리한다") {
            runTest {
                val confirmed = MutableStateFlow(false)
                val repository =
                    mock<FcmTokenRepository> {
                        every { observeNotificationEnabled() } returns confirmed
                        every { updateNotificationPermission(any()) } returns Unit
                        everySuspend { setNotificationEnabled(true) } calls { confirmed.value = true }
                    }
                val viewModel = SettingViewModel(mock(), mock(), ExceptionFilter.None, repository)
                viewModel.intent(SettingIntent.ClickNotificationToggle)
                runCurrent()
                viewModel.intent(SettingIntent.NotificationPermissionResult(false))
                runCurrent()
                viewModel.intent(SettingIntent.OpenNotificationSettings)
                runCurrent()
                viewModel.intent(SettingIntent.NotificationPermissionRefreshed(true))
                viewModel.intent(SettingIntent.NotificationPermissionRefreshed(true))
                runCurrent()
                viewModel.state.value.notificationEnabled shouldBe true
                viewModel.state.value.isNotificationLoading shouldBe false
                verifySuspend(exactly(1)) { repository.setNotificationEnabled(true) }
            }
        }

        suspend fun ReceiveTurbine<SettingSideEffect>.startInquiry(
            viewModel: SettingViewModel,
            scope: TestScope,
        ): Long {
            viewModel.intent(SettingIntent.ClickReportBug)
            scope.runCurrent()
            return (awaitItem() as SettingSideEffect.OpenInquiry).requestId
        }

        test("URL 열기 완료보다 백그라운드와 복귀가 빠르면 완료 후 평가를 요청한다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                    expectNoEvents()
                }
            }
        }

        test("백그라운드 중 열기 성공은 복귀까지 평가를 기다린다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    advanceUntilIdle()
                    expectNoEvents()
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                }
            }
        }

        test("열기 성공 뒤 첫 실제 복귀만 평가를 요청하고 중복 결과는 무시한다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("성공 이후 중복 실패와 취소 결과는 확정된 열기를 되돌리지 않는다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, false))
                    viewModel.intent(SettingIntent.InquiryOpenCancelled(requestId))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                }
            }
        }

        test("열기 성공 없이 복귀해도 평가를 요청하지 않는다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("복귀 이후 실패한 URL 열기는 이후 복귀에도 평가를 요청하지 않는다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, false))
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("취소된 열기의 늦은 성공 결과는 평가를 만들지 않는다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.InquiryOpenCancelled(requestId))
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("새 문의 이후 이전 요청의 성공 실패 취소는 새 문의를 바꾸지 않는다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val previousId = startInquiry(viewModel, this@runTest)
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.InquiryOpenResult(previousId, true))
                    viewModel.intent(SettingIntent.InquiryOpenResult(previousId, false))
                    viewModel.intent(SettingIntent.InquiryOpenCancelled(previousId))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                }
            }
        }

        test("설정 이탈은 열기 결과와 이미 발행된 평가 요청의 소유권을 무효화한다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    val requestId = startInquiry(viewModel, this@runTest)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                    viewModel.isCurrentInquiryRequest(requestId) shouldBe true
                    viewModel.intent(SettingIntent.CancelPendingInquiry)
                    viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    viewModel.isCurrentInquiryRequest(requestId) shouldBe false
                    expectNoEvents()
                }
            }
        }

        test("일반 복귀와 약관 복귀에는 평가를 요청하지 않는다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    viewModel.intent(SettingIntent.ClickTermsOfService)
                    advanceUntilIdle()
                    awaitItem() shouldBe SettingSideEffect.NavigateWebView(WebViewType.TERMS_OF_SERVICE)
                    viewModel.intent(SettingIntent.AppBackgrounded)
                    viewModel.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("새 문의 복귀는 공용 평가 요청기에 다시 위임할 수 있다") {
            runTest {
                val (viewModel) = createViewModel()
                viewModel.sideEffect.test {
                    repeat(2) {
                        val requestId = startInquiry(viewModel, this@runTest)
                        viewModel.intent(SettingIntent.InquiryOpenResult(requestId, true))
                        viewModel.intent(SettingIntent.AppBackgrounded)
                        viewModel.intent(SettingIntent.AppResumed)
                        advanceUntilIdle()
                        awaitItem() shouldBe SettingSideEffect.RequestStoreReview(requestId)
                        expectNoEvents()
                    }
                }
            }
        }

        test("새 ViewModel은 이전 문의의 대기를 복원하지 않는다") {
            runTest {
                val (previous) = createViewModel()
                previous.sideEffect.test {
                    val requestId = startInquiry(previous, this@runTest)
                    previous.intent(SettingIntent.InquiryOpenResult(requestId, true))
                    previous.intent(SettingIntent.AppBackgrounded)
                    advanceUntilIdle()
                }
                val (recreated) = createViewModel()
                recreated.sideEffect.test {
                    recreated.intent(SettingIntent.AppResumed)
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("Initialize는 현재 사용자 정보를 상태에 반영한다") {
            runTest {
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } returns
                            MyInfo(
                                nickname = "캐로",
                                email = "caro@example.com",
                                socialLoginType = SocialLoginType.GOOGLE,
                            )
                    }
                val (viewModel) = createViewModel(profileRepository = profileRepository)

                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value shouldBe
                    SettingState(
                        isLoading = false,
                        nickname = "캐로",
                        emailAddress = "caro@example.com",
                        socialLoginType = SocialLoginType.GOOGLE,
                    )
                verifySuspend(exactly(1)) { profileRepository.getMyInfo() }
            }
        }

        test("Initialize 요청 중에는 isLoading이 true이고 완료 후 false가 된다") {
            runTest {
                val response = CompletableDeferred<MyInfo>()
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls { response.await() }
                    }
                val (viewModel) = createViewModel(profileRepository = profileRepository)

                viewModel.intent(SettingIntent.Initialize)
                runCurrent()

                viewModel.state.value.isLoading shouldBe true

                response.complete(
                    MyInfo(
                        nickname = "캐로",
                        email = "caro@example.com",
                        socialLoginType = SocialLoginType.APPLE,
                    ),
                )
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("Initialize 실패 시 기존 사용자 정보를 유지하고 isLoading을 false로 복구한다") {
            runTest {
                var requestCount = 0
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls {
                            requestCount++
                            if (requestCount == 1) {
                                MyInfo(
                                    nickname = "기존 닉네임",
                                    email = "old@example.com",
                                    socialLoginType = SocialLoginType.GOOGLE,
                                )
                            } else {
                                throw RuntimeException("my info failed")
                            }
                        }
                    }
                val (viewModel) = createViewModel(profileRepository = profileRepository)

                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value shouldBe
                    SettingState(
                        isLoading = false,
                        nickname = "기존 닉네임",
                        emailAddress = "old@example.com",
                        socialLoginType = SocialLoginType.GOOGLE,
                    )
            }
        }

        test("연속 Initialize에서는 이전 응답이 최신 상태와 로딩을 덮어쓰지 않는다") {
            runTest {
                val firstResponse = CompletableDeferred<MyInfo>()
                val secondResponse = CompletableDeferred<MyInfo>()
                var requestCount = 0
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } calls {
                            requestCount++
                            if (requestCount == 1) firstResponse.await() else secondResponse.await()
                        }
                    }
                val (viewModel) = createViewModel(profileRepository = profileRepository)

                viewModel.intent(SettingIntent.Initialize)
                runCurrent()
                viewModel.intent(SettingIntent.Initialize)
                runCurrent()

                firstResponse.complete(
                    MyInfo(
                        nickname = "이전 닉네임",
                        email = "old@example.com",
                        socialLoginType = SocialLoginType.GOOGLE,
                    ),
                )
                runCurrent()

                viewModel.state.value.nickname shouldBe ""
                viewModel.state.value.isLoading shouldBe true

                secondResponse.complete(
                    MyInfo(
                        nickname = "최신 닉네임",
                        email = "latest@example.com",
                        socialLoginType = SocialLoginType.APPLE,
                    ),
                )
                advanceUntilIdle()

                viewModel.state.value shouldBe
                    SettingState(
                        isLoading = false,
                        nickname = "최신 닉네임",
                        emailAddress = "latest@example.com",
                        socialLoginType = SocialLoginType.APPLE,
                    )
            }
        }

        test("Initialize의 사용자 정보 조회가 실패하면 로딩을 해제하고 에러 스낵바를 emit 한다") {
            runTest {
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } throws IllegalStateException("my info failed")
                    }
                val (viewModel, _) = createViewModel(profileRepository = profileRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.Initialize)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.ShowSnackbar(type = SnackbarType.USER_INFO_LOAD_ERROR)
                }
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("ClickLogOut 은 logout 을 호출하고 ShowSnackbar(LOGOUT) 와 NavigateToLogin 을 순서대로 emit 한다") {
            runTest {
                val (viewModel, authRepository) = createViewModel()

                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickLogOut)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowSnackbar(type = SnackbarType.LOGOUT)
                    awaitItem() shouldBe SettingSideEffect.NavigateToLogin
                }
                verifySuspend(exactly(1)) { authRepository.logout() }
            }
        }

        test("로그아웃 실패 시 로딩을 해제하고 성공 sideEffect를 방출하지 않는다") {
            runTest {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { logout() } throws RuntimeException("logout failed")
                    }
                val (viewModel) = createViewModel(authRepository = authRepository)

                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickLogOut)
                    advanceUntilIdle()

                    expectNoEvents()
                }
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("ClickNicknameChange 은 현재 닉네임으로 NavigateToEditNickName 을 emit 한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickNicknameChange)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.NavigateToEditNickName(
                            currentNickname = "",
                        )
                }
            }
        }

        test("ClickTermsOfService 은 TERMS_OF_SERVICE 웹뷰로 이동한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickTermsOfService)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.NavigateWebView(type = WebViewType.TERMS_OF_SERVICE)
                }
            }
        }

        test("ClickPrivacyPolicy 은 PRIVACY_POLICY 웹뷰로 이동한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickPrivacyPolicy)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.NavigateWebView(type = WebViewType.PRIVACY_POLICY)
                }
            }
        }

        test("ClickReportBug 은 식별자가 있는 URL 열기를 요청한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickReportBug)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.OpenInquiry(requestId = 1L)
                }
            }
        }

        test("ClickBack 은 PopBackStack 을 emit 한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.PopBackStack
                }
            }
        }

        test("ClickDeleteAccount 은 계정 삭제 다이얼로그를 연다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.state.value.accountDeleteDialogVisible shouldBe true
            }
        }

        test("다이얼로그가 열린 상태에서 ClickDeleteAccountDialogCancel 은 다이얼로그를 닫는다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()
                viewModel.intent(SettingIntent.ClickDeleteAccountDialogCancel)
                advanceUntilIdle()

                viewModel.state.value.accountDeleteDialogVisible shouldBe false
            }
        }

        test("ClickDeleteAccountDialogConfirm 은 다이얼로그를 닫고 ShowSnackbar(DELETE_ACCOUNT) 와 NavigateToLogin 을 emit 한다") {
            runTest {
                val (viewModel, authRepository) = createViewModel()

                viewModel.intent(SettingIntent.Initialize)
                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT)
                    awaitItem() shouldBe SettingSideEffect.NavigateToLogin
                }
                viewModel.state.value.accountDeleteDialogVisible shouldBe false
                viewModel.state.value.isLoading shouldBe false
                verifySuspend(exactly(1)) { authRepository.withdraw() }
            }
        }

        test("회원탈퇴 요청 중에는 isDeletingAccount가 true이고 완료 후 false가 된다") {
            runTest {
                val withdrawal = CompletableDeferred<Unit>()
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { withdraw() } calls { withdrawal.await() }
                    }
                val (viewModel) = createViewModel(authRepository = authRepository)
                viewModel.intent(SettingIntent.Initialize)
                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                runCurrent()

                viewModel.state.value.isDeletingAccount shouldBe true
                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.accountDeleteDialogVisible shouldBe true

                withdrawal.complete(Unit)
                advanceUntilIdle()

                viewModel.state.value.isDeletingAccount shouldBe false
                viewModel.state.value.accountDeleteDialogVisible shouldBe false
            }
        }

        test("회원탈퇴 확인을 연속 실행해도 API와 성공 sideEffect는 한 번만 실행된다") {
            runTest {
                val withdrawal = CompletableDeferred<Unit>()
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { withdraw() } calls { withdrawal.await() }
                    }
                val (viewModel) = createViewModel(authRepository = authRepository)
                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                    runCurrent()
                    viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                    runCurrent()

                    viewModel.state.value.isDeletingAccount shouldBe true
                    verifySuspend(exactly(1)) { authRepository.withdraw() }

                    withdrawal.complete(Unit)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT)
                    awaitItem() shouldBe SettingSideEffect.NavigateToLogin
                    expectNoEvents()
                }
            }
        }

        test("회원탈퇴 실패 시 다이얼로그를 유지하고 에러 스낵바를 방출한다") {
            runTest {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { withdraw() } throws RuntimeException("withdraw failed")
                    }
                val (viewModel) = createViewModel(authRepository = authRepository)
                viewModel.intent(SettingIntent.Initialize)
                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT_ERROR)
                    expectNoEvents()
                }
                viewModel.state.value.accountDeleteDialogVisible shouldBe true
                viewModel.state.value.isDeletingAccount shouldBe false
                viewModel.state.value.isLoading shouldBe false
            }
        }
    }
}
