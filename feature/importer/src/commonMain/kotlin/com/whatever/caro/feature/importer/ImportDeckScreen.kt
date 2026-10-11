package com.whatever.caro.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
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
import caromobile.core.designsystem.generated.resources.import_value_paste_deck_name
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.component.ImportCreatingDeckOverlay
import com.whatever.caro.feature.importer.component.ImportDialogs
import com.whatever.caro.feature.importer.component.ImportLoadingOverlay
import com.whatever.caro.feature.importer.component.ImportTopBar
import com.whatever.caro.feature.importer.logic.spreadsheetDateStyleFor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.step.PasteStep
import com.whatever.caro.feature.importer.step.SheetStep
import com.whatever.caro.feature.importer.step.SourceStep
import com.whatever.caro.feature.importer.step.SubdeckStep
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

@Composable
internal fun ImportDeckScreen(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val stateHolder = rememberSaveableStateHolder()
    val tableKey = rememberInputKey(state.table)
    val previewKey = rememberInputKey(state.preview)
    val pasteSourceName = stringResource(Res.string.import_value_paste_deck_name)
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
            )

            ImportLoadingOverlay(
                loading = state.overlayLoading,
                modifier = Modifier.weight(1f),
            ) {
                stateHolder.SaveableStateProvider(key = state.step.stateKey(tableKey, previewKey)) {
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
                            Unit
                        }

                        ImportStep.Mapping -> {
                            Unit
                        }

                        ImportStep.Preview -> {
                            Unit
                        }

                        ImportStep.DeckInfo, ImportStep.CreatingDeck -> {
                            Unit
                        }

                        ImportStep.Saving -> {
                            Unit
                        }
                    }
                }
            }
        }
    }

    ImportDialogs(dialog = state.dialog, onIntent = onIntent)
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
 * 앞 단계 입력이 바뀌면 키가 달라져 저장한 상태를 쓰지 않는다. 읽은 표는 미리보기 이전 단계에, 미리보기는 그 이후 단계에 영향을 준다.
 */
private fun ImportStep.stateKey(
    tableKey: Int,
    previewKey: Int,
): String {
    val step = if (this == ImportStep.CreatingDeck) ImportStep.DeckInfo else this
    val name = step::class.simpleName
    return when (step) {
        ImportStep.Preview, ImportStep.DeckInfo, ImportStep.Saving -> "$name-$tableKey-$previewKey"
        else -> "$name-$tableKey"
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
