package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse

interface ProfileDataSource {
    suspend fun getRandomNickname(): NicknameResponse

    suspend fun checkNicknameAvailability(nickname: String): NicknameCheckResponse
}
