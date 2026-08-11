package com.whatever.caro.core.remote.datasource.profile

import com.whatever.caro.core.remote.dto.nickname.response.NicknameResponse
import com.whatever.caro.core.remote.dto.user.request.UpdateNicknameRequest
import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse
import com.whatever.caro.core.remote.dto.user.response.NicknameCheckResponse
import com.whatever.caro.core.remote.dto.user.response.UpdateNicknameResponse

interface ProfileDataSource {
    suspend fun getRandomNickname(): NicknameResponse

    suspend fun getMyInfo(): MyInfoResponse

    suspend fun checkNicknameAvailability(nickname: String): NicknameCheckResponse

    suspend fun changeNickname(request: UpdateNicknameRequest): UpdateNicknameResponse
}
