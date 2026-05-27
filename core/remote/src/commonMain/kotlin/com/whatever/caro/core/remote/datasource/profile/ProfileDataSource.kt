package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.model.profile.response.NicknameAvailabilityResponse
import com.whatever.caro.core.remote.model.profile.response.RandomNicknameResponse

interface ProfileDataSource {
    suspend fun getRandomNickname(): RandomNicknameResponse

    suspend fun checkNicknameAvailability(nickname: String): NicknameAvailabilityResponse
}
