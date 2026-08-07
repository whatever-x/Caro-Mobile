package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.streak.response.StreakResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST

internal interface StreakApi {
    @GET("streaks")
    suspend fun requestStreak(): StreakResponse

    @POST("streaks/sync")
    suspend fun requestSyncStreak()
}
