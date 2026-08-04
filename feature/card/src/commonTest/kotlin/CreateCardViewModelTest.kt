import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.mvi.CreateCardIntent
import com.whatever.caro.feature.card.mvi.CreateCardSideEffect
import com.whatever.caro.feature.card.mvi.StagedCard
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
class CreateCardViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()
        val testDeckId = 42L

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        fun createViewModel(cardRepository: CardRepository = mock<CardRepository>()): CreateCardViewModel =
            CreateCardViewModel(
                cardRepository = cardRepository,
                deckId = testDeckId,
                exceptionFilter = ExceptionFilter.None,
            )

        test("Front/Back 입력은 FIELD_MAX(500)자에서 잘린다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("a".repeat(600)))
                viewModel.intent(CreateCardIntent.UpdateBack("나".repeat(600)))
                advanceUntilIdle()

                viewModel.state.value.front.length shouldBe CardInputLimits.FIELD_MAX
                viewModel.state.value.back.length shouldBe CardInputLimits.FIELD_MAX
            }
        }

        test("Front 와 Back 이 모두 채워졌을 때만 isAddEnabled 가 true 다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                advanceUntilIdle()
                viewModel.state.value.isAddEnabled shouldBe false

                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                advanceUntilIdle()
                viewModel.state.value.isAddEnabled shouldBe true
            }
        }

        test("ClickSwap 은 Front 와 Back 의 내용을 맞바꾼다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickSwap)
                advanceUntilIdle()

                viewModel.state.value.front shouldBe "달리다"
                viewModel.state.value.back shouldBe "Run"
            }
        }

        test("ClickAddCard 는 카드를 추가하고 입력란을 비운다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                viewModel.state.value.addedCards shouldBe
                    listOf(StagedCard(id = 0L, content = CardContent(front = "Run", back = "달리다")))
                viewModel.state.value.front shouldBe ""
                viewModel.state.value.back shouldBe ""
                viewModel.state.value.isSaveEnabled shouldBe true
            }
        }

        test("ClickRemoveCard 는 해당 id 의 카드를 제거한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                viewModel.intent(CreateCardIntent.UpdateFront("Walk"))
                viewModel.intent(CreateCardIntent.UpdateBack("걷다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                viewModel.intent(CreateCardIntent.ClickRemoveCard(0L))
                advanceUntilIdle()

                viewModel.state.value.addedCards shouldBe
                    listOf(StagedCard(id = 1L, content = CardContent(front = "Walk", back = "걷다")))
            }
        }

        test("같은 id 를 중복 삭제(빠른 더블탭)해도 인접 카드는 영향받지 않는다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                viewModel.intent(CreateCardIntent.UpdateFront("Walk"))
                viewModel.intent(CreateCardIntent.UpdateBack("걷다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                // id 0(Run) 을 두 번 삭제 시도 — 두 번째는 무시되어 Walk 가 남아야 한다
                viewModel.intent(CreateCardIntent.ClickRemoveCard(0L))
                viewModel.intent(CreateCardIntent.ClickRemoveCard(0L))
                advanceUntilIdle()

                viewModel.state.value.addedCards shouldBe
                    listOf(StagedCard(id = 1L, content = CardContent(front = "Walk", back = "걷다")))
            }
        }

        test("ClickSave 는 추가된 카드를 deckId 와 함께 저장하고 NavigateBack 을 emit 한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { createCards(any(), any()) } returns Unit
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.ClickSave)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateCardSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) {
                    cardRepository.createCards(
                        deckId = testDeckId,
                        cards = listOf(CardContent(front = "Run", back = "달리다")),
                    )
                }
            }
        }

        test("ClickSave 를 빠르게 두 번 눌러도 저장 요청은 한 번만 보낸다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { createCards(any(), any()) } returns Unit
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.ClickSave)
                    viewModel.intent(CreateCardIntent.ClickSave)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateCardSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) {
                    cardRepository.createCards(
                        deckId = testDeckId,
                        cards = listOf(CardContent(front = "Run", back = "달리다")),
                    )
                }
            }
        }

        test("ClickSave 가 실패하면 ShowSaveError 를 emit 하고 isSaving 을 false 로 되돌린다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { createCards(any(), any()) } throws RuntimeException("network")
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.ClickSave)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateCardSideEffect.ShowSaveError
                }
                viewModel.state.value.isSaving shouldBe false
            }
        }

        test("입력이 없으면 ClickBack 은 바로 NavigateBack 을 emit 한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateCardSideEffect.NavigateBack
                }
                viewModel.state.value.isDiscardDialogVisible shouldBe false
            }
        }

        test("입력 중인 텍스트가 있으면 ClickBack 은 확인 다이얼로그만 띄운다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                    viewModel.intent(CreateCardIntent.ClickBack)
                    advanceUntilIdle()

                    expectNoEvents()
                }
                viewModel.state.value.isDiscardDialogVisible shouldBe true
            }
        }

        test("추가된 카드가 있으면 ClickBack 은 확인 다이얼로그를 띄우고 ConfirmDiscard 로 나간다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                viewModel.intent(CreateCardIntent.ClickBack)
                advanceUntilIdle()

                viewModel.state.value.isDiscardDialogVisible shouldBe true

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.ConfirmDiscard)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateCardSideEffect.NavigateBack
                }
            }
        }

        test("DismissDiscardDialog 는 다이얼로그만 닫고 화면을 유지한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.ClickBack)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateCardIntent.DismissDiscardDialog)
                    advanceUntilIdle()

                    expectNoEvents()
                }
                viewModel.state.value.isDiscardDialogVisible shouldBe false
                viewModel.state.value.front shouldBe "Run"
            }
        }

        test("MAX_CARDS 장에 도달하면 한도 다이얼로그를 띄우고 isAddEnabled 를 false 로 만든다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                repeat(CardInputLimits.MAX_CARDS) { index ->
                    viewModel.intent(CreateCardIntent.UpdateFront("front$index"))
                    viewModel.intent(CreateCardIntent.UpdateBack("back$index"))
                    viewModel.intent(CreateCardIntent.ClickAddCard)
                }
                advanceUntilIdle()

                viewModel.state.value.addedCards.size shouldBe CardInputLimits.MAX_CARDS
                viewModel.state.value.isMaxCardsDialogVisible shouldBe true
                viewModel.state.value.isMaxCardsReached shouldBe true

                viewModel.intent(CreateCardIntent.UpdateFront("overflow"))
                viewModel.intent(CreateCardIntent.UpdateBack("초과"))
                advanceUntilIdle()

                viewModel.state.value.isAddEnabled shouldBe false
            }
        }

        test("한도 도달 전에는 한도 다이얼로그를 띄우지 않는다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("Run"))
                viewModel.intent(CreateCardIntent.UpdateBack("달리다"))
                viewModel.intent(CreateCardIntent.ClickAddCard)
                advanceUntilIdle()

                viewModel.state.value.isMaxCardsDialogVisible shouldBe false
            }
        }

        test("FIELD_MAX 에 도달하면 isFrontMaxReached 가 true 다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(CreateCardIntent.UpdateFront("가".repeat(CardInputLimits.FIELD_MAX - 1)))
                advanceUntilIdle()
                viewModel.state.value.isFrontMaxReached shouldBe false

                viewModel.intent(CreateCardIntent.UpdateFront("가".repeat(CardInputLimits.FIELD_MAX + 10)))
                advanceUntilIdle()
                viewModel.state.value.isFrontMaxReached shouldBe true
            }
        }
    }
}
