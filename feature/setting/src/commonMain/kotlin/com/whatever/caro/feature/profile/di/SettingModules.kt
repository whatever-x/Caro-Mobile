package com.whatever.caro.feature.profile.di

import com.whatever.caro.feature.profile.SettingViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val settingModules =
    module {
        viewModel<SettingViewModel>()
    }
