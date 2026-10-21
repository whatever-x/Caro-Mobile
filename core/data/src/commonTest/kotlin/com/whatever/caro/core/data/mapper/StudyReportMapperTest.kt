package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucketUnit
import com.whatever.caro.core.model.report.ReportCardLabel
import com.whatever.caro.core.model.report.ReportRatingCounts
import com.whatever.caro.core.remote.dto.studyReport.response.RatingDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportBucketResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardLabelResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardReviewsResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCountsResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportDeckOptionResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportDeckPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportRangeResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportRatingsResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportReviewResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.ReportScopeResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.StudyReportSummaryResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class StudyReportMapperTest : FunSpec() {
    private val counts =
        ReportCountsResponseDto(
            reviewCount = 144,
            uniqueCardCount = 96,
            studyDayCount = 6,
            ratings = ReportRatingsResponseDto(easy = 82, fair = 38, again = 24),
        )

    private val range =
        ReportRangeResponseDto(
            startDate = "2026-10-15",
            endDate = "2026-10-21",
            bucketUnit = ReportRangeResponseDto.BucketUnitDto.DAY,
        )

    init {
        test("toStudyReportSummary는 날짜와 수치, 전체 누적을 모델로 바꾼다") {
            val summary =
                StudyReportSummaryResponse(
                    scope = ReportScopeResponseDto(deckId = null, deckName = null, deleted = false),
                    range = range,
                    lifetime = counts,
                    selected = counts,
                    buckets =
                        listOf(
                            ReportBucketResponseDto("2026-10-15", "2026-10-15", 18, ReportRatingsResponseDto(10, 5, 3)),
                            ReportBucketResponseDto(null, "2026-10-16", 1, null),
                        ),
                    restDates = listOf("2026-10-17", "bad-date"),
                ).toStudyReportSummary()

            summary.range.startDate shouldBe LocalDate(2026, 10, 15)
            summary.range.bucketUnit shouldBe ReportBucketUnit.DAY
            summary.lifetime?.reviewCount shouldBe 144
            summary.selected.ratings shouldBe ReportRatingCounts(easy = 82, fair = 38, again = 24)
            summary.buckets.size shouldBe 1
            summary.restDates shouldBe listOf(LocalDate(2026, 10, 17))
        }

        test("덱을 고른 응답에는 전체 누적이 없다") {
            val summary =
                StudyReportSummaryResponse(
                    scope = ReportScopeResponseDto(deckId = 3, deckName = null, deleted = true),
                    range = range.copy(startDate = null),
                    selected = counts,
                    buckets = null,
                    restDates = null,
                ).toStudyReportSummary()

            summary.lifetime.shouldBeNull()
            summary.scope.deleted shouldBe true
            summary.range.startDate.shouldBeNull()
        }

        test("선택 기간 수치가 없으면 잘못된 응답으로 처리한다") {
            shouldThrow<CaroInvalidResponseException> {
                StudyReportSummaryResponse(
                    scope = null,
                    range = range,
                    selected = null,
                    buckets = null,
                    restDates = null,
                ).toStudyReportSummary()
            }
        }

        test("삭제된 덱은 이름 없이 삭제 상태로 바꾸고 id가 없는 항목은 제외한다") {
            val page =
                ReportDeckPageResponse(
                    items =
                        listOf(
                            ReportDeckOptionResponseDto(deckId = 5, name = "영어", deleted = false),
                            ReportDeckOptionResponseDto(deckId = 4, name = null, deleted = true),
                            ReportDeckOptionResponseDto(deckId = null, name = "x", deleted = false),
                        ),
                    nextCursor = "c1",
                ).toReportDeckOptionPage()

            page.items.map { it.deckId to it.deleted } shouldBe listOf(5L to false, 4L to true)
            page.nextCursor shouldBe "c1"
        }

        test("삭제된 카드는 응답에 본문이 있어도 본문을 보여주지 않는다") {
            val page =
                ReportCardPageResponse(
                    items =
                        listOf(
                            ReportCardResponseDto(
                                cardId = 1,
                                deleted = true,
                                front = "leak",
                                back = "leak",
                                reviewCount = 3,
                                lastRating = RatingDto.FAIR,
                                lastReviewedAt = "2026-10-21T12:00:00Z",
                                lastStudyDate = "2026-10-21",
                            ),
                        ),
                    nextCursor = null,
                ).toReportCardRecordPage()

            page.items.single().card shouldBe ReportCardLabel(cardId = 1, deleted = true, front = null, back = null)
            page.items.single().lastRating shouldBe StudyRating.FAIR
        }

        test("평가 시각은 학습할 때의 시간대로 바꾼다") {
            val reviews =
                ReportCardReviewsResponse(
                    card = ReportCardLabelResponseDto(cardId = 7, deleted = false, front = "apple", back = "사과"),
                    range = range,
                    items =
                        listOf(
                            ReportReviewResponseDto(
                                reviewId = 9,
                                sessionId = 1,
                                rating = RatingDto.AGAIN,
                                studyDate = "2026-10-21",
                                reviewedAt = "2026-10-21T12:12:00Z",
                                timezone = "Asia/Seoul",
                            ),
                            ReportReviewResponseDto(10, 1, RatingDto.EASY, "2026-10-21", "not-time", "Asia/Seoul"),
                        ),
                    nextCursor = null,
                ).toReportCardReviews()

            reviews.card.front shouldBe "apple"
            reviews.page.items
                .single()
                .reviewedAt shouldBe LocalDateTime(2026, 10, 21, 21, 12)
        }
    }
}
