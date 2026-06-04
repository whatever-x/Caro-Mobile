package com.whatever.caro.feature.profile.di

import com.whatever.caro.feature.profile.CreateProfileViewModel
import com.whatever.caro.feature.profile.NicknameValidator
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val profileModule =
    module {
        single<NicknameValidator>()

        viewModel<CreateProfileViewModel> {
            CreateProfileViewModel(
                authRepository = get(),
                profileRepository = get(),
                nicknameValidator = get(),
            )
        }
    }
