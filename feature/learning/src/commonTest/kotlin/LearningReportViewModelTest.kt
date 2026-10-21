import com.whatever.caro.core.data.repository.report.StudyReportRepository
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucketUnit
import com.whatever.caro.core.model.report.ReportCardLabel
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportCardReviews
import com.whatever.caro.core.model.report.ReportCounts
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPage
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.ReportRange
import com.whatever.caro.core.model.report.ReportRatingCounts
import com.whatever.caro.core.model.report.ReportScope
import com.whatever.caro.core.model.report.ReportSession
import com.whatever.caro.core.model.report.StudyReportSummary
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.learning.report.LearningReportViewModel
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import com.whatever.caro.feature.learning.report.ReportPeriods
import com.whatever.caro.feature.learning.report.ReportToday
import com.whatever.caro.feature.learning.report.mvi.LearningReportIntent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LearningReportViewModelTest : FunSpec() {
    private val today = LocalDate(2026, 10, 21)
    private val recent7 = ReportPeriod(LocalDate(2026, 10, 15), today)

    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest { Dispatchers.setMain(dispatcher) }
        afterTest { Dispatchers.resetMain() }

        fun createViewModel(
            repository: FakeStudyReportRepository,
            deckId: Long? = null,
        ) = LearningReportViewModel(
            deckId = deckId,
            repository = repository,
            reportToday = ReportToday { today },
            exceptionFilter = ExceptionFilter.None,
        )

        test("일반 진입은 전체와 오늘을 포함한 최근 7일로 불러온다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)

                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                repository.summaryRequests shouldBe listOf(null to recent7)
                repository.cardRequests.shouldBeEmpty()
                viewModel.state.value.summary
                    .shouldNotBeNull()
                viewModel.state.value.isSummaryLoading shouldBe false
            }
        }

        test("덱에서 들어오면 그 덱의 리포트와 카드별 평가를 불러오고 덱 이름을 채운다") {
            runTest {
                val repository = FakeStudyReportRepository(scopeName = "영어 단어")
                val viewModel = createViewModel(repository, deckId = 3L)

                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                repository.summaryRequests shouldBe listOf(3L to recent7)
                repository.cardRequests shouldBe listOf(CardRequest(3L, recent7, null, null))
                viewModel.state.value.filter.deckName shouldBe "영어 단어"
                viewModel.state.value.cards.size shouldBe 1
            }
        }

        test("시트에서 고른 덱과 기간은 체크를 눌러야 한 번에 반영된다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.OpenSheet)
                advanceUntilIdle()
                viewModel.intent(LearningReportIntent.SelectDraftPreset(ReportPeriodPreset.RECENT_30))
                viewModel.intent(LearningReportIntent.SelectDraftDeck(ReportDeckOption(deckId = 5L, name = "CS", deleted = false)))
                advanceUntilIdle()

                repository.summaryRequests.size shouldBe 1
                viewModel.state.value.filter.deckId
                    .shouldBeNull()

                viewModel.intent(LearningReportIntent.ApplySheet)
                advanceUntilIdle()

                val recent30 = ReportPeriod(LocalDate(2026, 9, 22), today)
                repository.summaryRequests shouldBe listOf(null to recent7, 5L to recent30)
                viewModel.state.value.sheet
                    .shouldBeNull()
                viewModel.state.value.filter.preset shouldBe ReportPeriodPreset.RECENT_30
            }
        }

        test("시트를 취소하면 고른 값을 버리고 다시 불러오지 않는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.OpenSheet)
                viewModel.intent(LearningReportIntent.SelectDraftPreset(ReportPeriodPreset.RECENT_30))
                viewModel.intent(LearningReportIntent.DismissSheet)
                advanceUntilIdle()

                repository.summaryRequests.size shouldBe 1
                viewModel.state.value.filter.period shouldBe recent7
            }
        }

        test("같은 조건으로 체크하면 닫기만 하고 다시 불러오지 않는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.OpenSheet)
                viewModel.intent(LearningReportIntent.ApplySheet)
                advanceUntilIdle()

                repository.summaryRequests.size shouldBe 1
                viewModel.state.value.sheet
                    .shouldBeNull()
            }
        }

        test("직접 설정은 미래 날짜를 고르지 않고, 완료해도 체크 전에는 불러오지 않는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.OpenSheet)
                viewModel.intent(LearningReportIntent.SelectDraftPreset(ReportPeriodPreset.CUSTOM))
                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 1)))
                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 22)))
                advanceUntilIdle()
                viewModel.state.value.sheet
                    ?.dateEditor
                    ?.endDate
                    .shouldBeNull()

                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 16)))
                viewModel.intent(LearningReportIntent.ConfirmDateRange)
                advanceUntilIdle()

                val custom = ReportPeriod(LocalDate(2026, 10, 1), LocalDate(2026, 10, 16))
                viewModel.state.value.sheet
                    ?.draft
                    ?.period shouldBe custom
                viewModel.state.value.sheet
                    ?.dateEditor
                    .shouldBeNull()
                repository.summaryRequests.size shouldBe 1

                viewModel.intent(LearningReportIntent.ApplySheet)
                advanceUntilIdle()

                repository.summaryRequests.last() shouldBe (null to custom)
            }
        }

        test("직접 설정에서 기간을 고르지 않으면 체크를 눌러도 반영하지 않는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.OpenSheet)
                viewModel.intent(LearningReportIntent.SelectDraftPreset(ReportPeriodPreset.CUSTOM))
                viewModel.intent(LearningReportIntent.CloseDateEditor)
                advanceUntilIdle()
                // 직접 설정을 열었다 닫아도 기존 선택(최근 7일)은 그대로다
                viewModel.state.value.sheet
                    ?.canApply shouldBe true
                viewModel.state.value.sheet
                    ?.draft
                    ?.period shouldBe recent7
            }
        }

        test("새 조건을 반영하면 이전 조건의 늦은 응답은 버린다") {
            runTest {
                val gate = CompletableDeferred<Unit>()
                val repository = FakeStudyReportRepository(firstSummaryGate = gate)
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.ClickAllPeriod)
                advanceUntilIdle()
                gate.complete(Unit)
                advanceUntilIdle()

                viewModel.state.value.filter.period
                    .shouldBeNull()
                viewModel.state.value.summary
                    ?.range
                    ?.startDate
                    .shouldBeNull()
                repository.summaryRequests shouldBe listOf(null to recent7, null to null)
            }
        }

        test("처음 불러오기에 실패하면 빈 기록으로 바꾸지 않고 다시 시도할 수 있다") {
            runTest {
                val repository = FakeStudyReportRepository(failSummaryTimes = 1)
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.state.value.hasSummaryError shouldBe true
                viewModel.state.value.summary
                    .shouldBeNull()

                viewModel.intent(LearningReportIntent.ClickRetry)
                advanceUntilIdle()

                viewModel.state.value.hasSummaryError shouldBe false
                viewModel.state.value.summary
                    .shouldNotBeNull()
            }
        }

        test("카드 평가 필터를 바꾸면 카드 목록만 처음부터 다시 불러온다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository, deckId = 3L)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.SelectCardRating(StudyRating.AGAIN))
                advanceUntilIdle()

                repository.summaryRequests.size shouldBe 1
                repository.cardRequests.last() shouldBe CardRequest(3L, recent7, StudyRating.AGAIN, null)
            }
        }

        test("최근 7일과 30일은 오늘을 포함하고 직접 설정은 하루부터 오늘까지 고를 수 있다") {
            ReportPeriods.presetPeriod(ReportPeriodPreset.RECENT_7, today) shouldBe recent7
            ReportPeriods.presetPeriod(ReportPeriodPreset.RECENT_30, LocalDate(2026, 3, 1)) shouldBe
                ReportPeriod(LocalDate(2026, 1, 31), LocalDate(2026, 3, 1))
            ReportPeriods.isSelectable(today, today, today) shouldBe true
            ReportPeriods.isSelectable(today, LocalDate(2026, 10, 20), today) shouldBe false
            ReportPeriods.isSelectable(LocalDate(2026, 10, 20), LocalDate(2026, 10, 22), today) shouldBe false
            ReportPeriods.isSelectable(null, today, today) shouldBe false
        }
    }
}

private data class CardRequest(
    val deckId: Long,
    val period: ReportPeriod?,
    val rating: StudyRating?,
    val cursor: String?,
)

private class FakeStudyReportRepository(
    private val scopeName: String? = null,
    private val firstSummaryGate: CompletableDeferred<Unit>? = null,
    private var failSummaryTimes: Int = 0,
) : StudyReportRepository {
    val summaryRequests = mutableListOf<Pair<Long?, ReportPeriod?>>()
    val cardRequests = mutableListOf<CardRequest>()

    override suspend fun getSummary(
        deckId: Long?,
        period: ReportPeriod?,
    ): StudyReportSummary {
        summaryRequests += deckId to period
        if (summaryRequests.size == 1) firstSummaryGate?.await()
        if (failSummaryTimes > 0) {
            failSummaryTimes--
            throw IllegalStateException("summary failed")
        }
        val counts = ReportCounts(3, 2, 1, ReportRatingCounts(1, 1, 1))
        return StudyReportSummary(
            scope = ReportScope(deckId = deckId, deckName = scopeName, deleted = false),
            range = ReportRange(startDate = period?.startDate, endDate = LocalDate(2026, 10, 21), bucketUnit = ReportBucketUnit.DAY),
            lifetime = if (deckId == null) counts else null,
            selected = counts,
            buckets = emptyList(),
            restDates = emptyList(),
        )
    }

    override suspend fun getDeckOptions(cursor: String?): ReportPage<ReportDeckOption> =
        ReportPage(items = listOf(ReportDeckOption(deckId = 5L, name = "CS", deleted = false)), nextCursor = null)

    override suspend fun getSessions(
        deckId: Long?,
        period: ReportPeriod?,
        cursor: String?,
    ): ReportPage<ReportSession> = ReportPage(items = emptyList(), nextCursor = null)

    override suspend fun getCards(
        deckId: Long,
        period: ReportPeriod?,
        lastRating: StudyRating?,
        cursor: String?,
    ): ReportPage<ReportCardRecord> {
        cardRequests += CardRequest(deckId, period, lastRating, cursor)
        return ReportPage(
            items =
                listOf(
                    ReportCardRecord(
                        card = ReportCardLabel(cardId = 10L, deleted = false, front = "apple", back = "사과"),
                        reviewCount = 2,
                        lastRating = StudyRating.FAIR,
                        lastStudyDate = LocalDate(2026, 10, 21),
                    ),
                ),
            nextCursor = null,
        )
    }

    override suspend fun getCardReviews(
        deckId: Long,
        cardId: Long,
        period: ReportPeriod?,
        cursor: String?,
    ): ReportCardReviews = error("not used")
}
