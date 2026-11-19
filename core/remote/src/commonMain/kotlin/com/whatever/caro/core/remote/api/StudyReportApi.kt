package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardReviewsResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportDeckPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportSessionPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.StudyReportSummaryResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

// null인 Query는 요청에 넣지 않는다. 날짜는 yyyy-MM-dd이고, 둘 다 생략하면 전체 기간이다.
internal interface StudyReportApi {
    @Headers(ApiVersionHeaders.V1_0)
    @GET("study-reports/summary")
    suspend fun requestSummary(
        @Query("deckId") deckId: Long?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
    ): StudyReportSummaryResponse

    @Headers(ApiVersionHeaders.V1_0)
    @GET("study-reports/decks")
    suspend fun requestDecks(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ReportDeckPageResponse

    @Headers(ApiVersionHeaders.V1_0)
    @GET("study-reports/sessions")
    suspend fun requestSessions(
        @Query("deckId") deckId: Long?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ReportSessionPageResponse

    @Headers(ApiVersionHeaders.V1_0)
    @GET("study-reports/cards")
    suspend fun requestCards(
        @Query("deckId") deckId: Long,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("lastRating") lastRating: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ReportCardPageResponse

    @Headers(ApiVersionHeaders.V1_0)
    @GET("study-reports/cards/{cardId}/reviews")
    suspend fun requestCardReviews(
        @Path("cardId") cardId: Long,
        @Query("deckId") deckId: Long,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ReportCardReviewsResponse
}
