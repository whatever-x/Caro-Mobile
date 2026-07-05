import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.login.LoginViewModel
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.LoginError
import com.whatever.caro.feature.login.model.SocialLoginResult
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
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
class LoginViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        fun createViewModel(authRepository: AuthRepository = mock<AuthRepository>()) =
            LoginViewModel(
                authRepository = authRepository,
                exceptionFilter = ExceptionFilter.None,
            )

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("Google 로그인 취소는 USER_CANCELLED 스낵바를 emit 한다") {
            runTest(testDispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(LoginIntent.ClickGoogleLoginButton(SocialLoginResult.UserCancelled))
                    advanceUntilIdle()

                    awaitItem() shouldBe LoginSideEffect.ShowErrorSnackbar(LoginError.USER_CANCELLED)
                }
            }
        }

        test("Google 로그인 성공은 repository 로그인 후 NavigateHome 을 emit 한다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { loginWithSocial(SocialLoginType.GOOGLE, "id-token") } returns
                            AuthSession(accessToken = "access", refreshToken = "refresh")
                    }
                val viewModel = createViewModel(authRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(
                        LoginIntent.ClickGoogleLoginButton(
                            SocialLoginResult.Success(GoogleUser(idToken = "id-token")),
                        ),
                    )
                    advanceUntilIdle()

                    awaitItem() shouldBe LoginSideEffect.NavigateHome
                }
                verifySuspend(exactly(1)) {
                    authRepository.loginWithSocial(
                        provider = SocialLoginType.GOOGLE,
                        idToken = "id-token",
                    )
                }
            }
        }
    }
}
