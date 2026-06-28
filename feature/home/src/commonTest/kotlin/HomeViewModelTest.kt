import app.cash.turbine.test
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
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
class HomeViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        fun createViewModel() = HomeViewModel(exceptionFilter = ExceptionFilter.None)

        test("ClickCreateCard 는 NavigateToCreateCard 를 emit 한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickCreateCard)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToCreateCard
                }
            }
        }

        test("ClickProfile 은 NavigateToProfile 을 emit 한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickProfile)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToProfile
                }
            }
        }
    }
}
