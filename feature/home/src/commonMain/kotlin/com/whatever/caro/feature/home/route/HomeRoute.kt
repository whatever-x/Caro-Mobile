package com.whatever.caro.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.navigator.contract.NavCommand.To
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.LearningEntry
import com.whatever.caro.core.navigator.entries.SettingEntry
import com.whatever.caro.feature.home.HomeScreen
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.intent(HomeIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.NavigateToDailyLearning -> {
                    navDispatcher.emit(
                        command =
                            To(
                                key =
                                    LearningEntry(
                                        deckId = sideEffect.deckId,
                                        mode = LearningMode.DAILY,
                                    ),
                            ),
                    )
                }

                is HomeSideEffect.NavigateToCreateDeck -> {
                    navDispatcher.emit(command = To(key = CreateDeckEntry))
                }

                is HomeSideEffect.NavigateToDeckDetail -> {
                    navDispatcher.emit(
                        command =
                            To(
                                key =
                                    DeckDetailEntry(
                                        deck = sideEffect.deck,
                                    ),
                            ),
                    )
                }

                HomeSideEffect.NavigateToSetting -> {
                    navDispatcher.emit(command = To(key = SettingEntry))
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}
