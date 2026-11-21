package com.whatever.caro.feature.importer.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_value_empty_field
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.logic.spreadsheetDateStyleFor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.platform.rememberImportFilePicker
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImportDeckRoute(
    viewModel: ImportDeckViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val dateStyle = spreadsheetDateStyleFor(Locale.current.language)
    val emptySideText = stringResource(Res.string.import_value_empty_field)
    val openFilePicker =
        rememberImportFilePicker { result ->
            viewModel.intent(ImportDeckIntent.FilePicked(result = result, dateStyle = dateStyle, emptySideText = emptySideText))
        }
    val latestOpenFilePicker by rememberUpdatedState(openFilePicker)

    // 읽는 중, 대화상자 여부에 따른 판단은 ViewModel이 하므로 시스템 뒤로 가기도 늘 같은 intent로 보낸다.
    NavigationBackHandler(
        state = backState,
        onBackCompleted = { viewModel.intent(ImportDeckIntent.ClickBack) },
    )

    LaunchedEffect(viewModel, navDispatcher) {
        viewModel.sideEffect.collect { sideEffect ->
            handleImportSideEffect(
                sideEffect = sideEffect,
                navDispatcher = navDispatcher,
                openFilePicker = { latestOpenFilePicker() },
            )
        }
    }

    ImportDeckScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

internal suspend fun handleImportSideEffect(
    sideEffect: ImportDeckSideEffect,
    navDispatcher: NavigationDispatcher,
    openFilePicker: () -> Unit,
) {
    when (sideEffect) {
        ImportDeckSideEffect.OpenFilePicker -> openFilePicker()
        ImportDeckSideEffect.NavigateBack -> navDispatcher.emit(command = NavCommand.Back)
    }
}
