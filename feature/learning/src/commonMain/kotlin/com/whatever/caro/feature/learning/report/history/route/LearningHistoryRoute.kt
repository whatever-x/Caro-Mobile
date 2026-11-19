package com.whatever.caro.feature.learning.report.history.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.learning.report.history.LearningHistoryScreen
import com.whatever.caro.feature.learning.report.history.LearningHistoryViewModel
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistoryIntent
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistorySideEffect

@Composable
fun LearningHistoryRoute(
    viewModel: LearningHistoryViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.intent(LearningHistoryIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                LearningHistorySideEffect.NavigateBack -> navDispatcher.emit(NavCommand.Back)
            }
        }
    }

    LearningHistoryScreen(state = state, onIntent = viewModel::intent)
}
