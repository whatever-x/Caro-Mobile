package com.whatever.caro.feature.learning.report.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
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
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    // 주차 목록이나 막대로 바꾼 기간이 있으면 시스템 뒤로 가기도 상단 버튼처럼 리포트 안에서 한 단계씩 돌아간다
    NavigationBackHandler(
        state = backState,
        isBackEnabled = state.handlesBack,
        onBackCompleted = { viewModel.intent(LearningReportIntent.ClickBack) },
    )

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
