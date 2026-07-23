import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.model.CardReviewState
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DeckDetailViewModelTest :
    FunSpec({
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("초기 state는 전달받은 홈 덱 데이터를 그대로 가진다") {
            val deck = createDeck()
            val viewModel = createViewModel(deck = deck)

            viewModel.state.value.isSortBottomSheetVisible shouldBe false
            viewModel.state.value.isDeckEditBottomSheetVisible shouldBe false
            viewModel.state.value.selectedSortOption shouldBe DeckDetailSortOption.CREATED
            viewModel.state.value.deck shouldBe deck
            viewModel.state.value.deck.todayProgress shouldBe 50
        }

        test("init 은 덱의 카드 목록을 로드한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel(deck = createDeck())

                advanceUntilIdle()

                viewModel.state.value.deckCardList shouldBe
                    persistentListOf(
                        CardItem(id = 1L, front = "Run", back = "달리다"),
                        CardItem(id = 2L, front = "Walk", back = "걷다"),
                    )
                viewModel.state.value.isCardListLoading shouldBe false
            }
        }

        test("덱 카드의 badge/복습 수가 CardItem 에 매핑된다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns
                            listOf(
                                DeckCard(
                                    id = 7L,
                                    content = CardContent(front = "Run", back = "달리다"),
                                    badge = CardBadge.HARD,
                                    reviewCount = 5,
                                ),
                            )
                    }
                val viewModel = createViewModel(deck = createDeck(), deckRepository = deckRepository)

                advanceUntilIdle()

                viewModel.state.value.deckCardList shouldBe
                    persistentListOf(
                        CardItem(
                            id = 7L,
                            front = "Run",
                            back = "달리다",
                            reviewCount = 5,
                            reviewState = CardReviewState.HARD,
                        ),
                    )
            }
        }

        test("RefreshCards 는 카드 목록을 다시 로드한다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } returns createDeckCards()
                    }
                val viewModel = createViewModel(deck = createDeck(), deckRepository = deckRepository)
                advanceUntilIdle()

                everySuspend { deckRepository.getDeckCards(any()) } returns
                    createDeckCards() +
                    DeckCard(
                        id = 3L,
                        content = CardContent(front = "Jump", back = "뛰다"),
                        badge = CardBadge.NEW,
                        reviewCount = 0,
                    )
                viewModel.intent(DeckDetailIntent.RefreshCards)
                advanceUntilIdle()

                viewModel.state.value.deckCardList.size shouldBe 3
            }
        }

        test("카드 목록 로드 실패 시 ShowCardLoadError 를 방출한다") {
            runTest(dispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDeckCards(any()) } throws RuntimeException("network")
                    }
                val viewModel = createViewModel(deck = createDeck(), deckRepository = deckRepository)

                viewModel.sideEffect.test {
                    advanceUntilIdle()

                    awaitItem() shouldBe DeckDetailSideEffect.ShowCardLoadError
                }
                viewModel.state.value.isCardListLoading shouldBe false
                viewModel.state.value.isEmptyDeckCard shouldBe true
            }
        }

        test("ClickAddCard 는 카드 생성 이동 side effect를 방출한다") {
            runTest(dispatcher) {
                val deck = createDeck()
                val viewModel = createViewModel(deck = deck)

                viewModel.sideEffect.test {
                    viewModel.intent(DeckDetailIntent.ClickAddCard)

                    awaitItem() shouldBe DeckDetailSideEffect.NavigateToCreateCard(deckId = deck.id)
                }
            }
        }

        test("ClickAllStudy 는 전체 학습 이동 side effect를 방출한다") {
            runTest(dispatcher) {
                val deck = createDeck()
                val viewModel = createViewModel(deck = deck)

                viewModel.sideEffect.test {
                    viewModel.intent(DeckDetailIntent.ClickAllStudy)

                    awaitItem() shouldBe
                        DeckDetailSideEffect.NavigateToLearning(
                            deckId = deck.id,
                            mode = LearningMode.ALL,
                        )
                }
            }
        }

        test("ClickDailyStudy 는 일일 학습 이동 side effect를 방출한다") {
            runTest(dispatcher) {
                val deck = createDeck()
                val viewModel = createViewModel(deck = deck)

                viewModel.sideEffect.test {
                    viewModel.intent(DeckDetailIntent.ClickDailyStudy)

                    awaitItem() shouldBe
                        DeckDetailSideEffect.NavigateToLearning(
                            deckId = deck.id,
                            mode = LearningMode.DAILY,
                        )
                }
            }
        }

        test("ClickEditCardList 는 카드 목록 편집 이동 side effect를 방출한다") {
            runTest(dispatcher) {
                val deck = createDeck()
                val viewModel = createViewModel(deck = deck)

                viewModel.sideEffect.test {
                    viewModel.intent(DeckDetailIntent.ClickEditCardList)

                    awaitItem() shouldBe DeckDetailSideEffect.NavigateToEditCardList(deckId = deck.id)
                }
            }
        }

        test("ClickDeckEditBottomSheetEdit 은 바텀시트를 닫고 덱 수정 이동 side effect를 방출한다") {
            runTest(dispatcher) {
                val deck = createDeck()
                val viewModel = createViewModel(deck = deck)

                viewModel.intent(DeckDetailIntent.ClickEditDeck)
                dispatcher.scheduler.advanceUntilIdle()
                viewModel.state.value.isDeckEditBottomSheetVisible shouldBe true

                viewModel.sideEffect.test {
                    viewModel.intent(DeckDetailIntent.ClickDeckEditBottomSheetEdit)

                    awaitItem() shouldBe DeckDetailSideEffect.NavigateToEditDeck(deckId = deck.id)
                }

                viewModel.state.value.isDeckEditBottomSheetVisible shouldBe false
            }
        }

        test("ClickCard 는 현재 카드 내용으로 카드 수정 이동 side effect를 방출한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel(deck = createDeck())
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(DeckDetailIntent.ClickCard(cardId = 1L))

                    awaitItem() shouldBe
                        DeckDetailSideEffect.NavigateToEditCard(
                            cardId = 1L,
                            front = "Run",
                            back = "달리다",
                        )
                }
            }
        }
    })

private fun createDeckCards(): List<DeckCard> =
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
            badge = CardBadge.NEW,
            reviewCount = 0,
        ),
    )

private fun createViewModel(
    deck: Deck,
    deckRepository: DeckRepository =
        mock {
            everySuspend { getDeckCards(any()) } returns createDeckCards()
        },
): DeckDetailViewModel =
    DeckDetailViewModel(
        deck = deck,
        deckRepository = deckRepository,
        exceptionFilter = ExceptionFilter.None,
    )

private fun createDeck(): Deck =
    Deck(
        id = 1L,
        title = "테스트 덱",
        description = "테스트 설명",
        cardTotalCount = 24,
        todayLearningCount = 12,
        todayCompleteCount = 6,
        state = DeckState.LEARNING,
    )
