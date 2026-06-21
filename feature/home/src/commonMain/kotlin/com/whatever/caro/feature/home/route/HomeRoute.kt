package com.whatever.caro.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand.To
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.feature.home.HomeScreen
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeSideEffect

// TODO: 덱 상세 화면 연동 전까지 사용하는 임시 테스트용 덱 ID
private const val TEST_DECK_ID = 1L

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.NavigateToProfile -> {
                    navDispatcher.emit(command = To(key = CreateProfileEntry))
                }

                is HomeSideEffect.NavigateToCreateDeck -> {
                    navDispatcher.emit(command = To(key = CreateDeckEntry))
                }

                is HomeSideEffect.NavigateToCreateCard -> {
                    navDispatcher.emit(command = To(key = CreateCardEntry(deckId = TEST_DECK_ID)))
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}
