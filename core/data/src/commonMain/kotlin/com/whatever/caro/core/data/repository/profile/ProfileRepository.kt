package com.whatever.caro.core.data.repository.profile

import com.whatever.caro.core.model.profile.MyInfo

interface ProfileRepository {
    suspend fun getRandomNickname(): String

    suspend fun getMyInfo(): MyInfo

    suspend fun getMyNickname(): String

    suspend fun isNicknameAvailable(nickname: String): Boolean

    suspend fun updateNickname(nickname: String)
}
