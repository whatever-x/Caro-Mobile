package com.whatever.caro.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.intl.Locale
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_title_mapping
import caromobile.core.designsystem.generated.resources.import_title_paste
import caromobile.core.designsystem.generated.resources.import_title_preview
import caromobile.core.designsystem.generated.resources.import_title_saving
import caromobile.core.designsystem.generated.resources.import_title_sheet
import caromobile.core.designsystem.generated.resources.import_title_source
import caromobile.core.designsystem.generated.resources.import_title_subdeck
import caromobile.core.designsystem.generated.resources.import_title_text_check
import caromobile.core.designsystem.generated.resources.import_value_empty_field
import caromobile.core.designsystem.generated.resources.import_value_paste_deck_name
import caromobile.core.designsystem.generated.resources.import_value_paste_description
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.components.ImportDialogs
import com.whatever.caro.feature.importer.components.ImportLoadingOverlay
import com.whatever.caro.feature.importer.components.ImportSnackbarFrame
import com.whatever.caro.feature.importer.components.ImportTopBar
import com.whatever.caro.feature.importer.logic.spreadsheetDateStyleFor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.platform.rememberReduceMotionEnabled
import com.whatever.caro.feature.importer.step.PasteStep
import com.whatever.caro.feature.importer.step.PreviewStep
import com.whatever.caro.feature.importer.step.SavingStep
import com.whatever.caro.feature.importer.step.SettingsStep
import com.whatever.caro.feature.importer.step.SourceStep
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

@Composable
internal fun ImportDeckScreen(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val backFocus = remember { FocusRequester() }
    val bottomFocus = remember { FocusRequester() }
    val stateHolder = rememberSaveableStateHolder()
    val previewKey = rememberIdentityKey(state.preview)

    ImportSnackbarFrame(hostState = snackbarHostState, modifier = Modifier.fillMaxSize()) {
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
                stateHolder.SaveableStateProvider(key = state.step.stateKey(previewKey)) {
                    ImportStepBody(state = state, onIntent = onIntent, bottomFocus = bottomFocus)
                }
            }
        }
    }

    ImportDialogs(dialog = state.dialog, onIntent = onIntent)
    DiscardLeftStepState(
        holder = stateHolder,
        key = state.step.stateKey(previewKey),
        keepsLeft = (state.step as? ImportStep.Settings)?.entry == SettingsEntry.FromPreview,
    )
    RestoreFocusAfterDialog(
        dialog = state.dialog,
        step = state.step,
        canFocusBottom = state.canFocusBottomButton(),
        backFocus = backFocus,
        bottomFocus = bottomFocus,
    )
    DismissScreenSnackbarWhenCovered(
        hostState = snackbarHostState,
        step = state.step,
        isCreatingDeck = state.loading == ImportLoading.CreatingDeck,
    )
}

/**
 * 안내 문구의 상한은 [ImportLimits] 정책 값이라, 테스트에서 상한을 낮춰 주입해도 안내 문구는 정책 값을 보인다.
 * [bottomFocus]는 대화상자를 닫은 뒤 포커스를 돌려받는 미리보기와 저장 화면의 하단 버튼에만 단다.
 */
@Composable
private fun ImportStepBody(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
    bottomFocus: FocusRequester,
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
            PreviewStep(state = state, onIntent = onIntent, importFocusRequester = bottomFocus)
        }

        is ImportStep.Settings -> {
            SettingsStep(state = state, onIntent = onIntent)
        }

        ImportStep.Saving -> {
            state.saving?.let { progress ->
                SavingStep(
                    progress = progress,
                    reduceMotion = rememberReduceMotionEnabled(),
                    onStop = { onIntent(ImportDeckIntent.ClickStop) },
                    stopFocusRequester = bottomFocus,
                )
            }
        }
    }
}

/** 붙여넣기의 덱 이름과 설명 기본값, 빈 면 문구, 날짜 표기는 앱 언어를 따라야 하므로 리소스에서 읽어 [다음] intent에 담는다. */
@Composable
private fun PasteStepBody(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val sourceName = stringResource(Res.string.import_value_paste_deck_name)
    val emptySideText = stringResource(Res.string.import_value_empty_field)
    val description = stringResource(Res.string.import_value_paste_description)
    val dateStyle = spreadsheetDateStyleFor(Locale.current.language)

    PasteStep(
        text = state.pasteText,
        maxPasteChars = ImportLimits.MAX_PASTE_CHARS,
        isOverflowed = state.isPasteOverflowed,
        isNextEnabled = state.isPasteNextEnabled,
        onTextChange = { text -> onIntent(ImportDeckIntent.UpdatePasteText(text)) },
        onNext = {
            onIntent(
                ImportDeckIntent.ClickPasteNext(
                    sourceName = sourceName,
                    dateStyle = dateStyle,
                    emptySideText = emptySideText,
                    description = description,
                ),
            )
        },
    )
}

private fun ImportStep.titleResource(): StringResource =
    when (this) {
        ImportStep.Source -> Res.string.import_title_source
        ImportStep.Paste -> Res.string.import_title_paste
        ImportStep.Preview -> Res.string.import_title_preview
        is ImportStep.Settings -> kind.titleResource()
        ImportStep.Saving -> Res.string.import_title_saving
    }

/**
 * 키는 Android Bundle에 넣을 수 있는 String이어야 한다. 다른 타입이면 상태를 저장할 때 예외가 나 앱이 종료된다.
 * 설정 화면은 data class라 클래스 이름만으로는 종류가 갈리지 않으므로 [SettingsKind]까지 넣는다.
 * 미리보기는 같은 미리보기로 돌아올 때만 스크롤을 이어 써야 하므로 미리보기 객체가 바뀌면 키도 바뀐다.
 */
private fun ImportStep.stateKey(previewKey: Int): String =
    when (this) {
        ImportStep.Source -> "Source"
        ImportStep.Paste -> "Paste"
        ImportStep.Preview -> "Preview-$previewKey"
        is ImportStep.Settings -> "Settings-${kind.name}"
        ImportStep.Saving -> "Saving"
    }

/**
 * 떠난 단계의 저장 상태를 지워 다시 열 때 처음부터 보이고, 저장 인스턴스 상태에 쌓이지 않게 한다.
 * 미리보기에서 연 설정 화면에서 바꾼 내용 없이 돌아오면 보던 미리보기 자리로 돌아가야 하므로, 그때 떠난 상태([keepsLeft])는 돌아온 뒤에 정리한다.
 * 남겨 둔 키를 구성 변경 뒤에 잊으면 돌아온 뒤 그 상태를 지우지 못해 저장 인스턴스 상태에 쌓인다.
 */
@Composable
private fun DiscardLeftStepState(
    holder: SaveableStateHolder,
    key: String,
    keepsLeft: Boolean,
) {
    var shownKey by remember { mutableStateOf(key) }
    var keptKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(key) {
        val leftKey = shownKey
        shownKey = key
        if (leftKey == key) return@LaunchedEffect
        if (keepsLeft) {
            keptKey = leftKey
            return@LaunchedEffect
        }
        holder.removeState(leftKey)
        keptKey?.takeIf { kept -> kept != key }?.let(holder::removeState)
        keptKey = null
    }
}

/** [value]가 다른 객체로 바뀔 때마다 새 값을 낸다. 큰 미리보기를 값으로 비교하지 않으려고 같은 객체인지만 본다. */
@Composable
private fun rememberIdentityKey(value: Any?): Int = rememberSaveable(Identity(value)) { Random.nextInt() }

private class Identity(
    private val value: Any?,
) {
    override fun equals(other: Any?): Boolean = other is Identity && other.value === value

    override fun hashCode(): Int = 0
}

private fun SettingsKind.titleResource(): StringResource =
    when (this) {
        SettingsKind.Subdeck -> Res.string.import_title_subdeck
        SettingsKind.Sheet -> Res.string.import_title_sheet
        SettingsKind.Delimiter -> Res.string.import_title_text_check
        SettingsKind.CardLayout -> Res.string.import_title_mapping
    }

/**
 * 대화상자를 닫고 그 단계에 머물면 대화상자를 연 버튼으로 포커스를 돌려준다.
 * 처음으로 돌아갈지 묻는 대화상자는 뒤로 가기 버튼, 멈춤 확인과 덱 만들기 실패는 하단 버튼이 연다.
 * 닫기 요청을 처리하는 시점에는 대화상자 창이 아직 붙어 있어, 창을 걷은 다음 구성에서 요청한다.
 * 닫으면서 단계가 바뀌면 대화상자를 연 화면이 없어졌으므로 요청하지 않는다.
 */
@Composable
private fun RestoreFocusAfterDialog(
    dialog: ImportDialog?,
    step: ImportStep,
    canFocusBottom: Boolean,
    backFocus: FocusRequester,
    bottomFocus: FocusRequester,
) {
    var opened by remember { mutableStateOf<Pair<ImportDialog, ImportStep>?>(null) }

    LaunchedEffect(dialog) {
        val previous = opened
        opened = dialog?.let { shown -> shown to step }
        if (dialog != null || previous == null || previous.second != step) return@LaunchedEffect
        when (previous.first) {
            ImportDialog.DiscardConfirm -> backFocus.requestFocus()
            ImportDialog.StopConfirm, ImportDialog.DeckCreateFailed -> if (canFocusBottom) bottomFocus.requestFocus()
            else -> Unit
        }
    }
}

/** 하단 버튼이 그려지지 않았거나 꺼졌거나 덮개에 가려졌으면 포커스를 돌려줄 수 없다. [멈추기]와 [다시 시도]로 닫으면 이렇게 된다. */
private fun ImportDeckState.canFocusBottomButton(): Boolean =
    when (step) {
        ImportStep.Preview -> table != null && preview != null && loading == null
        ImportStep.Saving -> saving != null && saving.status != SavingStatus.Stopping
        else -> false
    }

/**
 * 화면 안 스낵바는 미리보기의 입력 거절 안내라, 덱 만드는 중 덮개나 다른 단계 위에 남으면 가린 화면과 맞지 않는다.
 * 덮개가 걷힐 때는 닫지 않는다. 덱 만들기가 입력 거절로 끝나면 덮개를 걷는 것과 같은 때 거절 스낵바를 띄운다.
 */
@Composable
private fun DismissScreenSnackbarWhenCovered(
    hostState: SnackbarHostState,
    step: ImportStep,
    isCreatingDeck: Boolean,
) {
    var shownStep by remember { mutableStateOf(step) }

    LaunchedEffect(step, isCreatingDeck) {
        val isStepChanged = shownStep != step
        shownStep = step
        if (isStepChanged || isCreatingDeck) hostState.currentSnackbarData?.dismiss()
    }
}
