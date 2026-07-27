import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import com.whatever.caro.feature.setting.mvi.SettingState
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

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
            profileRepository: ProfileRepository =
                mock {
                    everySuspend { getMyNickname() } returns "캐로"
                },
            exceptionFilter: ExceptionFilter = ExceptionFilter.None,
        ): Pair<SettingViewModel, AuthRepository> {
            val authRepository =
                mock<AuthRepository> {
                    everySuspend { logout() } returns Unit
                }
            val viewModel =
                SettingViewModel(
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    exceptionFilter = exceptionFilter,
                )
            return viewModel to authRepository
        }

        test("Initialize는 사용자 닉네임 조회 중 로딩을 표시하고 성공 후 닉네임을 반영한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.state.test {
                    awaitItem().isLoading shouldBe false

                    viewModel.intent(SettingIntent.Initialize)

                    awaitItem().isLoading shouldBe true
                    awaitItem() shouldBe
                        SettingState(
                            isLoading = false,
                            nickname = "캐로",
                        )
                }
            }
        }

        test("Initialize의 사용자 닉네임 조회가 실패하면 로딩을 해제한다") {
            runTest {
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyNickname() } throws IllegalStateException("network")
                    }
                val (viewModel, _) = createViewModel(profileRepository)

                viewModel.intent(SettingIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("Initialize의 사용자 닉네임 조회 예외가 전역 필터에서 억제되어도 로딩을 해제한다") {
            runTest {
                val suppressedException = IllegalStateException("suppressed")
                val profileRepository =
                    mock<ProfileRepository> {
                        everySuspend { getMyNickname() } throws suppressedException
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
                val (viewModel, _) = createViewModel()

                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT)
                    awaitItem() shouldBe SettingSideEffect.NavigateToLogin
                }
                viewModel.state.value.accountDeleteDialogVisible shouldBe false
            }
        }
    }
}
