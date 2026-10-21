package com.whatever.caro.core.remote.dto.studyReport.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Caro-Backend feat/learning-report-api의 /study-reports 응답. 서버 배포 후 swagger-sync 결과와 대조한다.

@Serializable
data class StudyReportSummaryResponse(
    val scope: ReportScopeResponseDto?,
    val range: ReportRangeResponseDto?,
    /** 덱을 고르면 응답에 없다 */
    val lifetime: ReportCountsResponseDto? = null,
    val selected: ReportCountsResponseDto?,
    val buckets: List<ReportBucketResponseDto>?,
    val restDates: List<String>?,
)

@Serializable
data class ReportScopeResponseDto(
    val deckId: Long?,
    val deckName: String?,
    val deleted: Boolean?,
)

@Serializable
data class ReportRangeResponseDto(
    val startDate: String?,
    val endDate: String?,
    val bucketUnit: BucketUnitDto?,
) {
    @Serializable
    enum class BucketUnitDto {
        @SerialName("DAY")
        DAY,

        @SerialName("WEEK")
        WEEK,

        @SerialName("MONTH")
        MONTH,
    }
}

@Serializable
data class ReportCountsResponseDto(
    val reviewCount: Long?,
    val uniqueCardCount: Long?,
    val studyDayCount: Long?,
    val ratings: ReportRatingsResponseDto?,
)

@Serializable
data class ReportRatingsResponseDto(
    val easy: Long?,
    val fair: Long?,
    val again: Long?,
)

@Serializable
data class ReportBucketResponseDto(
    val startDate: String?,
    val endDate: String?,
    val reviewCount: Long?,
    val ratings: ReportRatingsResponseDto?,
)

@Serializable
data class ReportDeckPageResponse(
    val items: List<ReportDeckOptionResponseDto>?,
    val nextCursor: String?,
)

@Serializable
data class ReportDeckOptionResponseDto(
    val deckId: Long?,
    val name: String?,
    val deleted: Boolean?,
)

@Serializable
data class ReportSessionPageResponse(
    val items: List<ReportSessionResponseDto>?,
    val nextCursor: String?,
)

@Serializable
data class ReportSessionResponseDto(
    val sessionId: Long?,
    val deckId: Long?,
    val deckName: String?,
    val deckDeleted: Boolean?,
    val studyDate: String?,
    val timezone: String?,
    val counts: ReportCountsResponseDto?,
)

@Serializable
data class ReportCardPageResponse(
    val items: List<ReportCardResponseDto>?,
    val nextCursor: String?,
)

@Serializable
data class ReportCardResponseDto(
    val cardId: Long?,
    val deleted: Boolean?,
    val front: String?,
    val back: String?,
    val reviewCount: Long?,
    val lastRating: RatingDto?,
    val lastReviewedAt: String?,
    val lastStudyDate: String?,
)

@Serializable
data class ReportCardReviewsResponse(
    val card: ReportCardLabelResponseDto?,
    val range: ReportRangeResponseDto?,
    val items: List<ReportReviewResponseDto>?,
    val nextCursor: String?,
)

@Serializable
data class ReportCardLabelResponseDto(
    val cardId: Long?,
    val deleted: Boolean?,
    val front: String?,
    val back: String?,
)

@Serializable
data class ReportReviewResponseDto(
    val reviewId: Long?,
    val sessionId: Long?,
    val rating: RatingDto?,
    val studyDate: String?,
    val reviewedAt: String?,
    val timezone: String?,
)

@Serializable
enum class RatingDto {
    @SerialName("EASY")
    EASY,

    @SerialName("FAIR")
    FAIR,

    @SerialName("AGAIN")
    AGAIN,
}
