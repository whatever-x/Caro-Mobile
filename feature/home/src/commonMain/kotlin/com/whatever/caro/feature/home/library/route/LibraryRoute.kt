package com.whatever.caro.feature.home.library.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LibraryPreviewEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.feature.home.library.LibraryScreen
import com.whatever.caro.feature.home.library.LibraryViewModel
import com.whatever.caro.feature.home.library.mvi.LibraryIntent
import com.whatever.caro.feature.home.library.mvi.LibrarySideEffect

@Composable
fun LibraryRoute(
    viewModel: LibraryViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.intent(LibraryIntent.Initialize) }
    LaunchedEffect(viewModel, navDispatcher) {
        viewModel.sideEffect.collect { navDispatcher.emit(it.toNavCommand()) }
    }
    LibraryScreen(state, viewModel::intent)
}

internal fun LibrarySideEffect.toNavCommand(): NavCommand =
    when (this) {
        LibrarySideEffect.Back -> NavCommand.Back
        LibrarySideEffect.Login -> NavCommand.ResetTo(LoginEntry)
        LibrarySideEffect.Home -> NavCommand.ResetTo(HomeEntry)
        is LibrarySideEffect.Preview -> NavCommand.To(LibraryPreviewEntry(id))
        is LibrarySideEffect.PersonalDeck -> NavCommand.To(DeckDetailEntry(deck))
    }
