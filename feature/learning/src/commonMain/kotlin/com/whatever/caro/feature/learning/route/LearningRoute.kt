package com.whatever.caro.feature.learning.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.whatever.caro.core.ads.InterstitialAdController
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.feature.learning.LearningScreen
import com.whatever.caro.feature.learning.LearningViewModel
import com.whatever.caro.feature.learning.mvi.LearningIntent
import com.whatever.caro.feature.learning.mvi.LearningSideEffect
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
fun LearningRoute(
    viewModel: LearningViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val adController = koinInject<InterstitialAdController>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        onBackCompleted = { viewModel.intent(LearningIntent.RequestStop) },
    )
    LaunchedEffect(adController) {
        try {
            adController.preload()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // 광고 준비가 실패해도 학습은 진행할 수 있다.
        }
    }
    LaunchedEffect(viewModel, adController) {
        viewModel.intent(LearningIntent.Load)
        viewModel.sideEffect.collect {
            when (it) {
                LearningSideEffect.PopBackStack -> {
                    navDispatcher.emit(NavCommand.Back)
                }

                LearningSideEffect.NavigateToHome -> {
                    navDispatcher.emit(NavCommand.ResetTo(key = HomeEntry))
                }

                is LearningSideEffect.ShowCompletionAd -> {
                    try {
                        adController.showIfEligible(it.evaluatedCardCount)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        // 광고 오류는 이미 끝난 학습의 결과 확인을 막지 않는다.
                    } finally {
                        viewModel.intent(LearningIntent.CompletionAdFinished)
                    }
                }
            }
        }
    }
    LearningScreen(state, viewModel::intent)
}
