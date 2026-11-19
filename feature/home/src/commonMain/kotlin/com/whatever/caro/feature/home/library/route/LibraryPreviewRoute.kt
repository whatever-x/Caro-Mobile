package com.whatever.caro.feature.home.library.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.home.library.LibraryPreviewScreen
import com.whatever.caro.feature.home.library.LibraryPreviewViewModel
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent

@Composable
fun LibraryPreviewRoute(
    viewModel: LibraryPreviewViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.intent(LibraryPreviewIntent.Initialize) }
    LaunchedEffect(viewModel, navDispatcher) {
        viewModel.sideEffect.collect { navDispatcher.emit(it.toNavCommand()) }
    }
    LibraryPreviewScreen(state, viewModel::intent)
}
