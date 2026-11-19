package com.whatever.caro.core.data.repository.report

import com.whatever.caro.core.data.mapper.toLastRatingQuery
import com.whatever.caro.core.data.mapper.toReportCardRecordPage
import com.whatever.caro.core.data.mapper.toReportCardReviews
import com.whatever.caro.core.data.mapper.toReportDeckOptionPage
import com.whatever.caro.core.data.mapper.toReportSessionPage
import com.whatever.caro.core.data.mapper.toStudyReportSummary
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportCardReviews
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.core.model.report.ReportPage
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.ReportSession
import com.whatever.caro.core.model.report.StudyReportSummary
import com.whatever.caro.core.remote.datasource.studyReport.StudyReportDataSource

private const val REPORT_PAGE_SIZE = 20

internal class StudyReportRepositoryImpl(
    private val dataSource: StudyReportDataSource,
) : StudyReportRepository {
    override suspend fun getSummary(
        deckId: Long?,
        period: ReportPeriod?,
    ): StudyReportSummary =
        dataSource
            .getSummary(
                deckId = deckId,
                startDate = period?.startDate?.toString(),
                endDate = period?.endDate?.toString(),
            ).toStudyReportSummary()

    override suspend fun getDeckOptions(cursor: String?): ReportPage<ReportDeckOption> =
        dataSource.getDecks(cursor = cursor, limit = REPORT_PAGE_SIZE).toReportDeckOptionPage()

    override suspend fun getSessions(
        deckId: Long?,
        period: ReportPeriod?,
        cursor: String?,
    ): ReportPage<ReportSession> =
        dataSource
            .getSessions(
                deckId = deckId,
                startDate = period?.startDate?.toString(),
                endDate = period?.endDate?.toString(),
                cursor = cursor,
                limit = REPORT_PAGE_SIZE,
            ).toReportSessionPage()

    override suspend fun getCards(
        deckId: Long,
        period: ReportPeriod?,
        lastRating: StudyRating?,
        cursor: String?,
    ): ReportPage<ReportCardRecord> =
        dataSource
            .getCards(
                deckId = deckId,
                startDate = period?.startDate?.toString(),
                endDate = period?.endDate?.toString(),
                lastRating = lastRating?.toLastRatingQuery(),
                cursor = cursor,
                limit = REPORT_PAGE_SIZE,
            ).toReportCardRecordPage()

    override suspend fun getCardReviews(
        deckId: Long,
        cardId: Long,
        period: ReportPeriod?,
        cursor: String?,
    ): ReportCardReviews =
        dataSource
            .getCardReviews(
                deckId = deckId,
                cardId = cardId,
                startDate = period?.startDate?.toString(),
                endDate = period?.endDate?.toString(),
                cursor = cursor,
                limit = REPORT_PAGE_SIZE,
            ).toReportCardReviews()
}
