package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.streak.response.StreakResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST

internal interface StreakApi {
    @Headers(ApiVersionHeaders.V1_0)
    @GET("streaks")
    suspend fun requestStreak(): StreakResponse

    @Headers(ApiVersionHeaders.V1_0)
    @POST("streaks/sync")
    suspend fun requestSyncStreak()
}
