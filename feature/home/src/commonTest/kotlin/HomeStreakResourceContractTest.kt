import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.img_home_streak_active
import caromobile.core.designsystem.generated.resources.img_home_streak_broken
import caromobile.core.designsystem.generated.resources.img_home_streak_not_started
import caromobile.core.designsystem.generated.resources.img_streak_broken
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

class HomeStreakResourceContractTest : FunSpec() {
    init {
        test("연속 학습 상태별 bitmap 리소스를 제공한다") {
            listOf(
                Res.drawable.img_home_streak_not_started,
                Res.drawable.img_home_streak_active,
                Res.drawable.img_home_streak_broken,
                Res.drawable.img_streak_broken,
            ) shouldHaveSize 4
        }
    }
}
