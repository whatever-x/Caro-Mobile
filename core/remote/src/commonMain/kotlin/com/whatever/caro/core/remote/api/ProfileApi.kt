package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.model.profile.response.NicknameAvailabilityResponse
import com.whatever.caro.core.remote.model.profile.response.RandomNicknameResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

internal interface ProfileApi {
    @GET("v1/nicknames/random")
    suspend fun getRandomNickname(): RandomNicknameResponse

    @GET("v1/users/nicknames/{nickname}/availability")
    suspend fun checkNicknameAvailability(
        @Path("nickname") nickname: String,
    ): NicknameAvailabilityResponse
}
