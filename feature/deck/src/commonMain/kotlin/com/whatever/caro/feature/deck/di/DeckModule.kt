package com.whatever.caro.feature.deck.di

import com.whatever.caro.feature.deck.CreateDeckViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val deckModule =
    module {
        viewModel<CreateDeckViewModel>()
    }
