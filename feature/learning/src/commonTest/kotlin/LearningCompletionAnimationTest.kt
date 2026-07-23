import com.whatever.caro.feature.learning.components.LEARNING_COMPLETION_ANIMATION_ITERATIONS
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LearningCompletionAnimationTest :
    FunSpec({
        test("학습 완료 애니메이션은 한 번만 재생한다") {
            LEARNING_COMPLETION_ANIMATION_ITERATIONS shouldBe 1
        }
    })
