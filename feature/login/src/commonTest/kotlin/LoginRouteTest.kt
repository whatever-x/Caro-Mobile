import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.feature.login.loginNavigationCommand
import com.whatever.caro.feature.login.mvi.LoginSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LoginRouteTest : FunSpec() {
    init {
        test("가입 완료 사용자의 홈 이동은 인증 백스택을 초기화한다") {
            loginNavigationCommand(LoginSideEffect.NavigateHome) shouldBe
                NavCommand.ResetTo(HomeEntry)
        }

        test("미가입 사용자의 프로필 생성 이동은 로그인 화면을 백스택에 유지한다") {
            loginNavigationCommand(LoginSideEffect.NavigateCreateProfile) shouldBe
                NavCommand.To(CreateProfileEntry)
        }
    }
}
