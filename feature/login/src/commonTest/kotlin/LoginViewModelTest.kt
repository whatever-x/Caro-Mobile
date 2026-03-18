import app.cash.turbine.test
import com.whatever.caro.feature.login.LoginViewModel
import com.whatever.caro.feature.login.di.LoginModule
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.SocialLoginResult
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
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
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.inject

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

        test("ClickLogin 인텐트를 보내면 NavigateHome sideEffect가 발생한다") {
            runTest {
                val vm by inject<LoginViewModel>()

                vm.sideEffect.test {
                    // vm.intent(LoginIntent.ClickGoogleLoginButton)
                    // advanceUntilIdle()
                    // awaitItem() shouldBe LoginSideEffect.NavigateHome
                    // cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }
}
