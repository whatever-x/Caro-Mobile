import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.create.CreateDeckViewModel
import com.whatever.caro.feature.deck.create.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.create.mvi.CreateDeckSideEffect
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
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
        val createdDeck =
            Deck(
                id = 7L,
                title = "영어 단어 2000개",
                description = "일상에서 많이 쓰는 단어",
                cardTotalCount = 0,
                todayLearningCount = 0,
                todayCompleteCount = 0,
                state = DeckState.NOT_STARTED,
            )

        fun viewModelWith(
            deckRepository: DeckRepository =
                mock { everySuspend { createDeck(any(), any()) } returns createdDeck },
        ) = CreateDeckViewModel(deckRepository, ExceptionFilter.None)

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("이름은 NAME_MAX(50)자에서 잘린다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.intent(CreateDeckIntent.UpdateName("가".repeat(60)))
                advanceUntilIdle()

                viewModel.state.value.name.length shouldBe DeckInputLimits.NAME_MAX
            }
        }

        test("설명은 DESC_MAX(500)자에서 잘린다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.intent(CreateDeckIntent.UpdateDescription("a".repeat(600)))
                advanceUntilIdle()

                viewModel.state.value.description.length shouldBe DeckInputLimits.DESC_MAX
            }
        }

        test("이름과 설명이 모두 채워졌을 때만 isConfirmEnabled가 true다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

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
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateDeckIntent.ClickBack)
                    awaitItem() shouldBe CreateDeckSideEffect.NavigateBack
                }
            }
        }

        test("입력 완료 후 ClickConfirm은 덱을 생성하고 Created를 방출한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> { everySuspend { createDeck(any(), any()) } returns createdDeck }
                val viewModel = viewModelWith(deckRepository)

                viewModel.intent(CreateDeckIntent.UpdateName("영어 단어 2000개"))
                viewModel.intent(CreateDeckIntent.UpdateDescription("일상에서 많이 쓰는 단어"))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateDeckIntent.ClickConfirm)
                    awaitItem() shouldBe CreateDeckSideEffect.Created(createdDeck)
                }
                verifySuspend {
                    deckRepository.createDeck(
                        name = "영어 단어 2000개",
                        description = "일상에서 많이 쓰는 단어",
                    )
                }
            }
        }

        test("덱 생성 실패 시 ShowError를 방출하고 isLoading이 false로 복귀한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { createDeck(any(), any()) } throws RuntimeException("network error")
                    }
                val viewModel = viewModelWith(deckRepository)

                viewModel.intent(CreateDeckIntent.UpdateName("영어 단어 2000개"))
                viewModel.intent(CreateDeckIntent.UpdateDescription("일상에서 많이 쓰는 단어"))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateDeckIntent.ClickConfirm)
                    awaitItem() shouldBe CreateDeckSideEffect.ShowError
                }

                viewModel.state.value.isLoading shouldBe false
            }
        }
    }
}
