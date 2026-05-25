package com.whatever.caro.core.data.repository.profile

import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource
import com.whatever.caro.core.remote.model.profile.request.CreateProfileRequest

internal class ProfileRepositoryImpl(
    private val profileDataSource: ProfileDataSource,
) : ProfileRepository {
    override suspend fun getRandomNickname(): String {
        val response = profileDataSource.getRandomNickname()
        return response.nickname
    }

    override suspend fun validateNickname(nickname: String): NicknameValidation {
        val response = profileDataSource.checkNicknameAvailability(nickname)
        return NicknameValidation(
            isValid = response.available,
            reason = if (response.available) null else "DUPLICATE",
        )
    }

    override suspend fun createProfile(nickname: String): Long {
        val response =
            profileDataSource.createProfile(
                CreateProfileRequest(nickname = nickname),
            )
        return response.userId
    }
}
