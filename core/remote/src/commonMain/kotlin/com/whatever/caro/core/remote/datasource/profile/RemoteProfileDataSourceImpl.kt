package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.api.NicknameApi
import com.whatever.caro.core.remote.api.UserApi
import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse

internal class RemoteProfileDataSourceImpl(
    private val nicknameApi: NicknameApi,
    private val userApi: UserApi,
) : ProfileDataSource {
    override suspend fun getRandomNickname(): NicknameResponse = nicknameApi.requestRandomNickname()

    override suspend fun checkNicknameAvailability(nickname: String): NicknameCheckResponse =
        userApi.requestCheckNicknameAvailability(nickname = nickname)
}
