package com.whatever.caro.core.data.repository.profile

import com.whatever.caro.core.data.mapper.toMyInfo
import com.whatever.caro.core.data.mapper.toMyNickname
import com.whatever.caro.core.model.profile.MyInfo
import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource
import com.whatever.caro.core.remote.dto.user.request.UpdateNicknameRequest

internal class ProfileRepositoryImpl(
    private val profileDataSource: ProfileDataSource,
) : ProfileRepository {
    override suspend fun getRandomNickname(): String {
        val response = profileDataSource.getRandomNickname()
        return response.nickname
    }

    override suspend fun getMyInfo(): MyInfo = profileDataSource.getMyInfo().toMyInfo()

    override suspend fun getMyNickname(): String = profileDataSource.getMyInfo().toMyNickname()

    override suspend fun isNicknameAvailable(nickname: String): Boolean {
        val response = profileDataSource.checkNicknameAvailability(nickname)
        return response.available
    }

    override suspend fun updateNickname(nickname: String) {
        val request = UpdateNicknameRequest(nickname = nickname)
        profileDataSource.changeNickname(request)
    }
}
