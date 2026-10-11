package com.whatever.caro.feature.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
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
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.ImportCreatingDeckOverlay
import com.whatever.caro.feature.importer.component.ImportDialogs
import com.whatever.caro.feature.importer.component.ImportLoadingOverlay
import com.whatever.caro.feature.importer.component.ImportTopBar
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
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
                        ImportStep.Source -> Unit
                        ImportStep.Paste -> Unit
                        ImportStep.SubdeckSelect -> Unit
                        ImportStep.SheetSelect -> Unit
                        ImportStep.TextCheck -> Unit
                        ImportStep.Mapping -> Unit
                        ImportStep.Preview -> Unit
                        ImportStep.DeckInfo, ImportStep.CreatingDeck -> Unit
                        ImportStep.Saving -> Unit
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
