package com.whatever.caro.feature.card.di

import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.EditCardViewModel
import com.whatever.caro.feature.card.delete.DeleteCardsViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val cardModule =
    module {
        viewModel<CreateCardViewModel>()
        viewModel<EditCardViewModel>()
        viewModel<DeleteCardsViewModel>()
    }
