package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.user.request.UpdateNicknameRequest
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse
import com.whatever.caro.core.remote.dto.user.response.UpdateNicknameResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.Path

internal interface UserApi {
    @GET("v1/users/nicknames/{nickname}/availability")
    suspend fun requestCheckNicknameAvailability(
        @Path("nickname") nickname: String,
    ): NicknameCheckResponse

    @PATCH("v1/users/me/nickname")
    suspend fun requestUpdateNickname(
        @Body request: UpdateNicknameRequest,
    ): UpdateNicknameResponse
}
