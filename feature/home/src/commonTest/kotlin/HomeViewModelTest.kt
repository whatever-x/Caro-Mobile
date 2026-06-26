import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.Payload
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.di.homeModule
import com.whatever.caro.feature.home.mvi.HomeState
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
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
                        single<AuthRepository> { mock<AuthRepository>() }
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

        test("init() 호출 시 navKey payload 로 state 갱신") {
            runTest {
                val navKey = HomeEntry(Payload(1, "테스터"))
                val vm: HomeViewModel = get { parametersOf(navKey) }

                vm.init()
                advanceUntilIdle()

                vm.state.value shouldBe
                    HomeState(
                        screenName = "HomeScreen",
                        name = "테스터",
                    )
            }
        }
    }
}
