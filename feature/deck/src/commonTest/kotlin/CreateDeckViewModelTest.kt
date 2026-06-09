import app.cash.turbine.test
import com.whatever.caro.feature.deck.CreateDeckViewModel
import com.whatever.caro.feature.deck.DeckInputLimits
import com.whatever.caro.feature.deck.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.mvi.CreateDeckSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CreateDeckViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("이름은 NAME_MAX(50)자에서 잘린다") {
            runTest(testDispatcher) {
                val viewModel = CreateDeckViewModel()

                viewModel.intent(CreateDeckIntent.UpdateName("가".repeat(60)))
                advanceUntilIdle()

                viewModel.state.value.name.length shouldBe DeckInputLimits.NAME_MAX
            }
        }

        test("설명은 DESC_MAX(500)자에서 잘린다") {
            runTest(testDispatcher) {
                val viewModel = CreateDeckViewModel()

                viewModel.intent(CreateDeckIntent.UpdateDescription("a".repeat(600)))
                advanceUntilIdle()

                viewModel.state.value.description.length shouldBe DeckInputLimits.DESC_MAX
            }
        }

        test("이름과 설명이 모두 채워졌을 때만 isConfirmEnabled가 true다") {
            runTest(testDispatcher) {
                val viewModel = CreateDeckViewModel()

                viewModel.intent(CreateDeckIntent.UpdateName("영어 단어 2000개"))
                advanceUntilIdle()
                viewModel.state.value.isConfirmEnabled shouldBe false

                viewModel.intent(CreateDeckIntent.UpdateDescription("일상에서 많이 쓰는 단어"))
                advanceUntilIdle()
                viewModel.state.value.isConfirmEnabled shouldBe true
            }
        }

        test("ClickBack은 NavigateBack을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = CreateDeckViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateDeckIntent.ClickBack)
                    awaitItem() shouldBe CreateDeckSideEffect.NavigateBack
                }
            }
        }

        test("입력 완료 후 ClickConfirm은 NavigateBack을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = CreateDeckViewModel()

                viewModel.intent(CreateDeckIntent.UpdateName("영어 단어 2000개"))
                viewModel.intent(CreateDeckIntent.UpdateDescription("일상에서 많이 쓰는 단어"))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateDeckIntent.ClickConfirm)
                    awaitItem() shouldBe CreateDeckSideEffect.NavigateBack
                }
            }
        }
    }
}
