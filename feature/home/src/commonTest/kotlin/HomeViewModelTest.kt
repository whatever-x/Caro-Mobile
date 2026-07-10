import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
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
class HomeViewModelTest : FunSpec() {
    init {
        val testDispatcher = StandardTestDispatcher()

        fun viewModelWith(
            deckRepository: DeckRepository =
                mock { everySuspend { getDecks() } returns emptyList() },
        ) = HomeViewModel(deckRepository, ExceptionFilter.None)

        beforeTest {
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        test("ClickSettingButton 은 NavigateToSetting 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickSettingButton)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToSetting
                }
            }
        }

        test("ClickProfile 은 NavigateToProfile 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickProfile)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToProfile
                }
            }
        }

        test("ClickCreateDeck 은 NavigateToCreateDeck 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToCreateDeck
                }
            }
        }

        test("ClickCreateDeckButton 은 NavigateToCreateDeck 을 방출한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickCreateDeckButton)
                    advanceUntilIdle()

                    awaitItem() shouldBe HomeSideEffect.NavigateToCreateDeck
                }
            }
        }

        test("덱 클릭 시 덱 상세 이동 side effect에 홈 덱 데이터를 그대로 포함한다") {
            runTest(testDispatcher) {
                val viewModel = viewModelWith()
                val deck =
                    Deck(
                        id = 1L,
                        title = "테스트 덱",
                        description = "테스트 설명",
                        cardTotalCount = 24,
                        todayLearningCount = 12,
                        todayCompleteCount = 6,
                        state = DeckState.LEARNING,
                    )

                viewModel.sideEffect.test {
                    viewModel.intent(HomeIntent.ClickDeckButton(deck = deck))

                    val sideEffect = awaitItem()
                    sideEffect shouldBe HomeSideEffect.NavigateToDeckDetail(deck = deck)

                    val payloadDeck = (sideEffect as HomeSideEffect.NavigateToDeckDetail).deck
                    payloadDeck.title shouldBe "테스트 덱"
                    payloadDeck.description shouldBe "테스트 설명"
                    payloadDeck.cardTotalCount shouldBe 24
                    payloadDeck.todayLearningCount shouldBe 12
                    payloadDeck.todayCompleteCount shouldBe 6
                    payloadDeck.todayProgress shouldBe 50
                    payloadDeck.state shouldBe DeckState.LEARNING
                }
            }
        }

        test("Initialize 는 덱을 불러와 state.decks 를 채우고 로딩을 종료한다") {
            runTest(testDispatcher) {
                val decks =
                    listOf(
                        Deck(
                            id = 1L,
                            title = "덱1",
                            description = "설명1",
                            cardTotalCount = 10,
                            todayLearningCount = 4,
                            todayCompleteCount = 2,
                            state = DeckState.LEARNING,
                        ),
                    )
                val deckRepository =
                    mock<DeckRepository> { everySuspend { getDecks() } returns decks }
                val viewModel = viewModelWith(deckRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
                viewModel.state.value.isDeckEmpty shouldBe false
                viewModel.state.value.decks
                    .toList() shouldBe decks
                verifySuspend { deckRepository.getDecks() }
            }
        }

        test("Initialize 중 덱 조회가 실패하면 isLoading 이 false 로 복귀한다") {
            runTest(testDispatcher) {
                val deckRepository =
                    mock<DeckRepository> {
                        everySuspend { getDecks() } throws RuntimeException("network error")
                    }
                val viewModel = viewModelWith(deckRepository)

                viewModel.intent(HomeIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.isLoading shouldBe false
            }
        }
    }
}
