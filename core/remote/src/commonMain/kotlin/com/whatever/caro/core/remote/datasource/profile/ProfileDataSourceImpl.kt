package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.api.ProfileApi
import com.whatever.caro.core.remote.model.profile.response.NicknameAvailabilityResponse
import com.whatever.caro.core.remote.model.profile.response.RandomNicknameResponse

internal class ProfileDataSourceImpl(
    private val profileApi: ProfileApi,
) : ProfileDataSource {
    override suspend fun getRandomNickname(): RandomNicknameResponse = profileApi.getRandomNickname()

    override suspend fun checkNicknameAvailability(nickname: String): NicknameAvailabilityResponse =
        profileApi.checkNicknameAvailability(nickname = nickname)
}
