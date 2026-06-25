package com.whatever.caro.core.data.repository.profile

interface ProfileRepository {
    suspend fun getRandomNickname(): String

    suspend fun isNicknameAvailable(nickname: String): Boolean

    suspend fun changeNickname(nickname: String)
}
