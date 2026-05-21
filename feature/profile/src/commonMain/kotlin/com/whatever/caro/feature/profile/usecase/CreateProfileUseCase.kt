package com.whatever.caro.feature.profile.usecase

import com.whatever.caro.core.data.repository.profile.ProfileRepository

class CreateProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(nickname: String): Long = profileRepository.createProfile(nickname)
}
