package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.user.request.UpdateNicknameRequest
import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse
import com.whatever.caro.core.remote.dto.user.response.UpdateNicknameResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.Path

internal interface UserApi {
    @Headers(ApiVersionHeaders.V1_0)
    @GET("users/me/info")
    suspend fun requestMyInfo(): MyInfoResponse

    @Headers(ApiVersionHeaders.V1_0)
    @GET("users/nicknames/{nickname}/availability")
    suspend fun requestCheckNicknameAvailability(
        @Path("nickname") nickname: String,
    ): NicknameCheckResponse

    @Headers(ApiVersionHeaders.V1_0)
    @PATCH("users/me/nickname")
    suspend fun requestUpdateNickname(
        @Body request: UpdateNicknameRequest,
    ): UpdateNicknameResponse
}
