package com.whatever.caro.feature.deck.detail.di

import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val deckDetailModule =
    module {
        viewModel<DeckDetailViewModel> { DeckDetailViewModel(get()) }
    }
