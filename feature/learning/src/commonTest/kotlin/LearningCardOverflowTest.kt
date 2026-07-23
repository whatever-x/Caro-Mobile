import com.whatever.caro.feature.learning.components.LearningCardTextUiState
import com.whatever.caro.feature.learning.components.learningCardTextUiState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LearningCardOverflowTest :
    FunSpec({
        test("앞면이 11줄을 초과할 때 앞면 더보기를 표시한다") {
            learningCardTextUiState(
                text = "front",
                hasVisualOverflow = true,
            ) shouldBe LearningCardTextUiState(text = "front", showMore = true)
        }

        test("뒷면이 11줄을 초과할 때 뒷면 더보기를 표시한다") {
            learningCardTextUiState(
                text = "back",
                hasVisualOverflow = true,
            ) shouldBe LearningCardTextUiState(text = "back", showMore = true)
        }

        test("현재 면이 11줄을 초과하지 않으면 더보기를 숨긴다") {
            learningCardTextUiState(
                text = "front",
                hasVisualOverflow = false,
            ) shouldBe LearningCardTextUiState(text = "front", showMore = false)
        }
    })
