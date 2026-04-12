package com.whatever.caro.feature.login.di

import com.whatever.caro.feature.login.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loginModule =
    module {
        viewModel { LoginViewModel() }
    }

expect val socialModule: Module
