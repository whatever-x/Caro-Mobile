import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.DeckDetailPayload
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeckDetailViewModelTest :
    FunSpec({
        test("init() 호출 시 state 갱신") {
            val viewModel =
                DeckDetailViewModel(
                    navKey =
                        DeckDetailEntry(
                            payload = DeckDetailPayload(deckId = 1L),
                        ),
                )

            viewModel.init()

            viewModel.state.value shouldBe
                DeckDetailState(
                    screenName = "DeckDetailScreen",
                    deckId = 1L,
                )
        }
    })
