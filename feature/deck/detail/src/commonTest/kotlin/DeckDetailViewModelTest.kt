import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.DeckDetailPayload
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeckDetailViewModelTest :
    FunSpec({
        test("초기 state는 bottom sheet가 닫혀 있고 기본 정렬 옵션을 가진다") {
            val viewModel =
                DeckDetailViewModel(
                    navKey =
                        DeckDetailEntry(
                            payload =
                                DeckDetailPayload(
                                    deckId = 1L,
                                    deckTitle = "테스트 덱",
                                    deckDescription = "테스트 설명",
                                ),
                        ),
                    exceptionFilter = ExceptionFilter.None,
                )

            viewModel.state.value.isSortBottomSheetVisible shouldBe false
            viewModel.state.value.isDeckEditBottomSheetVisible shouldBe false
            viewModel.state.value.selectedSortOption shouldBe DeckDetailSortOption.CREATED
        }
    })
