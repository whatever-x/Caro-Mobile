package com.whatever.caro.core.data.repository.profile

import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource

internal class ProfileRepositoryImpl(
    private val profileDataSource: ProfileDataSource,
) : ProfileRepository {
    override suspend fun getRandomNickname(): String {
        val response = profileDataSource.getRandomNickname()
        return response.nickname
    }

    override suspend fun isNicknameAvailable(nickname: String): Boolean {
        val response = profileDataSource.checkNicknameAvailability(nickname)
        return response.available
    }

    override suspend fun changeNickname(nickname: String) {
        val request = Request
    }
}
