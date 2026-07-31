import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
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
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
                )
            return SettingFixture(
                viewModel = viewModel,
                authRepository = authRepository,
                profileRepository = profileRepository,
            )
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

        test("Initialize의 사용자 정보 조회 예외가 전역 필터에서 억제되어도 로딩을 해제한다") {
            runTest {
                val suppressedException = IllegalStateException("suppressed")
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyInfo() } throws suppressedException
                    }
                val (viewModel, _) =
                    createViewModel(
                        profileRepository = profileRepository,
                        exceptionFilter = ExceptionFilter { it === suppressedException },
                    )

                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("ClickLogOut 은 logout 을 호출하고 ShowSnackbar(LOGOUT) 와 NavigateToLogin 을 순서대로 emit 한다") {
            runTest {
                val (viewModel, authRepository) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickLogOut)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowSnackbar(type = SnackbarType.LOGOUT)
                    awaitItem() shouldBe SettingSideEffect.NavigateToLogin
                }
                verifySuspend(exactly(1)) { authRepository.logout() }
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

        test("ClickReportBug 은 REPORT_BUG 웹뷰로 이동한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickReportBug)
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        SettingSideEffect.NavigateWebView(type = WebViewType.REPORT_BUG)
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

        test("회원탈퇴 실패 시 다이얼로그를 유지하고 성공 sideEffect를 방출하지 않는다") {
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

                    expectNoEvents()
                }
                viewModel.state.value.accountDeleteDialogVisible shouldBe true
                viewModel.state.value.isDeletingAccount shouldBe false
                viewModel.state.value.isLoading shouldBe false
            }
        }
    }
}
