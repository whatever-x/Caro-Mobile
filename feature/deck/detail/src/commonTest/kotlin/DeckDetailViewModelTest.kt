import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeckDetailViewModelTest :
    FunSpec({
        test("초기 state는 전달받은 홈 덱 데이터를 그대로 가진다") {
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
            val viewModel =
                DeckDetailViewModel(
                    deck = deck,
                    exceptionFilter = ExceptionFilter.None,
                )

            viewModel.state.value.isSortBottomSheetVisible shouldBe false
            viewModel.state.value.isDeckEditBottomSheetVisible shouldBe false
            viewModel.state.value.selectedSortOption shouldBe DeckDetailSortOption.CREATED
            viewModel.state.value.deck shouldBe deck
            viewModel.state.value.deck.todayProgress shouldBe 50
        }
    })
