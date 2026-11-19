package com.whatever.caro.core.remote.datasource.studyReport

import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportCardReviewsResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportDeckPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.ReportSessionPageResponse
import com.whatever.caro.core.remote.dto.studyReport.response.StudyReportSummaryResponse

interface StudyReportDataSource {
    suspend fun getSummary(
        deckId: Long?,
        startDate: String?,
        endDate: String?,
    ): StudyReportSummaryResponse

    suspend fun getDecks(
        cursor: String?,
        limit: Int,
    ): ReportDeckPageResponse

    suspend fun getSessions(
        deckId: Long?,
        startDate: String?,
        endDate: String?,
        cursor: String?,
        limit: Int,
    ): ReportSessionPageResponse

    suspend fun getCards(
        deckId: Long,
        startDate: String?,
        endDate: String?,
        lastRating: String?,
        cursor: String?,
        limit: Int,
    ): ReportCardPageResponse

    suspend fun getCardReviews(
        deckId: Long,
        cardId: Long,
        startDate: String?,
        endDate: String?,
        cursor: String?,
        limit: Int,
    ): ReportCardReviewsResponse
}
