import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.EditCardViewModel
import com.whatever.caro.feature.card.mvi.EditCardIntent
import com.whatever.caro.feature.card.mvi.EditCardSideEffect
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EditCardViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()
        val testCardId = 7L

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        fun createViewModel(cardRepository: CardRepository = mock<CardRepository>()): EditCardViewModel =
            EditCardViewModel(
                cardRepository = cardRepository,
                cardId = testCardId,
                front = "Run",
                back = "달리다",
                exceptionFilter = ExceptionFilter.None,
            )

        test("Front/Back 입력은 FIELD_MAX(500)자에서 잘린다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(EditCardIntent.UpdateFront("a".repeat(600)))
                viewModel.intent(EditCardIntent.UpdateBack("나".repeat(600)))
                advanceUntilIdle()

                viewModel.state.value.front.length shouldBe CardInputLimits.FIELD_MAX
                viewModel.state.value.back.length shouldBe CardInputLimits.FIELD_MAX
            }
        }

        test("ClickSwap 은 Front 와 Back 의 내용을 맞바꾼다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(EditCardIntent.ClickSwap)
                advanceUntilIdle()

                viewModel.state.value.front shouldBe "달리다"
                viewModel.state.value.back shouldBe "Run"
            }
        }

        test("ClickSave 는 수정된 카드를 저장하고 NavigateBack 을 emit 한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { updateCard(any(), any()) } returns Unit
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.intent(EditCardIntent.UpdateFront("Walk"))
                viewModel.intent(EditCardIntent.UpdateBack("걷다"))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(EditCardIntent.ClickSave)
                    advanceUntilIdle()

                    awaitItem() shouldBe EditCardSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) {
                    cardRepository.updateCard(
                        cardId = testCardId,
                        content = CardContent(front = "Walk", back = "걷다"),
                    )
                }
            }
        }

        test("ClickSave 를 빠르게 두 번 눌러도 수정 요청은 한 번만 보낸다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { updateCard(any(), any()) } returns Unit
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(EditCardIntent.ClickSave)
                    viewModel.intent(EditCardIntent.ClickSave)
                    advanceUntilIdle()

                    awaitItem() shouldBe EditCardSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) {
                    cardRepository.updateCard(
                        cardId = testCardId,
                        content = CardContent(front = "Run", back = "달리다"),
                    )
                }
            }
        }

        test("ClickSave 가 실패하면 ShowSaveError 를 emit 하고 isSaving 을 false 로 되돌린다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { updateCard(any(), any()) } throws RuntimeException("network")
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.sideEffect.test {
                    viewModel.intent(EditCardIntent.ClickSave)
                    advanceUntilIdle()

                    awaitItem() shouldBe EditCardSideEffect.ShowSaveError
                }
                viewModel.state.value.isSaving shouldBe false
            }
        }

        test("ClickBack 은 NavigateBack 을 emit 한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(EditCardIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe EditCardSideEffect.NavigateBack
                }
            }
        }
    }
}
