package com.whatever.caro.feature.setting.di

import com.whatever.caro.feature.setting.SettingViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val settingModule =
    module {
        viewModel<SettingViewModel>()
    }
