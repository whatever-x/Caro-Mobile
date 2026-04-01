package com.whatever.caro.core.data.repository.profile

interface ProfileRepository {
    suspend fun getRandomNickname(): String

    suspend fun validateNickname(nickname: String): NicknameValidation

    suspend fun createProfile(nickname: String): Long
}

data class NicknameValidation(
    val isValid: Boolean,
    val reason: String? = null,
)
