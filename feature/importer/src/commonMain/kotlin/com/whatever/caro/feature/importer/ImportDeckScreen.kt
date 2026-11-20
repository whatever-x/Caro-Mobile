package com.whatever.caro.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.intl.Locale
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_title_paste
import caromobile.core.designsystem.generated.resources.import_title_preview
import caromobile.core.designsystem.generated.resources.import_title_source
import caromobile.core.designsystem.generated.resources.import_value_empty_field
import caromobile.core.designsystem.generated.resources.import_value_paste_deck_name
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.components.ImportDialogs
import com.whatever.caro.feature.importer.components.ImportLoadingOverlay
import com.whatever.caro.feature.importer.components.ImportTopBar
import com.whatever.caro.feature.importer.logic.spreadsheetDateStyleFor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.step.PasteStep
import com.whatever.caro.feature.importer.step.PreviewStep
import com.whatever.caro.feature.importer.step.SourceStep
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ImportDeckScreen(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val backFocus = remember { FocusRequester() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
    ) {
        ImportTopBar(
            title = stringResource(state.step.titleResource()),
            onBack = { onIntent(ImportDeckIntent.ClickBack) },
            backFocusRequester = backFocus,
        )
        ImportLoadingOverlay(loading = state.loading, modifier = Modifier.weight(1f)) {
            ImportStepBody(state = state, onIntent = onIntent)
        }
    }

    ImportDialogs(dialog = state.dialog, onIntent = onIntent)
    RestoreBackFocusAfterDiscard(dialog = state.dialog, step = state.step, backFocus = backFocus)
}

/** 안내 문구의 상한은 [ImportLimits] 정책 값이라, 테스트에서 상한을 낮춰 주입해도 안내 문구는 정책 값을 보인다. */
@Composable
private fun ImportStepBody(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    when (state.step) {
        ImportStep.Source -> {
            SourceStep(
                maxCards = ImportLimits.MAX_CARDS,
                onClickFile = { onIntent(ImportDeckIntent.ClickPickFile) },
                onClickPaste = { onIntent(ImportDeckIntent.ClickPaste) },
            )
        }

        ImportStep.Paste -> {
            PasteStepBody(state = state, onIntent = onIntent)
        }

        ImportStep.Preview -> {
            PreviewStep(state = state, onIntent = onIntent)
        }
    }
}

/** 붙여넣기의 덱 이름 기본값과 빈 면 문구, 날짜 표기는 앱 언어를 따라야 하므로 리소스에서 읽어 [다음] intent에 담는다. */
@Composable
private fun PasteStepBody(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val sourceName = stringResource(Res.string.import_value_paste_deck_name)
    val emptySideText = stringResource(Res.string.import_value_empty_field)
    val dateStyle = spreadsheetDateStyleFor(Locale.current.language)

    PasteStep(
        text = state.pasteText,
        maxPasteChars = ImportLimits.MAX_PASTE_CHARS,
        isOverflowed = state.isPasteOverflowed,
        isNextEnabled = state.isPasteNextEnabled,
        onTextChange = { text -> onIntent(ImportDeckIntent.UpdatePasteText(text)) },
        onNext = {
            onIntent(
                ImportDeckIntent.ClickPasteNext(sourceName = sourceName, dateStyle = dateStyle, emptySideText = emptySideText),
            )
        },
    )
}

private fun ImportStep.titleResource(): StringResource =
    when (this) {
        ImportStep.Source -> Res.string.import_title_source
        ImportStep.Paste -> Res.string.import_title_paste
        ImportStep.Preview -> Res.string.import_title_preview
    }

/**
 * 처음으로 돌아갈지 묻는 대화상자를 닫고 그 단계에 머물면 대화상자를 연 뒤로 가기 버튼으로 포커스를 돌려준다.
 * 닫기 요청을 처리하는 시점에는 대화상자 창이 아직 붙어 있어, 창을 걷은 다음 구성에서 요청한다.
 * [돌아가기]로 단계가 바뀌면 대화상자를 연 화면이 없어졌으므로 요청하지 않는다.
 */
@Composable
private fun RestoreBackFocusAfterDiscard(
    dialog: ImportDialog?,
    step: ImportStep,
    backFocus: FocusRequester,
) {
    var discardOpenedOn by remember { mutableStateOf<ImportStep?>(null) }

    LaunchedEffect(dialog) {
        val openedOn = discardOpenedOn
        discardOpenedOn = step.takeIf { dialog == ImportDialog.DiscardConfirm }
        if (dialog == null && openedOn == step) backFocus.requestFocus()
    }
}
