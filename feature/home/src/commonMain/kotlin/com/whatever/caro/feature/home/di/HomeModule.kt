package com.whatever.caro.feature.home.di

import com.whatever.caro.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule =
    module {
        viewModel<HomeViewModel> { HomeViewModel(get()) }
    }
