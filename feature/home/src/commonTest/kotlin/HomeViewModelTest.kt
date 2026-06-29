import app.cash.turbine.test
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.di.homeModule
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest :
    FunSpec(),
    KoinTest {
    init {
        extensions(
            KoinExtension(
                listOf(
                    homeModule,
                    module {
                        single<ExceptionFilter> { ExceptionFilter.None }
                    },
                ),
            ),
        )

        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("ClickSettingButton 은 NavigateToSetting 을 방출한다") {
            runTest(dispatcher) {
                val viewModel: HomeViewModel = get()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickSettingButton)
                    awaitItem() shouldBe HomeSideEffect.NavigateToSetting
                }
            }
        }

        test("ClickProfile 은 NavigateToProfile 을 방출한다") {
            runTest(dispatcher) {
                val viewModel: HomeViewModel = get()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickProfile)
                    awaitItem() shouldBe HomeSideEffect.NavigateToProfile
                }
            }
        }

        test("ClickCreateDeck 은 NavigateToCreateDeck 을 방출한다") {
            runTest(dispatcher) {
                val viewModel: HomeViewModel = get()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickCreateDeck)
                    awaitItem() shouldBe HomeSideEffect.NavigateToCreateDeck
                }
            }
        }
    }
}
