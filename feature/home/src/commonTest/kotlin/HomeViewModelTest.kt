import com.whatever.caro.core.data.repository.demo.DemoRepository
import com.whatever.caro.core.model.User
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.Payload
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.di.HomeModule
import com.whatever.caro.feature.home.mvi.HomeState
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : FunSpec(), KoinTest {

    private lateinit var overrideModule: Module

    init {
        extensions(KoinExtension(HomeModule().module))

        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            unloadKoinModules(overrideModule)
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        test("init() 호출 시 state 갱신") {
            runTest {
                val demoRepositoryMock = mock<DemoRepository>()

                overrideModule = module {
                    single<DemoRepository> { demoRepositoryMock }
                }
                loadKoinModules(overrideModule)

                everySuspend { demoRepositoryMock.getData(1L) } returns User(1L, "건형")

                val navKey = HomeEntry(Payload(1, "테스터"))
                val vm: HomeViewModel = get { parametersOf(navKey) }

                vm.init()
                advanceUntilIdle()

                vm.state.value shouldBe HomeState(
                    screenName = "HomeScreen",
                    name = "건형",
                )
            }
        }
    }
}