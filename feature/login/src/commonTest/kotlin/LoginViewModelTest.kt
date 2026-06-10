import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.login.di.loginModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import org.koin.test.KoinTest

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest :
    FunSpec(),
    KoinTest {
    init {
        extensions(
            KoinExtension(
                listOf(
                    loginModule,
                    module {
                        single<ExceptionFilter> { ExceptionFilter.None }
                    },
                ),
            ),
        )

        val testDispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            testDispatcher.cancel()
        }
    }
}
