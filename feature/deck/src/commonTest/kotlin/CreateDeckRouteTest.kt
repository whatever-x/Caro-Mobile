import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.feature.deck.create.createDeckCreatedSnackbar
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class CreateDeckRouteTest : FunSpec() {
    init {
        test("생성 완료 스낵바 액션은 생성된 덱 상세로 이동한다") {
            val deck =
                Deck(
                    id = 7L,
                    title = "영어 단어",
                    description = "일상 단어",
                    cardTotalCount = 0,
                    todayLearningCount = 0,
                    todayCompleteCount = 0,
                    state = DeckState.NOT_STARTED,
                )
            val navDispatcher = RecordingNavigationDispatcher()

            val snackbar =
                createDeckCreatedSnackbar(
                    deck = deck,
                    message = "덱 생성이 완료되었습니다.",
                    actionLabel = "바로가기",
                    navDispatcher = navDispatcher,
                )

            snackbar.message shouldBe "덱 생성이 완료되었습니다."
            snackbar.actionLabel shouldBe "바로가기"
            snackbar.onAction?.invoke()
            navDispatcher.emittedCommands shouldBe
                listOf(
                    NavCommand.To(
                        DeckDetailEntry(
                            payload = DeckDetailEntry.Payload(deck = deck),
                        ),
                    ),
                )
        }
    }
}

private class RecordingNavigationDispatcher : NavigationDispatcher {
    override val commands: Flow<NavCommand> = emptyFlow()
    val emittedCommands = mutableListOf<NavCommand>()

    override suspend fun emit(command: NavCommand) {
        emittedCommands += command
    }
}
