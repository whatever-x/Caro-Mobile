package com.whatever.caro.core.remote.datasource.studyReport

import com.whatever.caro.core.remote.api.StudyReportApi

internal class RemoteStudyReportDataSourceImpl(
    private val api: StudyReportApi,
) : StudyReportDataSource {
    override suspend fun getSummary(
        deckId: Long?,
        startDate: String?,
        endDate: String?,
    ) = api.requestSummary(deckId, startDate, endDate)

    override suspend fun getDecks(
        cursor: String?,
        limit: Int,
    ) = api.requestDecks(cursor, limit)

    override suspend fun getSessions(
        deckId: Long?,
        startDate: String?,
        endDate: String?,
        cursor: String?,
        limit: Int,
    ) = api.requestSessions(deckId, startDate, endDate, cursor, limit)

    override suspend fun getCards(
        deckId: Long,
        startDate: String?,
        endDate: String?,
        lastRating: String?,
        cursor: String?,
        limit: Int,
    ) = api.requestCards(deckId, startDate, endDate, lastRating, cursor, limit)

    override suspend fun getCardReviews(
        deckId: Long,
        cardId: Long,
        startDate: String?,
        endDate: String?,
        cursor: String?,
        limit: Int,
    ) = api.requestCardReviews(cardId, deckId, startDate, endDate, cursor, limit)
}
