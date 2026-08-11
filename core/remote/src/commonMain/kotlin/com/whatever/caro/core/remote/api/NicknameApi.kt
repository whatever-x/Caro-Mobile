package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import de.jensklingenberg.ktorfit.http.GET

internal interface NicknameApi {
    @GET("nicknames/random")
    suspend fun requestRandomNickname(): NicknameResponse
}
