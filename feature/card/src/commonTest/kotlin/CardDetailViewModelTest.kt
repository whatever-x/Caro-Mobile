import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.detail.CardDetailViewModel
import com.whatever.caro.feature.card.detail.cardDetailIndexForPage
import com.whatever.caro.feature.card.detail.cardDetailInitialPage
import com.whatever.caro.feature.card.detail.mvi.CardDetailIntent
import com.whatever.caro.feature.card.detail.mvi.CardDetailSideEffect
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
class CardDetailViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("초기 카드 목록을 불러오고 전달받은 cardId 위치를 선택한다") {
            runTest(dispatcher) {
                val viewModel = createCardDetailViewModel(initialCardId = 2L)

                advanceUntilIdle()

                viewModel.state.value.cards shouldBe cards
                viewModel.state.value.currentIndex shouldBe 1
                viewModel.state.value.currentCard
                    ?.id shouldBe 2L
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("카드 목록이 비어 있으면 이전 화면 이동을 요청한다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns emptyList()
                    }

                val viewModel =
                    createCardDetailViewModel(
                        deckRepository = deckRepository,
                    )

                viewModel.sideEffect.test {
                    advanceUntilIdle()

                    awaitItem() shouldBe CardDetailSideEffect.NavigateBack
                }
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("카드 목록 조회에 실패하면 로딩을 끝내고 오류를 알린다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } throws RuntimeException("network")
                    }

                val viewModel =
                    createCardDetailViewModel(
                        deckRepository = deckRepository,
                    )

                viewModel.sideEffect.test {
                    advanceUntilIdle()

                    awaitItem() shouldBe CardDetailSideEffect.ShowLoadError
                }
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("새로고침 실패 시 기존 카드를 유지하고 화면을 닫지 않는다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns cards
                    }
                val viewModel = createCardDetailViewModel(deckRepository = deckRepository)
                advanceUntilIdle()
                everySuspend { deckRepository.getDeckCards(any()) } throws RuntimeException("network")

                viewModel.sideEffect.test {
                    viewModel.intent(CardDetailIntent.RefreshCards)
                    advanceUntilIdle()

                    awaitItem() shouldBe CardDetailSideEffect.ShowRefreshError
                    expectNoEvents()
                }
                viewModel.state.value.cards shouldBe cards
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("새로고침 후 현재 카드가 유지되면 뒤집기 상태도 유지한다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns cards
                    }
                val viewModel = createCardDetailViewModel(deckRepository = deckRepository)
                advanceUntilIdle()

                viewModel.intent(CardDetailIntent.FlipCard)
                viewModel.intent(CardDetailIntent.RefreshCards)
                advanceUntilIdle()

                viewModel.state.value.currentCard
                    ?.id shouldBe 1L
                viewModel.state.value.isFlipped shouldBe true
            }
        }

        test("새로고침 후 같은 위치의 카드가 교체되면 뒤집기 상태를 초기화한다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns cards
                    }
                val viewModel = createCardDetailViewModel(deckRepository = deckRepository)
                advanceUntilIdle()
                everySuspend { deckRepository.getDeckCards(any()) } returns
                    listOf(
                        cards.first().copy(id = 4L),
                        cards[1],
                        cards[2],
                    )

                viewModel.intent(CardDetailIntent.FlipCard)
                viewModel.intent(CardDetailIntent.RefreshCards)
                advanceUntilIdle()

                viewModel.state.value.currentCard
                    ?.id shouldBe 4L
                viewModel.state.value.isFlipped shouldBe false
            }
        }

        test("카드를 넘기면 선택 인덱스를 바꾸고 뒤집기 상태를 초기화한다") {
            runTest(dispatcher) {
                val viewModel = createCardDetailViewModel()
                advanceUntilIdle()

                viewModel.intent(CardDetailIntent.FlipCard)
                viewModel.intent(CardDetailIntent.ChangeCard(index = 1))
                advanceUntilIdle()

                viewModel.state.value.currentIndex shouldBe 1
                viewModel.state.value.isFlipped shouldBe false
            }
        }

        test("ClickEdit 은 현재 카드 내용으로 수정 이동을 요청한다") {
            runTest(dispatcher) {
                val viewModel = createCardDetailViewModel(initialCardId = 2L)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CardDetailIntent.ClickEdit)

                    awaitItem() shouldBe
                        CardDetailSideEffect.NavigateToEdit(
                            cardId = 2L,
                            front = "Walk",
                            back = "걷다",
                        )
                }
            }
        }

        test("삭제 다이얼로그가 열린 동안에는 카드 목록을 새로고침하지 않는다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns cards
                    }
                val viewModel = createCardDetailViewModel(deckRepository = deckRepository)
                advanceUntilIdle()

                viewModel.intent(CardDetailIntent.ClickDelete)
                viewModel.intent(CardDetailIntent.RefreshCards)
                advanceUntilIdle()

                viewModel.state.value.isDeleteDialogVisible shouldBe true
                verifySuspend(exactly(1)) {
                    deckRepository.getDeckCards(deckId = 42L)
                }
            }
        }

        test("현재 카드를 삭제하면 다음 카드가 같은 위치를 차지한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { deleteCards(any()) } returns Unit
                    }
                val viewModel =
                    createCardDetailViewModel(
                        initialCardId = 2L,
                        cardRepository = cardRepository,
                    )
                advanceUntilIdle()

                viewModel.intent(CardDetailIntent.ClickDelete)
                viewModel.intent(CardDetailIntent.ConfirmDelete)
                advanceUntilIdle()

                viewModel.state.value.cards
                    .map(DeckCard::id) shouldBe listOf(1L, 3L)
                viewModel.state.value.currentIndex shouldBe 1
                viewModel.state.value.currentCard
                    ?.id shouldBe 3L
                viewModel.state.value.isDeleteDialogVisible shouldBe false
                verifySuspend(exactly(1)) {
                    cardRepository.deleteCards(cardIds = listOf(2L))
                }
            }
        }

        test("마지막 한 장을 삭제하면 이전 화면으로 이동한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { deleteCards(any()) } returns Unit
                    }
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns listOf(cards.first())
                    }
                val viewModel =
                    createCardDetailViewModel(
                        cardRepository = cardRepository,
                        deckRepository = deckRepository,
                    )

                viewModel.sideEffect.test {
                    advanceUntilIdle()
                    viewModel.intent(CardDetailIntent.ClickDelete)
                    viewModel.intent(CardDetailIntent.ConfirmDelete)
                    advanceUntilIdle()

                    awaitItem() shouldBe CardDetailSideEffect.NavigateBack
                }
                viewModel.state.value.cards shouldBe emptyList()
            }
        }

        test("삭제 실패 시 다이얼로그와 진행 상태를 닫고 오류를 알린다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { deleteCards(any()) } throws RuntimeException("network")
                    }
                val viewModel = createCardDetailViewModel(cardRepository = cardRepository)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CardDetailIntent.ClickDelete)
                    viewModel.intent(CardDetailIntent.ConfirmDelete)
                    advanceUntilIdle()

                    awaitItem() shouldBe CardDetailSideEffect.ShowDeleteError
                }
                viewModel.state.value.cards shouldBe cards
                viewModel.state.value.isDeleting shouldBe false
                viewModel.state.value.isDeleteDialogVisible shouldBe false
            }
        }

        test("무한 페이저의 기준 페이지와 카드 인덱스가 일치한다") {
            val initialPage = cardDetailInitialPage(currentIndex = 2, cardCount = 3)

            cardDetailIndexForPage(initialPage, cardCount = 3) shouldBe 2
            cardDetailIndexForPage(initialPage + 1, cardCount = 3) shouldBe 0
            cardDetailIndexForPage(initialPage - 1, cardCount = 3) shouldBe 1
            cardDetailInitialPage(currentIndex = 0, cardCount = 1) shouldBe 0
        }
    }
}

private val cards =
    listOf(
        DeckCard(
            id = 1L,
            content = CardContent(front = "Run", back = "달리다"),
            badge = CardBadge.NEW,
            reviewCount = 0,
        ),
        DeckCard(
            id = 2L,
            content = CardContent(front = "Walk", back = "걷다"),
            badge = CardBadge.REVIEW,
            reviewCount = 2,
        ),
        DeckCard(
            id = 3L,
            content = CardContent(front = "Jump", back = "뛰다"),
            badge = CardBadge.HARD,
            reviewCount = 4,
        ),
    )

private fun createCardDetailViewModel(
    initialCardId: Long = 1L,
    cardRepository: CardRepository = mock(),
    deckRepository: DeckRepository =
        mock {
            everySuspend { getDeckCards(any()) } returns cards
        },
): CardDetailViewModel =
    CardDetailViewModel(
        deckId = 42L,
        initialCardId = initialCardId,
        deckRepository = deckRepository,
        cardRepository = cardRepository,
        exceptionFilter = ExceptionFilter.None,
    )
