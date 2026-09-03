import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.feature.profile.create.createProfileNavigationCommand
import com.whatever.caro.feature.profile.create.mvi.CreateProfileSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CreateProfileRouteTest : FunSpec() {
    init {
        test("프로필 생성 완료 후 홈 이동은 인증 플로우 백스택을 초기화한다") {
            createProfileNavigationCommand(CreateProfileSideEffect.NavigateHome) shouldBe
                NavCommand.ResetTo(HomeEntry)
        }

        test("프로필 생성 취소는 로그인 화면으로 백스택을 초기화한다") {
            createProfileNavigationCommand(CreateProfileSideEffect.NavigateLogin) shouldBe
                NavCommand.ResetTo(LoginEntry)
        }
    }
}
