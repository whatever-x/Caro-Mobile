package com.whatever.caro.feature.deck.di

import com.whatever.caro.feature.deck.create.CreateDeckViewModel
import com.whatever.caro.feature.deck.edit.EditDeckViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val deckModule =
    module {
        viewModel<CreateDeckViewModel>()
        viewModel<EditDeckViewModel>()
    }
