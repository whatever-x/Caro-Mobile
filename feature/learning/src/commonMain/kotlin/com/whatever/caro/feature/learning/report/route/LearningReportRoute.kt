package com.whatever.caro.feature.learning.report.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CardReviewHistoryEntry
import com.whatever.caro.core.navigator.entries.LearningHistoryEntry
import com.whatever.caro.feature.learning.report.LearningReportScreen
import com.whatever.caro.feature.learning.report.LearningReportViewModel
import com.whatever.caro.feature.learning.report.mvi.LearningReportIntent
import com.whatever.caro.feature.learning.report.mvi.LearningReportSideEffect

@Composable
fun LearningReportRoute(
    viewModel: LearningReportViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.intent(LearningReportIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                LearningReportSideEffect.NavigateBack -> {
                    navDispatcher.emit(NavCommand.Back)
                }

                is LearningReportSideEffect.NavigateToHistory -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            LearningHistoryEntry(
                                deckId = sideEffect.deckId,
                                deckName = sideEffect.deckName,
                                startDate = sideEffect.startDate,
                                endDate = sideEffect.endDate,
                            ),
                        ),
                    )
                }

                is LearningReportSideEffect.NavigateToCardReviews -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            CardReviewHistoryEntry(
                                deckId = sideEffect.deckId,
                                cardId = sideEffect.cardId,
                                startDate = sideEffect.startDate,
                                endDate = sideEffect.endDate,
                            ),
                        ),
                    )
                }
            }
        }
    }

    LearningReportScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}
