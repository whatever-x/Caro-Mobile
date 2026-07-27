package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.api.NicknameApi
import com.whatever.caro.core.remote.api.UserApi
import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import com.whatever.caro.core.remote.dto.user.request.UpdateNicknameRequest
import com.whatever.caro.core.remote.dto.user.response.MyNicknameResponse
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse
import com.whatever.caro.core.remote.dto.user.response.UpdateNicknameResponse

internal class RemoteProfileDataSourceImpl(
    private val nicknameApi: NicknameApi,
    private val userApi: UserApi,
) : ProfileDataSource {
    override suspend fun getRandomNickname(): NicknameResponse = nicknameApi.requestRandomNickname()

    override suspend fun getMyNickname(): MyNicknameResponse = userApi.requestMyNickname()

    override suspend fun checkNicknameAvailability(nickname: String): NicknameCheckResponse =
        userApi.requestCheckNicknameAvailability(nickname = nickname)

    override suspend fun changeNickname(request: UpdateNicknameRequest): UpdateNicknameResponse =
        userApi.requestUpdateNickname(request = request)
}
