import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.splash.SplashViewModel
import com.whatever.caro.feature.splash.mvi.SplashIntent
import com.whatever.caro.feature.splash.mvi.SplashSideEffect
import com.whatever.caro.feature.splash.mvi.SplashState
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            testDispatcher.cancel()
        }

        test("Initialize 중 토큰 갱신 실패 시 NavigateLogin sideEffect를 발행한다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { refreshToken() } throws RuntimeException("refresh failed")
                    }
                val viewModel =
                    SplashViewModel(
                        authRepository = authRepository,
                        exceptionFilter = ExceptionFilter.None,
                    )

                viewModel.sideEffect.test {
                    viewModel.intent(SplashIntent.Initialize)
                    advanceUntilIdle()

                    awaitItem() shouldBe SplashSideEffect.NavigateLogin
                }
            }
        }

        test("닉네임 설정을 마치지 않은 세션이면 NavigateCreateProfile sideEffect를 발행한다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { refreshToken() } returns Unit
                        everySuspend { isRegistrationComplete() } returns false
                    }
                val viewModel =
                    SplashViewModel(
                        authRepository = authRepository,
                        exceptionFilter = ExceptionFilter.None,
                    )

                viewModel.sideEffect.test {
                    viewModel.intent(SplashIntent.Initialize)
                    advanceUntilIdle()

                    awaitItem() shouldBe SplashSideEffect.NavigateCreateProfile
                }
            }
        }

        test("Initialize 완료 시 isInitializing 이 false 로 갱신된다") {
            runTest(testDispatcher) {
                val authRepository =
                    mock<AuthRepository> {
                        everySuspend { refreshToken() } returns Unit
                        everySuspend { isRegistrationComplete() } returns true
                    }
                val viewModel =
                    SplashViewModel(
                        authRepository = authRepository,
                        exceptionFilter = ExceptionFilter.None,
                    )

                viewModel.intent(SplashIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value shouldBe SplashState(isInitializing = false)
            }
        }
    }
}
