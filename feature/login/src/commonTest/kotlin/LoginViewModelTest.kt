import com.whatever.caro.feature.login.di.LoginModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.ksp.generated.module
import org.koin.test.KoinTest

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest :
    FunSpec(),
    KoinTest {
    init {
        extensions(KoinExtension(LoginModule().module))

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
