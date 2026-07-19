package com.whatever.caro.feature.deck.detail.di

import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val deckDetailModule =
    module {
        viewModel<DeckDetailViewModel> { params ->
            DeckDetailViewModel(
                deck = params[0],
                deckRepository = get(),
                exceptionFilter = get(),
            )
        }
    }
