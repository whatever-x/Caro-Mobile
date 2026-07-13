package com.whatever.caro.feature.learning.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.learning.LearningScreen
import com.whatever.caro.feature.learning.LearningViewModel
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningSideEffect

@Composable
fun LearningRoute(
    viewModel: LearningViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        onBackCompleted = { viewModel.intent(LearningIntent.RequestStop) },
    )
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { if (it == LearningSideEffect.NavigateBack) navDispatcher.emit(NavCommand.Back) }
    }
    LearningScreen(state, viewModel::intent)
}
