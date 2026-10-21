package com.whatever.caro.feature.learning.di

import com.whatever.caro.feature.learning.LearningViewModel
import com.whatever.caro.feature.learning.report.LearningReportViewModel
import com.whatever.caro.feature.learning.report.ReportToday
import com.whatever.caro.feature.learning.report.SystemReportToday
import com.whatever.caro.feature.learning.report.card.CardReviewHistoryViewModel
import com.whatever.caro.feature.learning.report.history.LearningHistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val learningModule =
    module {
        viewModel<LearningViewModel> { params ->
            LearningViewModel(
                deckId = params[0],
                mode = params[1],
                repository = get(),
                deckRepository = get(),
                exceptionFilter = get(),
            )
        }

        single<ReportToday> { SystemReportToday }

        viewModel<LearningReportViewModel> { params ->
            LearningReportViewModel(
                deckId = params[0],
                repository = get(),
                reportToday = get(),
                exceptionFilter = get(),
            )
        }

        viewModel<LearningHistoryViewModel> { params ->
            LearningHistoryViewModel(
                deckId = params[0],
                deckName = params[1],
                startDate = params[2],
                endDate = params[3],
                repository = get(),
                exceptionFilter = get(),
            )
        }

        viewModel<CardReviewHistoryViewModel> { params ->
            CardReviewHistoryViewModel(
                deckId = params[0],
                cardId = params[1],
                startDate = params[2],
                endDate = params[3],
                repository = get(),
                exceptionFilter = get(),
            )
        }
    }
