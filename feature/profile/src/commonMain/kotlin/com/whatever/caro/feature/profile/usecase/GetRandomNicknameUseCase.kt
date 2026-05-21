package com.whatever.caro.feature.profile.usecase

import com.whatever.caro.core.data.repository.profile.ProfileRepository

class GetRandomNicknameUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): String = profileRepository.getRandomNickname()
}
