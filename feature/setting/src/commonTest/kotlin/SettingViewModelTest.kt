import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.model.ToastType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import dev.mokkery.answering.returns
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

        fun createViewModel(): Pair<SettingViewModel, AuthRepository> {
            val authRepository =
                mock<AuthRepository> {
                    everySuspend { logout() } returns Unit
                }
            val viewModel =
                SettingViewModel(
                    authRepository = authRepository,
                    exceptionFilter = ExceptionFilter.None,
                )
            return viewModel to authRepository
        }

        test("ClickLogOut 은 logout 을 호출하고 ShowToast(LOGOUT) 와 NavigateToLogin 을 순서대로 emit 한다") {
            runTest {
                val (viewModel, authRepository) = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickLogOut)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowToast(type = ToastType.LOGOUT)
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

        test("ClickDeleteAccountDialogConfirm 은 다이얼로그를 닫고 ShowToast(DELETE_ACCOUNT) 와 NavigateToLogin 을 emit 한다") {
            runTest {
                val (viewModel, _) = createViewModel()

                viewModel.intent(SettingIntent.ClickDeleteAccount)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe SettingSideEffect.ShowToast(type = ToastType.DELETE_ACCOUNT)
                    awaitItem() shouldBe SettingSideEffect.NavigateToLogin
                }
                viewModel.state.value.accountDeleteDialogVisible shouldBe false
            }
        }
    }
}
