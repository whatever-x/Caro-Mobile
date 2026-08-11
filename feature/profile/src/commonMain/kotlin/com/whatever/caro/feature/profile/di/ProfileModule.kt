package com.whatever.caro.feature.profile.di

import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.create.CreateProfileViewModel
import com.whatever.caro.feature.profile.edit.EditProfileViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val profileModule =
    module {
        single<NicknameValidator>()
        viewModel<CreateProfileViewModel>()
        viewModel<EditProfileViewModel>()
    }
