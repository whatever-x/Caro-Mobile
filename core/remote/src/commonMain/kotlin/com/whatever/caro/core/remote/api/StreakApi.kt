package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.streak.response.StreakResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST

internal interface StreakApi {
    @GET("v1/streaks")
    suspend fun requestStreak(): StreakResponse

    @POST("v1/streaks/sync")
    suspend fun requestSyncStreak()
}
