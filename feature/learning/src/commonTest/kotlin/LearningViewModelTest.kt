import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.study.StudySessionRepository
import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.learning.StudySession
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.LearningViewModel
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class LearningViewModelTest :
    FunSpec({
        val dispatcher = StandardTestDispatcher()
        beforeTest { Dispatchers.setMain(dispatcher) }
        afterTest { Dispatchers.resetMain() }

        test("전체 학습은 카드 저장소의 모든 카드를 로컬 세션으로 불러온다") {
            runTest(dispatcher) {
                val cards = listOf(Card(1, CardContent("앞1", "뒤1")), Card(2, CardContent("앞2", "뒤2")))
                val viewModel = createViewModel(cards)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.state.value.cards
                    .map { it.id } shouldBe listOf(1L, 2L)
                viewModel.state.value.totalCount shouldBe 2
            }
        }

        test("전체 학습의 마지막 평가는 서버 제출 없이 완료된다") {
            runTest(dispatcher) {
                val study = FakeStudyRepository()
                val viewModel = createViewModel(listOf(Card(1, CardContent("앞", "뒤"))), study)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()
                viewModel.state.value.isCompleted shouldBe true
                study.submitCount shouldBe 0
            }
        }

        test("전체 학습 카드가 없으면 빈 상태로 완료된다") {
            runTest(dispatcher) {
                val viewModel = createViewModel(emptyList())
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.state.value.isCompleted shouldBe true
                viewModel.state.value.totalCount shouldBe 0
            }
        }

        test("진행 중 뒤로가기는 학습 중단 다이얼로그를 표시한다") {
            runTest(dispatcher) {
                val viewModel = createViewModel(listOf(Card(1, CardContent("앞", "뒤"))))
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()

                viewModel.intent(LearningIntent.RequestStop)
                advanceUntilIdle()

                viewModel.state.value.showStopDialog shouldBe true
            }
        }

        test("학습 모드를 화면 상태로 노출한다") {
            LearningMode.entries.forEach { mode ->
                createViewModel(cards = emptyList(), mode = mode).state.value.mode shouldBe mode
            }
        }

        test("완료 상태의 뒤로가기는 중단 다이얼로그 없이 화면을 닫는다") {
            runTest(dispatcher) {
                val viewModel = createViewModel(emptyList())
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(LearningIntent.RequestStop)
                    awaitItem() shouldBe LearningSideEffect.NavigateBack
                    viewModel.state.value.showStopDialog shouldBe false
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("각 카드가 표시된 시점부터 평가까지의 시간을 독립적으로 기록한다") {
            runTest(dispatcher) {
                val timeSource = TestTimeSource()
                val cards = listOf(Card(1, CardContent("앞1", "뒤1")), Card(2, CardContent("앞2", "뒤2")))
                val viewModel = createViewModel(cards, timeSource = timeSource)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()

                timeSource += 1_200.milliseconds
                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()
                timeSource += 350.milliseconds
                viewModel.intent(LearningIntent.Evaluate(StudyRating.FAIR))
                advanceUntilIdle()

                viewModel.state.value.evaluations
                    .map { it.timeMs } shouldBe listOf(1_200, 350)
            }
        }
    })

private fun createViewModel(
    cards: List<Card>,
    study: FakeStudyRepository = FakeStudyRepository(),
    timeSource: TimeSource = TimeSource.Monotonic,
    mode: LearningMode = LearningMode.ALL,
) = LearningViewModel(1L, mode, study, FakeCardRepository(cards), ExceptionFilter.None, timeSource)

private class FakeCardRepository(
    private val cards: List<Card>,
) : CardRepository {
    override suspend fun getCards(deckId: Long) = cards

    override suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    ) = Unit

    override suspend fun deleteCards(cardIds: List<Long>) = Unit
}

private class FakeStudyRepository : StudySessionRepository {
    var submitCount = 0

    override suspend fun startDaily(deckId: Long): StudySession = error("daily API must not be called")

    override suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
    ) {
        submitCount++
    }
}
