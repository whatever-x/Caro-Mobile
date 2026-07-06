import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.login.LoginViewModel
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.LoginError
import com.whatever.caro.feature.login.model.SocialLoginResult
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        fun viewModelWith(
            authRepository: AuthRepository =
                mock {
                    everySuspend { loginWithSocial(any(), any()) } returns
                        AuthSession(accessToken = "access", refreshToken = "refresh")
                },
        ) = LoginViewModel(authRepository, ExceptionFilter.None)

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("구글 로그인 성공 시 GOOGLE 토큰으로 로그인하고 NavigateHome을 방출한다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { loginWithSocial(any(), any()) } returns
                            AuthSession(accessToken = "access", refreshToken = "refresh")
                    }
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(
                        LoginIntent.ClickGoogleLoginButton(
                            SocialLoginResult.Success(GoogleUser(idToken = "google-token")),
                        ),
                    )
                    awaitItem() shouldBe LoginSideEffect.NavigateHome
                }

                viewModel.state.value.isLoading shouldBe false
                verifySuspend {
                    authRepository.loginWithSocial(
                        provider = SocialLoginType.GOOGLE,
                        idToken = "google-token",
                    )
                }
            }
        }

        test("애플 로그인 성공 시 APPLE 토큰으로 로그인하고 NavigateHome을 방출한다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { loginWithSocial(any(), any()) } returns
                            AuthSession(accessToken = "access", refreshToken = "refresh")
                    }
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(
                        LoginIntent.ClickAppleLoginButton(
                            SocialLoginResult.Success(AppleUser(idToken = "apple-token")),
                        ),
                    )
                    awaitItem() shouldBe LoginSideEffect.NavigateHome
                }

                verifySuspend {
                    authRepository.loginWithSocial(
                        provider = SocialLoginType.APPLE,
                        idToken = "apple-token",
                    )
                }
            }
        }

        test("구글 로그인 결과가 Failed면 UNKNOWN 에러 스낵바를 방출하고 레포지토리를 호출하지 않는다") {
            runTest(testDispatcher) {
                val authRepository = mock<AuthRepository>()
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(LoginIntent.ClickGoogleLoginButton(SocialLoginResult.Failed))
                    awaitItem() shouldBe LoginSideEffect.ShowErrorSnackbar(LoginError.UNKNOWN)
                }

                verifySuspend(exactly(0)) {
                    authRepository.loginWithSocial(any(), any())
                }
            }
        }

        test("구글 로그인 결과가 UserCancelled면 USER_CANCELLED 에러 스낵바를 방출하고 레포지토리를 호출하지 않는다") {
            runTest(testDispatcher) {
                val authRepository = mock<AuthRepository>()
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(LoginIntent.ClickGoogleLoginButton(SocialLoginResult.UserCancelled))
                    awaitItem() shouldBe LoginSideEffect.ShowErrorSnackbar(LoginError.USER_CANCELLED)
                }

                verifySuspend(exactly(0)) {
                    authRepository.loginWithSocial(any(), any())
                }
            }
        }

        test("애플 로그인 결과가 Failed면 UNKNOWN 에러 스낵바를 방출하고 레포지토리를 호출하지 않는다") {
            runTest(testDispatcher) {
                val authRepository = mock<AuthRepository>()
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(LoginIntent.ClickAppleLoginButton(SocialLoginResult.Failed))
                    awaitItem() shouldBe LoginSideEffect.ShowErrorSnackbar(LoginError.UNKNOWN)
                }

                verifySuspend(exactly(0)) {
                    authRepository.loginWithSocial(any(), any())
                }
            }
        }

        test("애플 로그인 결과가 UserCancelled면 USER_CANCELLED 에러 스낵바를 방출하고 레포지토리를 호출하지 않는다") {
            runTest(testDispatcher) {
                val authRepository = mock<AuthRepository>()
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(LoginIntent.ClickAppleLoginButton(SocialLoginResult.UserCancelled))
                    awaitItem() shouldBe LoginSideEffect.ShowErrorSnackbar(LoginError.USER_CANCELLED)
                }

                verifySuspend(exactly(0)) {
                    authRepository.loginWithSocial(any(), any())
                }
            }
        }

        test("소셜 로그인 호출이 실패하면 UNKNOWN 스낵바를 방출하고 isLoading이 false로 복귀한다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { loginWithSocial(any(), any()) } throws RuntimeException("network error")
                    }
                val viewModel = viewModelWith(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(
                        LoginIntent.ClickGoogleLoginButton(
                            SocialLoginResult.Success(GoogleUser(idToken = "google-token")),
                        ),
                    )
                    awaitItem() shouldBe LoginSideEffect.ShowErrorSnackbar(LoginError.UNKNOWN)
                }

                viewModel.state.value.isLoading shouldBe false
            }
        }
    }
}
