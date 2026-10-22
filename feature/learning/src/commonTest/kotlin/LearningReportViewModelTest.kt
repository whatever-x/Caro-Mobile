import com.whatever.caro.core.data.repository.report.StudyReportRepository
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucket
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
import com.whatever.caro.feature.learning.report.ReportQuickRange
import com.whatever.caro.feature.learning.report.ReportToday
import com.whatever.caro.feature.learning.report.mvi.LearningReportIntent
import com.whatever.caro.feature.learning.report.mvi.LearningReportSideEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

        test("일반 진입은 전체 덱과 첫 학습일부터 오늘까지로 불러온다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)

                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                repository.summaryRequests shouldBe listOf(null to null)
                repository.cardRequests.shouldBeEmpty()
                viewModel.state.value.filter.preset shouldBe ReportPeriodPreset.ALL
                viewModel.state.value.summary
                    .shouldNotBeNull()
            }
        }

        test("덱에서 들어오면 그 덱의 전체 기간 리포트와 카드별 평가를 불러오고 덱 이름을 채운다") {
            runTest {
                val repository = FakeStudyReportRepository(scopeName = "영어 단어")
                val viewModel = createViewModel(repository, deckId = 3L)

                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                repository.summaryRequests shouldBe listOf(3L to null)
                repository.cardRequests shouldBe listOf(CardRequest(3L, null, null, null))
                viewModel.state.value.filter.deckName shouldBe "영어 단어"
                viewModel.state.value.cards.size shouldBe 1
            }
        }

        test("기간 칩은 누르는 즉시 다시 불러오고 이미 고른 칩은 다시 불러오지 않는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                advanceUntilIdle()

                repository.summaryRequests shouldBe listOf(null to null, null to recent7)
            }
        }

        test("덱 선택은 기간을 두고 바로 적용하며, 같은 덱이면 닫기만 하고 삭제한 덱은 목록에 없다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.OpenDeckSheet)
                advanceUntilIdle()
                viewModel.state.value.deckSheet
                    ?.decks
                    ?.map { it.deckId } shouldBe listOf(5L)

                viewModel.intent(LearningReportIntent.SelectDeck(ReportDeckOption(deckId = 5L, name = "CS", deleted = false)))
                advanceUntilIdle()

                repository.summaryRequests.last() shouldBe (5L to recent7)
                viewModel.state.value.deckSheet
                    .shouldBeNull()

                viewModel.intent(LearningReportIntent.OpenDeckSheet)
                viewModel.intent(LearningReportIntent.SelectDeck(ReportDeckOption(deckId = 5L, name = "CS", deleted = false)))
                advanceUntilIdle()

                repository.summaryRequests.size shouldBe 3
            }
        }

        test("직접 설정은 미래 날짜를 고르지 않고, 완료하면 바로 적용한다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.CUSTOM))
                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 1)))
                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 22)))
                advanceUntilIdle()
                viewModel.state.value.dateEditor
                    ?.endDate
                    .shouldBeNull()
                repository.summaryRequests.size shouldBe 1

                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 16)))
                viewModel.intent(LearningReportIntent.ConfirmDateRange)
                advanceUntilIdle()

                val custom = ReportPeriod(LocalDate(2026, 10, 1), LocalDate(2026, 10, 16))
                repository.summaryRequests.last() shouldBe (null to custom)
                viewModel.state.value.dateEditor
                    .shouldBeNull()
                viewModel.state.value.filter.preset shouldBe ReportPeriodPreset.CUSTOM
            }
        }

        test("직접 설정을 열면 직접 설정한 기간, 없으면 보고 있는 기간을 채우고 전체를 고르면 잊는다") {
            runTest {
                val viewModel = createViewModel(FakeStudyReportRepository())
                viewModel.intent(LearningReportIntent.Initialize)
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.CUSTOM))
                advanceUntilIdle()

                val recent =
                    viewModel.state.value.dateEditor
                        .shouldNotBeNull()
                recent.startDate shouldBe LocalDate(2026, 10, 15)
                recent.endDate shouldBe today

                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 1)))
                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 16)))
                viewModel.intent(LearningReportIntent.ConfirmDateRange)
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.CUSTOM))
                advanceUntilIdle()

                val remembered =
                    viewModel.state.value.dateEditor
                        .shouldNotBeNull()
                remembered.startDate shouldBe LocalDate(2026, 10, 1)
                remembered.endDate shouldBe LocalDate(2026, 10, 16)

                viewModel.intent(LearningReportIntent.DismissDateEditor)
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.ALL))
                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.CUSTOM))
                advanceUntilIdle()

                viewModel.state.value.dateEditor
                    ?.startDate
                    .shouldBeNull()
            }
        }

        test("빠른 선택은 날짜를 채우고, 날짜를 직접 바꾸면 빠른 선택이 풀린다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.CUSTOM))
                viewModel.intent(LearningReportIntent.SelectQuickRange(ReportQuickRange.THIS_WEEK))
                advanceUntilIdle()

                val editor =
                    viewModel.state.value.dateEditor
                        .shouldNotBeNull()
                editor.startDate shouldBe LocalDate(2026, 10, 19)
                editor.endDate shouldBe today
                editor.quickRange shouldBe ReportQuickRange.THIS_WEEK

                viewModel.intent(LearningReportIntent.ClickDate(LocalDate(2026, 10, 5)))
                advanceUntilIdle()

                viewModel.state.value.dateEditor
                    ?.quickRange
                    .shouldBeNull()
            }
        }

        test("월 막대를 누르면 그 달을 다시 불러와 주차로 더하고, 같은 막대를 다시 누르면 닫는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.ClickBar(october))
                advanceUntilIdle()

                repository.summaryRequests.last() shouldBe (null to octoberPeriod)
                val weeks =
                    viewModel.state.value.monthDetail
                        ?.weeks
                        .shouldNotBeNull()
                weeks.map { it.number to it.reviewCount } shouldBe listOf(1 to 4L, 2 to 0L, 3 to 0L, 4 to 6L)
                weeks.first().period shouldBe ReportPeriod(LocalDate(2026, 10, 1), LocalDate(2026, 10, 4))

                viewModel.intent(LearningReportIntent.ClickBar(october))
                advanceUntilIdle()

                viewModel.state.value.monthDetail
                    .shouldBeNull()
            }
        }

        test("주차로 들어간 기간은 뒤로 가기로 한 단계씩 돌아가고, 돌아갈 단계가 없으면 리포트를 닫는다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(LearningReportIntent.ClickBar(october))
                advanceUntilIdle()
                val week =
                    viewModel.state.value.monthDetail
                        ?.weeks!!
                        .last()

                viewModel.intent(LearningReportIntent.ClickWeek(week))
                advanceUntilIdle()

                repository.summaryRequests.last() shouldBe (null to week.period)
                viewModel.state.value.filter.preset shouldBe ReportPeriodPreset.CUSTOM
                viewModel.state.value.monthDetail
                    .shouldBeNull()

                // 주차 목록이 열린 그 달로 돌아간다
                viewModel.intent(LearningReportIntent.ClickBack)
                advanceUntilIdle()
                viewModel.state.value.filter.period
                    .shouldBeNull()
                viewModel.state.value.monthDetail
                    ?.bucket shouldBe october

                // 목록만 닫는다
                viewModel.intent(LearningReportIntent.ClickBack)
                advanceUntilIdle()
                viewModel.state.value.monthDetail
                    .shouldBeNull()
                viewModel.state.value.handlesBack shouldBe false

                viewModel.intent(LearningReportIntent.ClickBack)
                advanceUntilIdle()
                viewModel.sideEffect.first() shouldBe LearningReportSideEffect.NavigateBack
            }
        }

        test("기간 칩으로 바꾸면 막대로 쌓인 단계를 지운다") {
            runTest {
                val repository = FakeStudyReportRepository()
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()
                viewModel.intent(LearningReportIntent.ClickBar(october))
                advanceUntilIdle()
                viewModel.intent(LearningReportIntent.ClickMonthAll)
                advanceUntilIdle()
                repository.summaryRequests.last() shouldBe (null to octoberPeriod)
                viewModel.state.value.backSteps.size shouldBe 1

                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                advanceUntilIdle()

                viewModel.state.value.backSteps
                    .shouldBeEmpty()
            }
        }

        test("새 조건을 반영하면 이전 조건의 늦은 응답은 버린다") {
            runTest {
                val gate = CompletableDeferred<Unit>()
                val repository = FakeStudyReportRepository(firstSummaryGate = gate)
                val viewModel = createViewModel(repository)
                viewModel.intent(LearningReportIntent.Initialize)
                advanceUntilIdle()

                viewModel.intent(LearningReportIntent.SelectPreset(ReportPeriodPreset.RECENT_7))
                advanceUntilIdle()
                gate.complete(Unit)
                advanceUntilIdle()

                viewModel.state.value.summary
                    ?.range
                    ?.startDate shouldBe recent7.startDate
                repository.summaryRequests shouldBe listOf(null to null, null to recent7)
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
                repository.cardRequests.last() shouldBe CardRequest(3L, null, StudyRating.AGAIN, null)
            }
        }

        test("최근 7일과 빠른 선택은 오늘까지 포함하고 직접 설정은 하루부터 오늘까지 고를 수 있다") {
            ReportPeriods.presetPeriod(ReportPeriodPreset.RECENT_7, today) shouldBe recent7
            ReportPeriods.presetPeriod(ReportPeriodPreset.ALL, today).shouldBeNull()
            ReportPeriods.quickPeriod(ReportQuickRange.THIS_WEEK, today) shouldBe ReportPeriod(LocalDate(2026, 10, 19), today)
            ReportPeriods.quickPeriod(ReportQuickRange.THIS_WEEK, LocalDate(2026, 10, 19)) shouldBe
                ReportPeriod(LocalDate(2026, 10, 19), LocalDate(2026, 10, 19))
            ReportPeriods.quickPeriod(ReportQuickRange.THIS_MONTH, LocalDate(2026, 11, 1)) shouldBe
                ReportPeriod(LocalDate(2026, 11, 1), LocalDate(2026, 11, 1))
            ReportPeriods.quickPeriod(ReportQuickRange.RECENT_30, LocalDate(2026, 3, 1)) shouldBe
                ReportPeriod(LocalDate(2026, 1, 31), LocalDate(2026, 3, 1))
            ReportPeriods.isSelectable(today, today, today) shouldBe true
            ReportPeriods.isSelectable(today, LocalDate(2026, 10, 20), today) shouldBe false
            ReportPeriods.isSelectable(LocalDate(2026, 10, 20), LocalDate(2026, 10, 22), today) shouldBe false
            ReportPeriods.isSelectable(null, today, today) shouldBe false
        }

        test("주차는 월요일에 시작하고 잘린 달에서도 그 달 1일이 든 주를 1주차로 센다") {
            // 2026.3.1은 일요일이라 1주차는 하루다
            val march = ReportPeriod(LocalDate(2026, 3, 12), LocalDate(2026, 3, 31))
            ReportPeriods.splitWeeks(march, emptyList()).map { it.number to it.period } shouldBe
                listOf(
                    3 to ReportPeriod(LocalDate(2026, 3, 12), LocalDate(2026, 3, 15)),
                    4 to ReportPeriod(LocalDate(2026, 3, 16), LocalDate(2026, 3, 22)),
                    5 to ReportPeriod(LocalDate(2026, 3, 23), LocalDate(2026, 3, 29)),
                    6 to ReportPeriod(LocalDate(2026, 3, 30), LocalDate(2026, 3, 31)),
                )
        }
    }
}

private val octoberPeriod = ReportPeriod(LocalDate(2026, 10, 1), LocalDate(2026, 10, 21))
private val october = ReportBucket(octoberPeriod.startDate, octoberPeriod.endDate, 10, ReportRatingCounts(5, 3, 2))

private fun dayBucket(
    date: LocalDate,
    count: Long,
) = ReportBucket(date, date, count, ReportRatingCounts(count, 0, 0))

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
            range =
                ReportRange(
                    startDate = period?.startDate,
                    endDate = LocalDate(2026, 10, 21),
                    // 전체 기간은 월 단위 차트로 둔다
                    bucketUnit = if (period == null) ReportBucketUnit.MONTH else ReportBucketUnit.DAY,
                ),
            selected = counts,
            // 10월을 다시 불러오면 1주차(10.2)와 4주차(10.19, 10.21)에 기록이 있다
            buckets =
                if (period == octoberPeriod) {
                    listOf(
                        dayBucket(LocalDate(2026, 10, 2), 4),
                        dayBucket(LocalDate(2026, 10, 19), 1),
                        dayBucket(LocalDate(2026, 10, 21), 5),
                    )
                } else {
                    emptyList()
                },
            restDates = emptyList(),
        )
    }

    override suspend fun getDeckOptions(cursor: String?): ReportPage<ReportDeckOption> =
        ReportPage(
            items =
                listOf(
                    ReportDeckOption(deckId = 5L, name = "CS", deleted = false),
                    ReportDeckOption(deckId = 4L, name = null, deleted = true),
                ),
            nextCursor = null,
        )

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
