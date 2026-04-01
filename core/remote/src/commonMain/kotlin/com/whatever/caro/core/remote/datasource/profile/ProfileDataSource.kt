package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.model.profile.request.CreateProfileRequest
import com.whatever.caro.core.remote.model.profile.response.CreateProfileResponse
import com.whatever.caro.core.remote.model.profile.response.RandomNicknameResponse
import com.whatever.caro.core.remote.model.profile.response.ValidateNicknameResponse

interface ProfileDataSource {
    suspend fun getRandomNickname(): RandomNicknameResponse

    suspend fun validateNickname(nickname: String): ValidateNicknameResponse

    suspend fun createProfile(request: CreateProfileRequest): CreateProfileResponse
}
