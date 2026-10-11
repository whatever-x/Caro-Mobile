package com.whatever.caro.feature.importer.route

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_snackbar_completed
import caromobile.core.designsystem.generated.resources.import_snackbar_deck_create_failed
import caromobile.core.designsystem.generated.resources.import_snackbar_deck_invalid_input
import caromobile.core.designsystem.generated.resources.import_snackbar_empty_deck
import caromobile.core.designsystem.generated.resources.import_snackbar_partial
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Composable
fun ImportDeckRoute(
    viewModel: ImportDeckViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    // 읽는 중, 대화상자, 저장 중 여부에 따른 판단은 ViewModel이 하므로 시스템 뒤로 가기도 늘 같은 intent로 보낸다.
    NavigationBackHandler(
        state = backState,
        onBackCompleted = { viewModel.intent(ImportDeckIntent.ClickBack) },
    )

    LaunchedEffect(viewModel, navDispatcher, snackbarController) {
        viewModel.sideEffect.collect { sideEffect ->
            handleImportSideEffect(
                sideEffect = sideEffect,
                navDispatcher = navDispatcher,
                snackbarController = snackbarController,
                resolveText = { resource, args -> getString(resource, *args.toTypedArray()) },
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
    snackbarController: SnackbarController,
    resolveText: suspend (StringResource, List<Any>) -> String,
) {
    when (sideEffect) {
        ImportDeckSideEffect.OpenFilePicker -> {
            Unit
        }

        ImportDeckSideEffect.NavigateBack -> {
            navDispatcher.emit(command = NavCommand.Back)
        }

        ImportDeckSideEffect.NavigateHome -> {
            navDispatcher.emit(command = NavCommand.ResetTo(key = HomeEntry))
        }

        is ImportDeckSideEffect.NavigateToDeckDetail -> {
            navDispatcher.emit(command = NavCommand.Replace(key = DeckDetailEntry(deck = sideEffect.deck)))
            snackbarController.show(importResultSnackbar(sideEffect, resolveText))
        }

        ImportDeckSideEffect.ShowDeckInputError -> {
            snackbarController.show(
                SnackBarMessage(
                    message = resolveText(Res.string.import_snackbar_deck_invalid_input, emptyList()),
                    style = CaroSnackbarStyle.Error,
                ),
            )
        }

        ImportDeckSideEffect.ShowDeckCreateError -> {
            snackbarController.show(
                SnackBarMessage(
                    message = resolveText(Res.string.import_snackbar_deck_create_failed, emptyList()),
                    style = CaroSnackbarStyle.Error,
                ),
            )
        }
    }
}

private suspend fun importResultSnackbar(
    result: ImportDeckSideEffect.NavigateToDeckDetail,
    resolveText: suspend (StringResource, List<Any>) -> String,
): SnackBarMessage =
    when {
        result.completed -> {
            SnackBarMessage(
                message = resolveText(Res.string.import_snackbar_completed, listOf(result.saved.formatWithComma())),
                duration = SnackbarDuration.Short,
            )
        }

        result.saved == 0 -> {
            SnackBarMessage(
                message = resolveText(Res.string.import_snackbar_empty_deck, emptyList()),
                duration = SnackbarDuration.Long,
            )
        }

        else -> {
            SnackBarMessage(
                message =
                    resolveText(
                        Res.string.import_snackbar_partial,
                        listOf(result.total.formatWithComma(), result.saved.formatWithComma()),
                    ),
                duration = SnackbarDuration.Long,
            )
        }
    }
