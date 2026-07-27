package com.whatever.caro.feature.card.di

import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.EditCardViewModel
import com.whatever.caro.feature.card.delete.DeleteCardsViewModel
import com.whatever.caro.feature.card.detail.CardDetailViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel
import org.koin.core.module.dsl.viewModel as parameterizedViewModel

val cardModule =
    module {
        viewModel<CreateCardViewModel>()
        viewModel<EditCardViewModel>()
        viewModel<DeleteCardsViewModel>()
        parameterizedViewModel<CardDetailViewModel> { params ->
            CardDetailViewModel(
                deckId = params[0],
                initialCardId = params[1],
                deckRepository = get(),
                cardRepository = get(),
                exceptionFilter = get(),
            )
        }
    }
