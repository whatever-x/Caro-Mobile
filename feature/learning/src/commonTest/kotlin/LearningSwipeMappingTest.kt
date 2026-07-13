import com.whatever.caro.core.model.study.StudyRating
import com.whatever.caro.core.ui.swipe.SwipeDirection
import com.whatever.caro.feature.learning.mapper.toRating
import com.whatever.caro.feature.learning.mapper.toSwipeDirection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LearningSwipeMappingTest :
    FunSpec({
        test("swipe directions use the Figma evaluation mapping") {
            SwipeDirection.LEFT.toRating() shouldBe StudyRating.EASY
            SwipeDirection.UP.toRating() shouldBe StudyRating.FAIR
            SwipeDirection.RIGHT.toRating() shouldBe StudyRating.AGAIN
        }

        test("evaluation buttons animate in the matching swipe direction") {
            StudyRating.EASY.toSwipeDirection() shouldBe SwipeDirection.LEFT
            StudyRating.FAIR.toSwipeDirection() shouldBe SwipeDirection.UP
            StudyRating.AGAIN.toSwipeDirection() shouldBe SwipeDirection.RIGHT
        }
    })
