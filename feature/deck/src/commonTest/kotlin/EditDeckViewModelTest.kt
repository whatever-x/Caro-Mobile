import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.edit.EditDeckViewModel
import com.whatever.caro.feature.deck.edit.mvi.EditDeckIntent
import com.whatever.caro.feature.deck.edit.mvi.EditDeckSideEffect
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
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
class EditDeckViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        fun viewModelWith(
            deckRepository: DeckRepository =
                mock { everySuspend { updateDeck(any(), any(), any()) } returns Unit },
            deckId: Long = 1L,
            deckName: String = "기존 이름",
            deckDescription: String = "기존 설명",
        ) = EditDeckViewModel(
            deckRepository = deckRepository,
            deckId = deckId,
            exceptionFilter = ExceptionFilter.None,
            deckName = deckName,
            deckDescription = deckDescription,
        )

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("초기 상태는 생성자로 전달된 덱 이름/설명을 반영한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith(deckName = "영어 단어 2000개", deckDescription = "일상 단어")

                viewModel.state.value.name shouldBe "영어 단어 2000개"
                viewModel.state.value.description shouldBe "일상 단어"
                viewModel.state.value.isConfirmEnabled shouldBe true
            }
        }

        test("이름은 NAME_MAX(50)자에서 잘린다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.intent(EditDeckIntent.UpdateName("가".repeat(60)))
                advanceUntilIdle()

                viewModel.state.value.name.length shouldBe DeckInputLimits.NAME_MAX
            }
        }

        test("설명은 DESC_MAX(500)자에서 잘린다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.intent(EditDeckIntent.UpdateDescription("a".repeat(600)))
                advanceUntilIdle()

                viewModel.state.value.description.length shouldBe DeckInputLimits.DESC_MAX
            }
        }

        test("이름이나 설명이 비면 isConfirmEnabled가 false다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.intent(EditDeckIntent.UpdateName(""))
                advanceUntilIdle()
                viewModel.state.value.isConfirmEnabled shouldBe false

                viewModel.intent(EditDeckIntent.UpdateName("영어 단어 2000개"))
                advanceUntilIdle()
                viewModel.state.value.isConfirmEnabled shouldBe true
            }
        }

        test("ClickBack은 NavigateBack을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(EditDeckIntent.ClickBack)
                    awaitItem() shouldBe EditDeckSideEffect.NavigateBack
                }
            }
        }

        test("ClickConfirm은 현재 입력으로 덱을 수정하고 NavigateBack을 방출한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> { everySuspend { updateDeck(any(), any(), any()) } returns Unit }
                val viewModel =
                    viewModelWith(
                        deckRepository = deckRepository,
                        deckId = 42L,
                        deckName = "영어 단어 2000개",
                        deckDescription = "일상 단어",
                    )

                viewModel.intent(EditDeckIntent.UpdateDescription("수정된 설명"))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(EditDeckIntent.ClickConfirm)
                    awaitItem() shouldBe EditDeckSideEffect.NavigateBack
                }
                verifySuspend {
                    deckRepository.updateDeck(
                        deckId = 42L,
                        name = "영어 단어 2000개",
                        description = "수정된 설명",
                    )
                }
            }
        }

        test("덱 수정 실패 시 ShowError를 방출하고 isLoading이 false로 복귀한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { updateDeck(any(), any(), any()) } throws RuntimeException("network error")
                    }
                val viewModel = viewModelWith(deckRepository = deckRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(EditDeckIntent.ClickConfirm)
                    awaitItem() shouldBe EditDeckSideEffect.ShowError
                }

                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("입력이 비어 isConfirmEnabled가 false면 ClickConfirm은 덱을 수정하지 않는다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> { everySuspend { updateDeck(any(), any(), any()) } returns Unit }
                val viewModel = viewModelWith(deckRepository = deckRepository)

                viewModel.intent(EditDeckIntent.UpdateDescription(""))
                advanceUntilIdle()

                viewModel.intent(EditDeckIntent.ClickConfirm)
                advanceUntilIdle()

                verifySuspend(exactly(0)) {
                    deckRepository.updateDeck(any(), any(), any())
                }
            }
        }
    }
}
