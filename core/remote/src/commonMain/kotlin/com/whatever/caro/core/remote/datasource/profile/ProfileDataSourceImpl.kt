package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.api.ProfileApi
import com.whatever.caro.core.remote.model.profile.request.CreateProfileRequest
import com.whatever.caro.core.remote.model.profile.response.CreateProfileResponse
import com.whatever.caro.core.remote.model.profile.response.NicknameAvailabilityResponse
import com.whatever.caro.core.remote.model.profile.response.RandomNicknameResponse

internal class ProfileDataSourceImpl(
    private val profileApi: ProfileApi,
) : ProfileDataSource {
    override suspend fun getRandomNickname(): RandomNicknameResponse = profileApi.getRandomNickname()

    override suspend fun checkNicknameAvailability(nickname: String): NicknameAvailabilityResponse =
        profileApi.checkNicknameAvailability(nickname = nickname)

    override suspend fun createProfile(request: CreateProfileRequest): CreateProfileResponse {
        // TODO : 생성하기 API 명세 확정 시 ProfileApi로 추가
        return CreateProfileResponse(
            userId = 1L,
            nickname = request.nickname,
        )
    }
}
