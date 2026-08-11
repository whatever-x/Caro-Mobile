package com.whatever.caro.feature.login.di

import com.whatever.caro.feature.login.LoginViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val loginModule =
    module {
        viewModel<LoginViewModel>()
    }

expect val socialModule: Module
