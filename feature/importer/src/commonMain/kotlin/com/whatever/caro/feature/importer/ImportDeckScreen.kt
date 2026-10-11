package com.whatever.caro.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import caromobile.core.designsystem.generated.resources.import_title_deck_info
import caromobile.core.designsystem.generated.resources.import_title_mapping
import caromobile.core.designsystem.generated.resources.import_title_paste
import caromobile.core.designsystem.generated.resources.import_title_preview
import caromobile.core.designsystem.generated.resources.import_title_saving
import caromobile.core.designsystem.generated.resources.import_title_sheet
import caromobile.core.designsystem.generated.resources.import_title_source
import caromobile.core.designsystem.generated.resources.import_title_subdeck
import caromobile.core.designsystem.generated.resources.import_title_text_check
import caromobile.core.designsystem.generated.resources.import_value_empty_field
import caromobile.core.designsystem.generated.resources.import_value_file_description
import caromobile.core.designsystem.generated.resources.import_value_paste_deck_name
import caromobile.core.designsystem.generated.resources.import_value_paste_description
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.component.ImportCreatingDeckOverlay
import com.whatever.caro.feature.importer.component.ImportDialogs
import com.whatever.caro.feature.importer.component.ImportLoadingOverlay
import com.whatever.caro.feature.importer.component.ImportTopBar
import com.whatever.caro.feature.importer.logic.resolveDelimiter
import com.whatever.caro.feature.importer.logic.spreadsheetDateStyleFor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.platform.rememberReduceMotionEnabled
import com.whatever.caro.feature.importer.step.DeckInfoStep
import com.whatever.caro.feature.importer.step.MappingStep
import com.whatever.caro.feature.importer.step.PasteStep
import com.whatever.caro.feature.importer.step.PreviewStep
import com.whatever.caro.feature.importer.step.SavingStep
import com.whatever.caro.feature.importer.step.SheetStep
import com.whatever.caro.feature.importer.step.SourceStep
import com.whatever.caro.feature.importer.step.SubdeckStep
import com.whatever.caro.feature.importer.step.TextCheckStep
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

@Composable
internal fun ImportDeckScreen(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val stateHolder = rememberSaveableStateHolder()
    val stateKeys = StepStateKeys(state.sourceVersion, state.mappingInputVersion, rememberInputKey(state.preview))
    val backFocus = remember { FocusRequester() }
    val ctaFocus = remember { FocusRequester() }
    val pasteSourceName = stringResource(Res.string.import_value_paste_deck_name)
    val emptySideText = stringResource(Res.string.import_value_empty_field)
    // 파일 이름은 ViewModel이 120자로 줄여 {file} 자리에 넣는다.
    val fileDescriptionTemplate = stringResource(Res.string.import_value_file_description, "{file}")
    val pasteDescription = stringResource(Res.string.import_value_paste_description)
    val dateStyle = spreadsheetDateStyleFor(Locale.current.language)

    ImportCreatingDeckOverlay(isCreating = state.step == ImportStep.CreatingDeck) {
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

            ImportLoadingOverlay(
                loading = state.overlayLoading,
                modifier = Modifier.weight(1f),
            ) {
                stateHolder.SaveableStateProvider(key = stateKeys.of(state.step)) {
                    when (state.step) {
                        ImportStep.Source -> {
                            SourceStep(
                                maxCards = ImportLimits.MAX_CARDS,
                                onClickFile = { onIntent(ImportDeckIntent.ClickPickFile) },
                                onClickPaste = { onIntent(ImportDeckIntent.ClickPaste) },
                            )
                        }

                        ImportStep.Paste -> {
                            PasteStep(
                                text = state.pasteText,
                                maxPasteChars = ImportLimits.MAX_PASTE_CHARS,
                                onTextChange = { onIntent(ImportDeckIntent.UpdatePasteText(it)) },
                                onNext = { onIntent(ImportDeckIntent.ClickPasteNext(sourceName = pasteSourceName, dateStyle = dateStyle)) },
                            )
                        }

                        ImportStep.SubdeckSelect -> {
                            state.table?.let { table ->
                                SubdeckStep(
                                    table = table,
                                    selectedIds = state.selection.subdeckIds,
                                    maxCards = ImportLimits.MAX_CARDS,
                                    onToggleSubdeck = { id -> onIntent(ImportDeckIntent.ToggleSubdeck(id)) },
                                    onToggleAll = { onIntent(ImportDeckIntent.ToggleAllSubdecks) },
                                    onNext = { onIntent(ImportDeckIntent.ClickSelectionNext) },
                                )
                            }
                        }

                        ImportStep.SheetSelect -> {
                            state.table?.let { table ->
                                // 다시 읽는 동안 selection은 바꾸기 전 값이라 고른 시트가 바로 보이려면 pendingSelection이 먼저다.
                                val selection = state.pendingSelection ?: state.selection
                                SheetStep(
                                    table = table,
                                    sheetIndex = selection.sheetIndex,
                                    hasHeaderRow = selection.hasHeaderRow ?: table.layout?.hasHeaderRow ?: false,
                                    maxCards = ImportLimits.MAX_CARDS,
                                    onSelectSheet = { index -> onIntent(ImportDeckIntent.SelectSheet(index)) },
                                    onChangeHeaderRow = { checked -> onIntent(ImportDeckIntent.ChangeHeaderRow(checked)) },
                                    onNext = { onIntent(ImportDeckIntent.ClickSelectionNext) },
                                )
                            }
                        }

                        ImportStep.TextCheck -> {
                            state.table?.let { table ->
                                // 다시 나누는 동안 selection은 바꾸기 전 값이라 방금 고른 칩이 바로 보이려면 pendingSelection이 먼저다.
                                val selection = state.pendingSelection ?: state.selection
                                TextCheckStep(
                                    table = table,
                                    delimiter = resolveDelimiter(selection.delimiter, table.layout),
                                    hasHeaderRow = selection.hasHeaderRow ?: table.layout?.hasHeaderRow ?: false,
                                    isReparsing = state.loading == ImportLoading.TextReparsing,
                                    onSelectDelimiter = { delimiter -> onIntent(ImportDeckIntent.SelectDelimiter(delimiter)) },
                                    onChangeHeaderRow = { checked -> onIntent(ImportDeckIntent.ChangeHeaderRow(checked)) },
                                    onNext = { onIntent(ImportDeckIntent.ClickSelectionNext) },
                                )
                            }
                        }

                        ImportStep.Mapping -> {
                            state.table?.let { table ->
                                MappingStep(
                                    table = table,
                                    mappings = state.mappings,
                                    onToggleNoteType = { id -> onIntent(ImportDeckIntent.ToggleNoteType(id)) },
                                    onToggleField = {
                                        id,
                                        target,
                                        fieldName,
                                        ->
                                        onIntent(ImportDeckIntent.ToggleField(id, target, fieldName))
                                    },
                                    onNext = { onIntent(ImportDeckIntent.ClickMappingNext(emptySideText = emptySideText)) },
                                )
                            }
                        }

                        ImportStep.Preview -> {
                            val table = state.table
                            val preview = state.preview
                            if (table != null && preview != null) {
                                PreviewStep(
                                    preview = preview,
                                    format = table.format,
                                    isPaste = state.isPaste,
                                    filter = state.previewFilter,
                                    detailCard = (state.dialog as? ImportDialog.CardDetail)?.card,
                                    maxCards = ImportLimits.MAX_CARDS,
                                    onSelectFilter = { filter -> onIntent(ImportDeckIntent.SelectPreviewFilter(filter)) },
                                    onClickCard = { card -> onIntent(ImportDeckIntent.ClickCard(card)) },
                                    onDismissDetail = { onIntent(ImportDeckIntent.DismissDialog) },
                                    onNext = { onIntent(ImportDeckIntent.ClickPreviewNext(fileDescriptionTemplate, pasteDescription)) },
                                )
                            }
                        }

                        ImportStep.DeckInfo, ImportStep.CreatingDeck -> {
                            state.preview?.let { preview ->
                                DeckInfoStep(
                                    name = state.deckName,
                                    description = state.deckDescription,
                                    cardCount = preview.stats.cardCount,
                                    isConfirmEnabled = state.isDeckInfoConfirmEnabled,
                                    onNameChange = { name -> onIntent(ImportDeckIntent.UpdateDeckName(name)) },
                                    onDescriptionChange = { description -> onIntent(ImportDeckIntent.UpdateDeckDescription(description)) },
                                    onConfirm = { onIntent(ImportDeckIntent.ClickCreateDeck) },
                                    confirmFocusRequester = ctaFocus,
                                )
                            }
                        }

                        ImportStep.Saving -> {
                            state.saving?.let { saving ->
                                SavingStep(
                                    progress = saving,
                                    reduceMotion = rememberReduceMotionEnabled(),
                                    onStop = { onIntent(ImportDeckIntent.ClickStop) },
                                    stopFocusRequester = ctaFocus,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    ImportDialogs(dialog = state.dialog, onIntent = onIntent)
    DiscardReplacedStepStates(stateHolder, stateKeys)
    RestoreFocusAfterDialog(
        dialog = state.dialog,
        step = state.step,
        isCtaShown =
            (state.step == ImportStep.Saving && state.saving != null) || (state.step == ImportStep.DeckInfo && state.preview != null),
        backFocus = backFocus,
        ctaFocus = ctaFocus,
    )
}

private fun ImportStep.titleResource(): StringResource =
    when (this) {
        ImportStep.Source -> Res.string.import_title_source
        ImportStep.Paste -> Res.string.import_title_paste
        ImportStep.SubdeckSelect -> Res.string.import_title_subdeck
        ImportStep.SheetSelect -> Res.string.import_title_sheet
        ImportStep.TextCheck -> Res.string.import_title_text_check
        ImportStep.Mapping -> Res.string.import_title_mapping
        ImportStep.Preview -> Res.string.import_title_preview
        ImportStep.DeckInfo, ImportStep.CreatingDeck -> Res.string.import_title_deck_info
        ImportStep.Saving -> Res.string.import_title_saving
    }

/**
 * 키는 Android Bundle에 넣을 수 있는 String이어야 한다. 다른 타입이면 상태 저장 때 죽는다.
 * 덱 생성 중 화면은 덱 정보 위에 덮이므로 같은 키를 쓴다.
 * 단계가 쓰는 입력이 바뀌면 키가 달라져 저장한 상태를 쓰지 않는다. 같은 단계 안에서 표 객체만 바뀌는 다시 읽기는 키를 바꾸지 않아야 스크롤과 포커스가 남는다.
 */
private data class StepStateKeys(
    val sourceVersion: Int,
    val mappingInputVersion: Int,
    val previewKey: Int,
) {
    fun of(step: ImportStep): String {
        val keyed = if (step == ImportStep.CreatingDeck) ImportStep.DeckInfo else step
        val name = keyed::class.simpleName
        return when (keyed) {
            ImportStep.SubdeckSelect, ImportStep.SheetSelect, ImportStep.TextCheck -> "$name-$sourceVersion"
            ImportStep.Mapping -> "$name-$mappingInputVersion"
            ImportStep.Preview, ImportStep.DeckInfo, ImportStep.Saving -> "$name-$previewKey"
            else -> name.orEmpty()
        }
    }

    val inputKeyed: Set<String> get() = INPUT_KEYED_STEPS.map(::of).toSet()
}

private val INPUT_KEYED_STEPS =
    listOf(
        ImportStep.SubdeckSelect,
        ImportStep.SheetSelect,
        ImportStep.TextCheck,
        ImportStep.Mapping,
        ImportStep.Preview,
        ImportStep.DeckInfo,
        ImportStep.Saving,
    )

/** 입력이 바뀌어 다시는 쓰이지 않는 키의 저장 상태를 지워 저장 인스턴스 상태가 다시 읽을 때마다 쌓이지 않게 한다. */
@Composable
private fun DiscardReplacedStepStates(
    holder: SaveableStateHolder,
    keys: StepStateKeys,
) {
    var previous by remember { mutableStateOf(keys) }

    LaunchedEffect(keys) {
        (previous.inputKeyed - keys.inputKeyed).forEach(holder::removeState)
        previous = keys
    }
}

/**
 * 팝업을 닫으면 팝업을 연 요소로 포커스를 돌려준다. 닫기 요청을 처리하는 시점에는 팝업 창이 아직 붙어 있어, 창을 걷은 다음 구성에서 요청한다.
 * 닫으면서 단계가 바뀌면 돌려줄 요소가 사라졌거나 다른 요소이므로 요청하지 않는다.
 */
@Composable
private fun RestoreFocusAfterDialog(
    dialog: ImportDialog?,
    step: ImportStep,
    isCtaShown: Boolean,
    backFocus: FocusRequester,
    ctaFocus: FocusRequester,
) {
    var opened by remember { mutableStateOf<Pair<ImportDialog, ImportStep>?>(null) }

    LaunchedEffect(dialog) {
        val previous = opened
        opened = dialog?.let { it to step }
        if (dialog != null || previous == null || previous.second != step) return@LaunchedEffect
        when (previous.first) {
            ImportDialog.DiscardConfirm -> backFocus.requestFocus()
            ImportDialog.StopConfirm, ImportDialog.DeckCreateFailed -> if (isCtaShown) ctaFocus.requestFocus()
            else -> Unit
        }
    }
}

/** [input]이 다른 객체로 바뀔 때마다 새 값을 낸다. 큰 읽기 결과를 값으로 비교하지 않으려고 같은 객체인지만 본다. */
@Composable
private fun rememberInputKey(input: Any?): Int = rememberSaveable(InputIdentity(input)) { Random.nextInt() }

private class InputIdentity(
    private val input: Any?,
) {
    override fun equals(other: Any?): Boolean = other is InputIdentity && other.input === input

    override fun hashCode(): Int = 0
}
