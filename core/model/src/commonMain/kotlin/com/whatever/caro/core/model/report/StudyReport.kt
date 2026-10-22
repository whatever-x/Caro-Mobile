package com.whatever.caro.core.model.report

import com.whatever.caro.core.model.learning.StudyRating
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * 시작일과 종료일을 모두 포함하는 학습일 범위
 */
data class ReportPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

enum class ReportBucketUnit { DAY, WEEK, MONTH }

data class ReportRatingCounts(
    val easy: Long,
    val fair: Long,
    val again: Long,
) {
    val total: Long
        get() = easy + fair + again
}

data class ReportCounts(
    val reviewCount: Long,
    val uniqueCardCount: Long,
    val studyDayCount: Long,
    val ratings: ReportRatingCounts,
)

/**
 * 서버가 실제로 조회한 범위. 전체 기간에 기록이 없을 때만 startDate가 null이다.
 */
data class ReportRange(
    val startDate: LocalDate?,
    val endDate: LocalDate,
    val bucketUnit: ReportBucketUnit,
)

data class ReportBucket(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reviewCount: Long,
    val ratings: ReportRatingCounts,
)

/**
 * 조회한 덱. 전체는 deckId가 null이고 삭제된 덱은 이름 없이 deleted다.
 */
data class ReportScope(
    val deckId: Long?,
    val deckName: String?,
    val deleted: Boolean,
)

data class StudyReportSummary(
    val scope: ReportScope,
    val range: ReportRange,
    val selected: ReportCounts,
    val buckets: List<ReportBucket>,
    val restDates: List<LocalDate>,
)

data class ReportPage<T>(
    val items: List<T>,
    val nextCursor: String?,
)

data class ReportDeckOption(
    val deckId: Long,
    /** 삭제된 덱이면 null */
    val name: String?,
    val deleted: Boolean,
)

data class ReportSession(
    val sessionId: Long,
    val deckId: Long,
    val deckName: String?,
    val deckDeleted: Boolean,
    val studyDate: LocalDate,
    val counts: ReportCounts,
)

/**
 * 카드의 지금 내용. 카드나 카드가 속한 덱이 삭제됐으면 본문 없이 deleted다.
 */
data class ReportCardLabel(
    val cardId: Long,
    val deleted: Boolean,
    val front: String?,
    val back: String?,
)

data class ReportCardRecord(
    val card: ReportCardLabel,
    val reviewCount: Long,
    val lastRating: StudyRating,
    val lastStudyDate: LocalDate,
)

data class ReportReview(
    val reviewId: Long,
    val rating: StudyRating,
    val studyDate: LocalDate,
    /** 학습할 때의 시간대로 바꾼 평가 시각 */
    val reviewedAt: LocalDateTime,
)

data class ReportCardReviews(
    val card: ReportCardLabel,
    val range: ReportRange,
    val page: ReportPage<ReportReview>,
)
