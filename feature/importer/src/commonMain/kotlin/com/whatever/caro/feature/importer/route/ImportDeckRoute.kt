package com.whatever.caro.feature.importer.route

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_snackbar_completed
import caromobile.core.designsystem.generated.resources.import_snackbar_deck_invalid_input
import caromobile.core.designsystem.generated.resources.import_snackbar_empty_deck
import caromobile.core.designsystem.generated.resources.import_snackbar_partial
import caromobile.core.designsystem.generated.resources.import_snackbar_unconfirmed
import caromobile.core.designsystem.generated.resources.import_value_empty_field
import caromobile.core.designsystem.generated.resources.import_value_file_description
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.designsystem.components.showSnackbarMessage
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.ImportDeckViewModel
import com.whatever.caro.feature.importer.logic.FILE_NAME_PLACEHOLDER
import com.whatever.caro.feature.importer.logic.spreadsheetDateStyleFor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.platform.rememberImportFilePicker
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImportDeckRoute(
    viewModel: ImportDeckViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val dateStyle = spreadsheetDateStyleFor(Locale.current.language)
    val emptySideText = stringResource(Res.string.import_value_empty_field)
    val descriptionTemplate = stringResource(Res.string.import_value_file_description, FILE_NAME_PLACEHOLDER)
    val openFilePicker =
        rememberImportFilePicker { result ->
            viewModel.intent(
                ImportDeckIntent.FilePicked(
                    result = result,
                    dateStyle = dateStyle,
                    emptySideText = emptySideText,
                    descriptionTemplate = descriptionTemplate,
                ),
            )
        }
    val latestOpenFilePicker by rememberUpdatedState(openFilePicker)

    // 읽는 중, 대화상자, 저장 중 여부에 따른 판단은 ViewModel이 하므로 시스템 뒤로 가기도 늘 같은 intent로 보낸다.
    NavigationBackHandler(
        state = backState,
        onBackCompleted = { viewModel.intent(ImportDeckIntent.ClickBack) },
    )

    LaunchedEffect(viewModel, navDispatcher, snackbarController, snackbarHostState) {
        val effectScope = this
        viewModel.sideEffect.collect { sideEffect ->
            handleImportSideEffect(
                sideEffect = sideEffect,
                navDispatcher = navDispatcher,
                snackbarController = snackbarController,
                showScreenSnackbar = { message ->
                    showSnackbarMessage(effectScope, snackbarHostState, message.message, message.style, message.duration)
                },
                openFilePicker = { latestOpenFilePicker() },
                resolveText = { resource, args -> getString(resource, *args.toTypedArray()) },
            )
        }
    }

    ImportDeckScreen(
        state = state,
        onIntent = viewModel::intent,
        snackbarHostState = snackbarHostState,
    )
}

/** 가져오기 화면에 머무는 동안의 안내는 [showScreenSnackbar]로 화면 안에 띄우고, 화면을 떠난 뒤의 안내는 전역 [snackbarController]로 띄운다. */
internal suspend fun handleImportSideEffect(
    sideEffect: ImportDeckSideEffect,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
    showScreenSnackbar: (SnackBarMessage) -> Unit,
    openFilePicker: () -> Unit,
    resolveText: suspend (StringResource, List<Any>) -> String,
) {
    when (sideEffect) {
        ImportDeckSideEffect.OpenFilePicker -> {
            openFilePicker()
        }

        ImportDeckSideEffect.NavigateBack -> {
            navDispatcher.emit(command = NavCommand.Back)
        }

        ImportDeckSideEffect.NavigateHome -> {
            navDispatcher.emit(command = NavCommand.ResetTo(key = HomeEntry))
        }

        // 가져오기 화면을 덱 화면으로 바꿔 두어야 덱 화면에서 뒤로 가면 저장이 끝난 가져오기가 아니라 홈으로 간다.
        // 바꾸면 가져오기 화면이 구성에서 빠져 이 처리가 취소될 수 있으므로 결과 문구는 바꾸기 전에 읽는다.
        is ImportDeckSideEffect.NavigateToDeckDetail -> {
            val message = importResultSnackbar(sideEffect, resolveText)
            navDispatcher.emit(command = NavCommand.Replace(key = DeckDetailEntry(deck = sideEffect.deck)))
            snackbarController.show(message)
        }

        ImportDeckSideEffect.ShowDeckInputError -> {
            val message = resolveText(Res.string.import_snackbar_deck_invalid_input, emptyList())
            showScreenSnackbar(SnackBarMessage(message = message, style = CaroSnackbarStyle.Error))
        }
    }
}

/** 결과를 확인하지 못했으면 서버에 더 저장됐을 수 있어 장수나 빈 덱을 단정하는 문구를 고르지 않는다. */
private suspend fun importResultSnackbar(
    result: ImportDeckSideEffect.NavigateToDeckDetail,
    resolveText: suspend (StringResource, List<Any>) -> String,
): SnackBarMessage {
    val saved = result.saved.formatWithComma()
    return when {
        result.completed -> {
            SnackBarMessage(message = resolveText(Res.string.import_snackbar_completed, listOf(saved)), duration = SnackbarDuration.Short)
        }

        result.isSaveUnconfirmed -> {
            SnackBarMessage(message = resolveText(Res.string.import_snackbar_unconfirmed, emptyList()), duration = SnackbarDuration.Long)
        }

        result.saved == 0 -> {
            SnackBarMessage(message = resolveText(Res.string.import_snackbar_empty_deck, emptyList()), duration = SnackbarDuration.Long)
        }

        else -> {
            val message = resolveText(Res.string.import_snackbar_partial, listOf(result.total.formatWithComma(), saved))
            SnackBarMessage(message = message, duration = SnackbarDuration.Long)
        }
    }
}
