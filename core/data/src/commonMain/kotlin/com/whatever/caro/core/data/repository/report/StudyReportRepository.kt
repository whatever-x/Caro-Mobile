package com.whatever.caro.core.data.repository.report

import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportCardReviews
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPage
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.ReportSession
import com.whatever.caro.core.model.report.StudyReportSummary

/**
 * 학습 리포트 조회. deckId가 null이면 전체, period가 null이면 전체 기간이다.
 * 목록의 cursor는 같은 조건의 다음 페이지에만 쓴다.
 */
interface StudyReportRepository {
    suspend fun getSummary(
        deckId: Long?,
        period: ReportPeriod?,
    ): StudyReportSummary

    suspend fun getDeckOptions(cursor: String?): ReportPage<ReportDeckOption>

    suspend fun getSessions(
        deckId: Long?,
        period: ReportPeriod?,
        cursor: String?,
    ): ReportPage<ReportSession>

    suspend fun getCards(
        deckId: Long,
        period: ReportPeriod?,
        lastRating: StudyRating?,
        cursor: String?,
    ): ReportPage<ReportCardRecord>

    suspend fun getCardReviews(
        deckId: Long,
        cardId: Long,
        period: ReportPeriod?,
        cursor: String?,
    ): ReportCardReviews
}
