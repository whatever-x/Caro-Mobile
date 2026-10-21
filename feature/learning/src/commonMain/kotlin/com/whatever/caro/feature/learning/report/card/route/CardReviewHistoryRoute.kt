package com.whatever.caro.feature.learning.report.card.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.learning.report.card.CardReviewHistoryScreen
import com.whatever.caro.feature.learning.report.card.CardReviewHistoryViewModel
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistoryIntent
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistorySideEffect

@Composable
fun CardReviewHistoryRoute(
    viewModel: CardReviewHistoryViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.intent(CardReviewHistoryIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                CardReviewHistorySideEffect.NavigateBack -> navDispatcher.emit(NavCommand.Back)
            }
        }
    }

    CardReviewHistoryScreen(state = state, onIntent = viewModel::intent)
}
