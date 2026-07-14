package com.whatever.caro.feature.learning.di

import com.whatever.caro.feature.learning.LearningViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val learningModule =
    module {
        viewModel<LearningViewModel> { params ->
            LearningViewModel(
                deckId = params[0],
                mode = params[1],
                repository = get(),
                cardRepository = get(),
                exceptionFilter = get(),
            )
        }
    }
