package com.whatever.caro.feature.home.di

import com.whatever.caro.feature.home.HomeViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val homeModule =
    module {
        viewModel<HomeViewModel>()
    }
