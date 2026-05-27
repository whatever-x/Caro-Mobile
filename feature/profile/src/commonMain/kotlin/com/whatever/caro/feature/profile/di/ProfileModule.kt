package com.whatever.caro.feature.profile.di

import com.whatever.caro.feature.profile.CreateProfileViewModel
import com.whatever.caro.feature.profile.usecase.CheckNicknameUseCase
import com.whatever.caro.feature.profile.usecase.CreateProfileUseCase
import com.whatever.caro.feature.profile.usecase.GetRandomNicknameUseCase
import com.whatever.caro.feature.profile.usecase.ValidateNicknameUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule =
    module {
        single { ValidateNicknameUseCase() }
        single { CheckNicknameUseCase(profileRepository = get()) }
        single { CreateProfileUseCase(authRepository = get()) }
        single { GetRandomNicknameUseCase(profileRepository = get()) }

        viewModel<CreateProfileViewModel> {
            CreateProfileViewModel(
                validateNicknameUseCase = get(),
                checkNicknameUseCase = get(),
                createProfileUseCase = get(),
                getRandomNicknameUseCase = get(),
            )
        }
    }
