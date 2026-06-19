package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.studySession.request.EvaluatedCardRequest
import com.whatever.caro.core.remote.dto.studySession.request.StartDailyStudyRequest
import com.whatever.caro.core.remote.dto.studySession.response.DailyStudyResponse
import com.whatever.caro.core.remote.dto.studySession.response.DailyStudySummaryResponse
import com.whatever.caro.core.remote.dto.studySession.response.EvaluationResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

internal interface StudySessionApi {
    @GET("v1/study-sessions/daily/summary")
    suspend fun requestTodayDailyStudySummary(
        @Query("deckId") deckId: Long,
    ): DailyStudySummaryResponse

    @POST("v1/study-sessions/daily")
    suspend fun requestStartDailyStudy(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StartDailyStudyRequest,
    ): DailyStudyResponse

    @POST("v1/study-sessions/{sessionId}/evaluations")
    suspend fun requestEvaluate(
        @Path("sessionId") sessionId: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: List<EvaluatedCardRequest>,
    ): EvaluationResponse
}
