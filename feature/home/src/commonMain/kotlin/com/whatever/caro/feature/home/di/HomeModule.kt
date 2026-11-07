package com.whatever.caro.feature.home.di

import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.library.LibraryPreviewViewModel
import com.whatever.caro.feature.home.library.LibraryViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val homeModule =
    module {
        viewModel<HomeViewModel>()
        viewModel<LibraryViewModel>()
        viewModel<LibraryPreviewViewModel>()
    }
