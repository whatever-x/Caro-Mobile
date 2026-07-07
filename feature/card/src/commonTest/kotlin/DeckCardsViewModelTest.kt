import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.DeckCardsViewModel
import com.whatever.caro.feature.card.mvi.DeckCardsIntent
import com.whatever.caro.feature.card.mvi.DeckCardsSideEffect
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DeckCardsViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()
        val testDeckId = 42L
        val testDeckTitle = "English"
        val cards =
            listOf(
                Card(id = 1L, content = CardContent(front = "Run", back = "달리다")),
                Card(id = 2L, content = CardContent(front = "Walk", back = "걷다")),
            )

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        fun createViewModel(
            cardRepository: CardRepository =
                mock {
                    everySuspend { getCardsByDeck(any()) } returns cards
                },
        ): DeckCardsViewModel =
            DeckCardsViewModel(
                cardRepository = cardRepository,
                deckId = testDeckId,
                deckTitle = testDeckTitle,
                exceptionFilter = ExceptionFilter.None,
            )

        test("init 은 카드 목록을 로드한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()

                advanceUntilIdle()

                viewModel.state.value.cards shouldBe cards.toPersistentList()
                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.hasLoadFailed shouldBe false
            }
        }

        test("ClickEditCard 는 현재 카드 내용으로 NavigateToEditCard 를 방출한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeckCardsIntent.ClickEditCard(cardId = 1L))
                    advanceUntilIdle()

                    awaitItem() shouldBe
                        DeckCardsSideEffect.NavigateToEditCard(
                            cardId = 1L,
                            front = "Run",
                            back = "달리다",
                        )
                }
            }
        }

        test("ClickAddCard 는 현재 덱 id 로 NavigateToCreateCard 를 방출한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeckCardsIntent.ClickAddCard)
                    advanceUntilIdle()

                    awaitItem() shouldBe DeckCardsSideEffect.NavigateToCreateCard(deckId = testDeckId)
                }
            }
        }

        test("RefreshCards 는 카드 목록을 다시 로드한다") {
            runTest(dispatcher) {
                val refreshedCards =
                    listOf(
                        Card(id = 1L, content = CardContent(front = "Run", back = "달리다")),
                        Card(id = 2L, content = CardContent(front = "Walk", back = "걷다")),
                        Card(id = 3L, content = CardContent(front = "Jump", back = "뛰다")),
                    )
                val cardRepository = RefreshCardRepository(cards = cards)
                val viewModel = createViewModel(cardRepository)
                advanceUntilIdle()

                cardRepository.cards = refreshedCards
                viewModel.intent(DeckCardsIntent.RefreshCards)
                advanceUntilIdle()

                viewModel.state.value.cards shouldBe refreshedCards.toPersistentList()
                cardRepository.loadCount shouldBe 2
            }
        }

        test("카드 목록 로드 실패 시 ShowLoadError 를 방출한다") {
            runTest(dispatcher) {
                val cardRepository =
                    mock<CardRepository> {
                        everySuspend { getCardsByDeck(any()) } throws RuntimeException("network")
                    }
                val viewModel = createViewModel(cardRepository)

                viewModel.sideEffect.test {
                    advanceUntilIdle()

                    awaitItem() shouldBe DeckCardsSideEffect.ShowLoadError
                }
                viewModel.state.value.hasLoadFailed shouldBe true
                viewModel.state.value.isLoading shouldBe false
            }
        }

        test("ClickBack 은 NavigateBack 을 방출한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeckCardsIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe DeckCardsSideEffect.NavigateBack
                }
            }
        }
    }
}

private class RefreshCardRepository(
    var cards: List<Card>,
) : CardRepository {
    var loadCount: Int = 0
        private set

    override suspend fun getCardsByDeck(deckId: Long): List<Card> {
        loadCount += 1
        return cards
    }

    override suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    ) = Unit

    override suspend fun updateCard(
        cardId: Long,
        content: CardContent,
    ) = Unit
}
