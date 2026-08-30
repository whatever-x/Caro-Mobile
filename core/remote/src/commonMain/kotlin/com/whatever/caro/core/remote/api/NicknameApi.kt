package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers

internal interface NicknameApi {
    @Headers(ApiVersionHeaders.V1_0)
    @GET("nicknames/random")
    suspend fun requestRandomNickname(): NicknameResponse
}
