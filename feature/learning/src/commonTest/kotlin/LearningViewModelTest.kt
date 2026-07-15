import app.cash.turbine.test
import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.study.StudySessionRepository
import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.model.learning.StudyEvaluation
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.learning.StudySession
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.LearningViewModel
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

        test("일일 학습 중단 시 이전까지 평가한 카드를 제출한 뒤 화면을 닫는다") {
            runTest(dispatcher) {
                val study =
                    FakeStudyRepository(
                        session =
                            StudySession.InProgress(
                                sessionId = 42L,
                                studiedCardCount = 0,
                                totalCardCount = 2,
                                cards =
                                    listOf(
                                        StudyCard(1L, "앞1", "뒤1"),
                                        StudyCard(2L, "앞2", "뒤2"),
                                    ),
                            ),
                    )
                val viewModel = createViewModel(cards = emptyList(), study = study, mode = LearningMode.DAILY)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()
                study.submitGate = CompletableDeferred()

                viewModel.sideEffect.test {
                    viewModel.intent(LearningIntent.ConfirmStop)
                    runCurrent()
                    expectNoEvents()
                    study.submitGate?.complete(Unit)
                    awaitItem() shouldBe LearningSideEffect.NavigateBack
                    study.submittedSessionId shouldBe 42L
                    study.submittedEvaluations shouldBe viewModel.state.value.evaluations
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("일일 학습 중단 확인을 연속 실행해도 평가를 한 번만 제출한다") {
            runTest(dispatcher) {
                val study =
                    FakeStudyRepository(
                        StudySession.InProgress(
                            sessionId = 42L,
                            studiedCardCount = 0,
                            totalCardCount = 2,
                            cards =
                                listOf(
                                    StudyCard(1L, "앞1", "뒤1"),
                                    StudyCard(2L, "앞2", "뒤2"),
                                ),
                        ),
                    )
                val viewModel = createViewModel(cards = emptyList(), study = study, mode = LearningMode.DAILY)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(LearningIntent.ConfirmStop)
                    viewModel.intent(LearningIntent.ConfirmStop)
                    advanceUntilIdle()

                    awaitItem() shouldBe LearningSideEffect.NavigateBack
                    expectNoEvents()
                    study.submitCount shouldBe 1
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("일일 학습 중단 제출이 실패하면 오류 다이얼로그를 표시하고 확인 시 화면을 닫는다") {
            runTest(dispatcher) {
                val study =
                    FakeStudyRepository(
                        StudySession.InProgress(
                            sessionId = 42L,
                            studiedCardCount = 0,
                            totalCardCount = 2,
                            cards =
                                listOf(
                                    StudyCard(1L, "앞1", "뒤1"),
                                    StudyCard(2L, "앞2", "뒤2"),
                                ),
                        ),
                    )
                val viewModel = createViewModel(cards = emptyList(), study = study, mode = LearningMode.DAILY)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()
                viewModel.intent(LearningIntent.RequestStop)
                advanceUntilIdle()
                study.submitError = IllegalStateException("제출 실패")

                viewModel.sideEffect.test {
                    viewModel.intent(LearningIntent.ConfirmStop)
                    advanceUntilIdle()

                    expectNoEvents()
                    viewModel.state.value.isSubmitting shouldBe false
                    viewModel.state.value.isShowErrorDialog shouldBe true
                    viewModel.state.value.errorMessage shouldBe null
                    viewModel.state.value.showStopDialog shouldBe false

                    viewModel.intent(LearningIntent.ConfirmError)
                    awaitItem() shouldBe LearningSideEffect.NavigateBack
                    study.submitCount shouldBe 1
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("서버 예외 메시지를 오류 다이얼로그 내용으로 노출한다") {
            runTest(dispatcher) {
                val study =
                    FakeStudyRepository(
                        StudySession.InProgress(
                            sessionId = 42L,
                            studiedCardCount = 0,
                            totalCardCount = 1,
                            cards = listOf(StudyCard(1L, "앞", "뒤")),
                        ),
                    )
                study.submitError = CaroServerException("500", "서버 메시지", "debug")
                val viewModel = createViewModel(cards = emptyList(), study = study, mode = LearningMode.DAILY)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()

                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()

                viewModel.state.value.isShowErrorDialog shouldBe true
                viewModel.state.value.errorMessage shouldBe "서버 메시지"
            }
        }

        test("제출 중 추가 평가는 무시한다") {
            runTest(dispatcher) {
                val study =
                    FakeStudyRepository(
                        StudySession.InProgress(
                            sessionId = 42L,
                            studiedCardCount = 0,
                            totalCardCount = 1,
                            cards = listOf(StudyCard(1L, "앞", "뒤")),
                        ),
                    ).apply {
                        submitGate = CompletableDeferred()
                    }
                val viewModel = createViewModel(cards = emptyList(), study = study, mode = LearningMode.DAILY)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()

                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                runCurrent()
                viewModel.intent(LearningIntent.Evaluate(StudyRating.FAIR))
                runCurrent()

                viewModel.state.value.evaluations.size shouldBe 1
                study.submitCount shouldBe 1

                study.submitGate?.complete(Unit)
                advanceUntilIdle()
            }
        }

        test("평가하지 않은 일일 학습 중단은 제출 없이 화면을 닫는다") {
            runTest(dispatcher) {
                val study =
                    FakeStudyRepository(
                        StudySession.InProgress(
                            sessionId = 42L,
                            studiedCardCount = 0,
                            totalCardCount = 1,
                            cards = listOf(StudyCard(1L, "앞", "뒤")),
                        ),
                    )
                val viewModel = createViewModel(cards = emptyList(), study = study, mode = LearningMode.DAILY)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(LearningIntent.ConfirmStop)
                    awaitItem() shouldBe LearningSideEffect.NavigateBack
                    study.submitCount shouldBe 0
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("전체 학습 중단은 누적 평가가 있어도 제출 없이 화면을 닫는다") {
            runTest(dispatcher) {
                val study = FakeStudyRepository()
                val cards = listOf(Card(1L, CardContent("앞1", "뒤1")), Card(2L, CardContent("앞2", "뒤2")))
                val viewModel = createViewModel(cards = cards, study = study, mode = LearningMode.ALL)
                viewModel.intent(LearningIntent.Load)
                advanceUntilIdle()
                viewModel.intent(LearningIntent.Evaluate(StudyRating.EASY))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(LearningIntent.ConfirmStop)
                    awaitItem() shouldBe LearningSideEffect.NavigateBack
                    study.submitCount shouldBe 0
                    cancelAndIgnoreRemainingEvents()
                }
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

private class FakeStudyRepository(
    private val session: StudySession? = null,
) : StudySessionRepository {
    var submitCount = 0
    var submittedSessionId: Long? = null
    var submittedEvaluations: List<StudyEvaluation> = emptyList()
    var submitGate: CompletableDeferred<Unit>? = null
    var submitError: Throwable? = null

    override suspend fun startDaily(deckId: Long): StudySession = session ?: error("daily API must not be called")

    override suspend fun submit(
        sessionId: Long,
        evaluations: List<StudyEvaluation>,
    ) {
        submitCount++
        submittedSessionId = sessionId
        submittedEvaluations = evaluations
        submitGate?.await()
        submitError?.let { throw it }
    }
}
