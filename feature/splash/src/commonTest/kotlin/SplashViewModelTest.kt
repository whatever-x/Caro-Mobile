import app.cash.turbine.test
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.splash.SplashViewModel
import com.whatever.caro.feature.splash.mvi.SplashIntent
import com.whatever.caro.feature.splash.mvi.SplashSideEffect
import com.whatever.caro.feature.splash.mvi.SplashState
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

        test("Initialize intent 처리 시 NavigateLogin sideEffect를 발행한다") {
            runTest {
                val viewModel = SplashViewModel(exceptionFilter = ExceptionFilter.None)

                viewModel.sideEffect.test {
                    viewModel.intent(SplashIntent.Initialize)
                    advanceUntilIdle()

                    awaitItem() shouldBe SplashSideEffect.NavigateLogin
                }
            }
        }

        test("Initialize 완료 시 isInitializing 이 false 로 갱신된다") {
            runTest {
                val viewModel = SplashViewModel(exceptionFilter = ExceptionFilter.None)

                viewModel.intent(SplashIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value shouldBe SplashState(isInitializing = false)
            }
        }
    }
}
