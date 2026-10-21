package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportBucket
import com.whatever.caro.core.model.report.ReportBucketUnit
import com.whatever.caro.core.model.report.ReportCardLabel
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportCardReviews
import com.whatever.caro.core.model.report.ReportCounts
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPage
import com.whatever.caro.core.model.report.ReportRange
import com.whatever.caro.core.model.report.ReportRatingCounts
import com.whatever.caro.core.model.report.ReportReview
import com.whatever.caro.core.model.report.ReportScope
import com.whatever.caro.core.model.report.ReportSession
import com.whatever.caro.core.model.report.StudyReportSummary
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
import com.whatever.caro.core.remote.dto.studyReport.response.ReportSessionPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportSessionResponseDto
import com.whatever.caro.core.remote.dto.studyReport.response.StudyReportSummaryResponse
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val RATING_EASY = "EASY"
private const val RATING_FAIR = "FAIR"
private const val RATING_AGAIN = "AGAIN"

internal fun StudyRating.toLastRatingQuery(): String =
    when (this) {
        StudyRating.EASY -> RATING_EASY
        StudyRating.FAIR -> RATING_FAIR
        StudyRating.AGAIN -> RATING_AGAIN
    }

internal fun StudyReportSummaryResponse.toStudyReportSummary(): StudyReportSummary =
    StudyReportSummary(
        scope =
            ReportScope(
                deckId = scope?.deckId,
                deckName = scope?.deckName,
                deleted = scope?.deleted ?: false,
            ),
        range = range.toReportRange("summary.range"),
        lifetime = lifetime?.toReportCounts(),
        selected = selected?.toReportCounts() ?: invalid("summary.selected is null"),
        buckets = buckets.orEmpty().mapNotNull { it.toReportBucket() },
        restDates = restDates.orEmpty().mapNotNull { it.toLocalDateOrNull() },
    )

internal fun ReportDeckPageResponse.toReportDeckOptionPage(): ReportPage<ReportDeckOption> =
    ReportPage(
        items = items.orEmpty().mapNotNull { it.toReportDeckOption() },
        nextCursor = nextCursor,
    )

internal fun ReportSessionPageResponse.toReportSessionPage(): ReportPage<ReportSession> =
    ReportPage(
        items = items.orEmpty().mapNotNull { it.toReportSession() },
        nextCursor = nextCursor,
    )

internal fun ReportCardPageResponse.toReportCardRecordPage(): ReportPage<ReportCardRecord> =
    ReportPage(
        items = items.orEmpty().mapNotNull { it.toReportCardRecord() },
        nextCursor = nextCursor,
    )

internal fun ReportCardReviewsResponse.toReportCardReviews(): ReportCardReviews =
    ReportCardReviews(
        card = card?.toReportCardLabel() ?: invalid("cardReviews.card is null"),
        range = range.toReportRange("cardReviews.range"),
        page =
            ReportPage(
                items = items.orEmpty().mapNotNull { it.toReportReview() },
                nextCursor = nextCursor,
            ),
    )

private fun ReportRangeResponseDto?.toReportRange(field: String): ReportRange {
    val range = this ?: invalid("$field is null")
    return ReportRange(
        startDate = range.startDate?.toLocalDateOrNull(),
        endDate = range.endDate?.toLocalDateOrNull() ?: invalid("$field.endDate is invalid"),
        bucketUnit =
            when (range.bucketUnit) {
                ReportRangeResponseDto.BucketUnitDto.WEEK -> ReportBucketUnit.WEEK
                ReportRangeResponseDto.BucketUnitDto.MONTH -> ReportBucketUnit.MONTH
                ReportRangeResponseDto.BucketUnitDto.DAY, null -> ReportBucketUnit.DAY
            },
    )
}

private fun ReportCountsResponseDto.toReportCounts(): ReportCounts =
    ReportCounts(
        reviewCount = reviewCount ?: 0,
        uniqueCardCount = uniqueCardCount ?: 0,
        studyDayCount = studyDayCount ?: 0,
        ratings = ratings.toReportRatingCounts(),
    )

private fun ReportRatingsResponseDto?.toReportRatingCounts(): ReportRatingCounts =
    ReportRatingCounts(
        easy = this?.easy ?: 0,
        fair = this?.fair ?: 0,
        again = this?.again ?: 0,
    )

private fun ReportBucketResponseDto.toReportBucket(): ReportBucket? {
    val start = startDate?.toLocalDateOrNull() ?: return null
    val end = endDate?.toLocalDateOrNull() ?: return null
    return ReportBucket(
        startDate = start,
        endDate = end,
        reviewCount = reviewCount ?: 0,
        ratings = ratings.toReportRatingCounts(),
    )
}

private fun ReportDeckOptionResponseDto.toReportDeckOption(): ReportDeckOption? {
    val id = deckId ?: return null
    return ReportDeckOption(deckId = id, name = name, deleted = deleted ?: (name == null))
}

private fun ReportSessionResponseDto.toReportSession(): ReportSession? {
    val id = sessionId ?: return null
    return ReportSession(
        sessionId = id,
        deckId = deckId ?: return null,
        deckName = deckName,
        deckDeleted = deckDeleted ?: (deckName == null),
        studyDate = studyDate?.toLocalDateOrNull() ?: return null,
        counts = counts?.toReportCounts() ?: return null,
    )
}

private fun ReportCardResponseDto.toReportCardRecord(): ReportCardRecord? {
    val id = cardId ?: return null
    return ReportCardRecord(
        card = cardLabel(id, deleted, front, back),
        reviewCount = reviewCount ?: 0,
        lastRating = lastRating?.toStudyRating() ?: return null,
        lastStudyDate = lastStudyDate?.toLocalDateOrNull() ?: return null,
    )
}

private fun ReportCardLabelResponseDto.toReportCardLabel(): ReportCardLabel? {
    val id = cardId ?: return null
    return cardLabel(id, deleted, front, back)
}

// 삭제 표시가 없더라도 본문이 없으면 삭제된 카드로 보고 본문을 보여주지 않는다
private fun cardLabel(
    cardId: Long,
    deleted: Boolean?,
    front: String?,
    back: String?,
): ReportCardLabel {
    val isDeleted = deleted ?: (front == null && back == null)
    return ReportCardLabel(
        cardId = cardId,
        deleted = isDeleted,
        front = if (isDeleted) null else front.orEmpty(),
        back = if (isDeleted) null else back.orEmpty(),
    )
}

@OptIn(ExperimentalTime::class)
private fun ReportReviewResponseDto.toReportReview(): ReportReview? {
    val id = reviewId ?: return null
    val zone = timezone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: return null
    val instant = reviewedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return ReportReview(
        reviewId = id,
        rating = rating?.toStudyRating() ?: return null,
        studyDate = studyDate?.toLocalDateOrNull() ?: return null,
        reviewedAt = instant.toLocalDateTime(zone),
    )
}

private fun RatingDto.toStudyRating(): StudyRating =
    when (this) {
        RatingDto.EASY -> StudyRating.EASY
        RatingDto.FAIR -> StudyRating.FAIR
        RatingDto.AGAIN -> StudyRating.AGAIN
    }

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun invalid(message: String): Nothing = throw CaroInvalidResponseException(debugMessage = message)
