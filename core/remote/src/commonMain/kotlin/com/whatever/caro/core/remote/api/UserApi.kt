package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

internal interface UserApi {
    @GET("v1/users/nicknames/{nickname}/availability")
    suspend fun requestCheckNicknameAvailability(
        @Path("nickname") nickname: String,
    ): NicknameCheckResponse
}
