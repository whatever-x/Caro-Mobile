package com.whatever.caro.feature.card.di

import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.DeckCardsViewModel
import com.whatever.caro.feature.card.EditCardViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val cardModule =
    module {
        viewModel<CreateCardViewModel>()
        viewModel<DeckCardsViewModel>()
        viewModel<EditCardViewModel>()
    }
