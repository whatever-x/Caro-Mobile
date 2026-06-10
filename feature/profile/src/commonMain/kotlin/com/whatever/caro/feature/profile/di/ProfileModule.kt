package com.whatever.caro.feature.profile.di

import com.whatever.caro.feature.profile.CreateProfileViewModel
import com.whatever.caro.feature.profile.NicknameValidator
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val profileModule =
    module {
        single<NicknameValidator>()
        viewModel<CreateProfileViewModel>()
    }
