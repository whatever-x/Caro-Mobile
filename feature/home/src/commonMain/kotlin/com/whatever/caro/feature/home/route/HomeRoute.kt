package com.whatever.caro.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_snackbar_deck_load_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.navigator.contract.NavCommand.To
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.LearningEntry
import com.whatever.caro.core.navigator.entries.SettingEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.home.HomeScreen
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deckLoadErrorMessage = stringResource(Res.string.home_snackbar_deck_load_error)

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

                HomeSideEffect.ShowDeckLoadError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = deckLoadErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}
