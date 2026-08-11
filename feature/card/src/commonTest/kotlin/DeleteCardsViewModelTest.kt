import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.delete.DeleteCardsViewModel
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsIntent
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsSideEffect
import dev.mokkery.answering.calls
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
class DeleteCardsViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()
        val testDeckId = 42L
        val cards =
            listOf(
                DeckCard(
                    id = 1L,
                    content = CardContent(front = "apple", back = "사과"),
                    badge = CardBadge.NEW,
                    reviewCount = 0,
                ),
                DeckCard(
                    id = 2L,
                    content = CardContent(front = "run", back = "달리다"),
                    badge = CardBadge.NEW,
                    reviewCount = 0,
                ),
            )

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        fun createViewModel(
            cardRepository: CardRepository = mock(),
            deckRepository: DeckRepository =
                mock {
                    everySuspend { getDeckCards(any()) } returns cards
                },
        ): DeleteCardsViewModel =
            DeleteCardsViewModel(
                cardRepository = cardRepository,
                deckRepository = deckRepository,
                deckId = testDeckId,
                exceptionFilter = ExceptionFilter.None,
            )

        test("Initialize 는 deckId 로 카드 목록을 조회해 state 에 반영한다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns cards
                    }
                val viewModel = createViewModel(deckRepository = deckRepository)

                viewModel.intent(DeleteCardsIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.cards
                    .map { it.card } shouldBe cards
                viewModel.state.value.isLoading shouldBe false
                verifySuspend(exactly(1)) {
                    deckRepository.getDeckCards(deckId = testDeckId)
                }
            }
        }

        test("ClickCard 는 선택 상태를 토글한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(DeleteCardsIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                advanceUntilIdle()

                viewModel.state.value.selectedCardIds shouldBe setOf(1L)
                viewModel.state.value.isDeleteEnabled shouldBe true

                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                advanceUntilIdle()

                viewModel.state.value.selectedCardIds shouldBe emptySet()
                viewModel.state.value.isDeleteEnabled shouldBe false
            }
        }

        test("ClickSelectAll 은 전체 선택 상태를 토글한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(DeleteCardsIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(DeleteCardsIntent.ClickSelectAll)
                advanceUntilIdle()

                viewModel.state.value.selectedCardIds shouldBe setOf(1L, 2L)
                viewModel.state.value.cards
                    .map { item -> item.isSelected } shouldBe listOf(true, true)
                viewModel.state.value.isAllCardsSelected shouldBe true

                viewModel.intent(DeleteCardsIntent.ClickSelectAll)
                advanceUntilIdle()

                viewModel.state.value.selectedCardIds shouldBe emptySet()
                viewModel.state.value.cards
                    .map { item -> item.isSelected } shouldBe listOf(false, false)
                viewModel.state.value.isAllCardsSelected shouldBe false
            }
        }

        test("ClickSelectAll 은 일부 선택 상태에서 모든 카드를 선택한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(DeleteCardsIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                advanceUntilIdle()
                viewModel.intent(DeleteCardsIntent.ClickSelectAll)
                advanceUntilIdle()

                viewModel.state.value.selectedCardIds shouldBe setOf(1L, 2L)
                viewModel.state.value.cards
                    .map { item -> item.isSelected } shouldBe listOf(true, true)
                viewModel.state.value.isAllCardsSelected shouldBe true
            }
        }

        test("선택된 카드가 없으면 ClickDeleteSelected 는 다이얼로그를 열지 않는다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(DeleteCardsIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(DeleteCardsIntent.ClickDeleteSelected)
                advanceUntilIdle()

                viewModel.state.value.isDeleteConfirmDialogVisible shouldBe false
            }
        }

        test("선택된 카드가 있으면 ClickDeleteSelected 는 삭제 확인 다이얼로그를 연다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.intent(DeleteCardsIntent.Initialize)
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                advanceUntilIdle()
                viewModel.intent(DeleteCardsIntent.ClickDeleteSelected)
                advanceUntilIdle()

                viewModel.state.value.isDeleteConfirmDialogVisible shouldBe true
                viewModel.state.value.selectedCount shouldBe 1
            }
        }

        test("ClickDeleteConfirm 은 선택된 카드 id 를 삭제하고 NavigateBack 을 emit 한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { deleteCards(any()) } returns Unit
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.intent(DeleteCardsIntent.Initialize)
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 2L))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeleteCardsIntent.ClickDeleteSelected)
                    viewModel.intent(DeleteCardsIntent.ClickDeleteConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe DeleteCardsSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) {
                    cardRepository.deleteCards(cardIds = listOf(1L, 2L))
                }
            }
        }

        test("삭제 실패 시 ShowDeleteError 를 emit 하고 선택 상태를 유지한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { deleteCards(any()) } throws RuntimeException("network")
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.intent(DeleteCardsIntent.Initialize)
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeleteCardsIntent.ClickDeleteSelected)
                    viewModel.intent(DeleteCardsIntent.ClickDeleteConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe DeleteCardsSideEffect.ShowDeleteError
                }
                viewModel.state.value.selectedCardIds shouldBe setOf(1L)
                viewModel.state.value.isDeleting shouldBe false
            }
        }

        test("삭제 실패 시 카드 목록을 다시 조회해 서버 상태와 선택 상태를 동기화한다") {
            runTest(dispatcher) {
                var loadCount = 0
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { deleteCards(any()) } throws RuntimeException("network")
                    }
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } calls {
                            loadCount++
                            if (loadCount == 1) cards else listOf(cards[1])
                        }
                    }
                val viewModel = createViewModel(cardRepository, deckRepository)

                viewModel.intent(DeleteCardsIntent.Initialize)
                viewModel.intent(DeleteCardsIntent.ClickCard(cardId = 1L))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeleteCardsIntent.ClickDeleteSelected)
                    viewModel.intent(DeleteCardsIntent.ClickDeleteConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe DeleteCardsSideEffect.ShowDeleteError
                }
                viewModel.state.value.cards
                    .map { it.card.id } shouldBe listOf(2L)
                viewModel.state.value.selectedCardIds shouldBe emptySet()
                viewModel.state.value.isDeleting shouldBe false
                verifySuspend(exactly(2)) {
                    deckRepository.getDeckCards(deckId = testDeckId)
                }
            }
        }

        test("ClickCancel 과 ClickBack 은 NavigateBack 을 emit 한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                viewModel.sideEffect.test {
                    viewModel.intent(DeleteCardsIntent.ClickCancel)
                    viewModel.intent(DeleteCardsIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe DeleteCardsSideEffect.NavigateBack
                    awaitItem() shouldBe DeleteCardsSideEffect.NavigateBack
                }
            }
        }
    }
}
