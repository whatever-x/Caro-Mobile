package com.whatever.caro.feature.importer.screenshot

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_import
import caromobile.core.designsystem.generated.resources.import_button_stop_import
import caromobile.core.designsystem.generated.resources.import_snackbar_deck_invalid_input
import caromobile.core.designsystem.generated.resources.import_state_retrying
import caromobile.core.designsystem.generated.resources.import_state_stopping
import caromobile.core.designsystem.generated.resources.import_subtitle_saving
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.designsystem.components.CaroSnackbarVisuals
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.support.PreviewSamples
import org.jetbrains.compose.resources.stringResource

/** [가져오기]가 있는 미리보기, 입력 거절 스낵바, 저장 화면(저장 중, 다시 연결 중, 멈추는 중)이다. 상태는 고정 값이다. */
internal object ImportSaveScreenshotScenario {
    const val PREVIEW_IMPORT_ID = "importer.preview.import"
    const val PREVIEW_INPUT_ERROR_ID = "importer.preview.deck_input_error"
    const val SAVING_ID = "importer.saving.progress"
    const val RETRYING_ID = "importer.saving.retrying"
    const val STOPPING_ID = "importer.saving.stopping"

    private const val DECK_NAME = "JLPT N5 단어"
    private const val DESCRIPTION = "JLPT N5 단어.apkg에서 가져왔어요"
    private const val TOTAL_CARDS = 1_512
    private const val SAVED_CARDS = 1_000

    private val preview get() =
        PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview).copy(deckDescription = DESCRIPTION)

    fun scenarios(): List<ScreenshotScenario> =
        listOf(
            previewImport(),
            ScreenshotScenario(
                PREVIEW_INPUT_ERROR_ID,
                render = { RenderWithInputError(preview) },
                expectedStrings = listOf(ScreenshotString(Res.string.import_snackbar_deck_invalid_input)),
                beforeCapture = { strings -> onNodeWithText(strings[Res.string.import_snackbar_deck_invalid_input]).assertIsDisplayed() },
            ),
            ScreenshotScenario(
                SAVING_ID,
                render = { RenderImportScreen(saving(SavingStatus.Saving)) },
                expectedStrings = listOf(ScreenshotString(Res.string.import_subtitle_saving)),
                beforeCapture = { strings -> onNodeWithText(strings[Res.string.import_subtitle_saving]).assertIsDisplayed() },
            ),
            ScreenshotScenario(
                RETRYING_ID,
                render = { RenderImportScreen(saving(SavingStatus.Retrying)) },
                expectedStrings = listOf(ScreenshotString(Res.string.import_state_retrying)),
                beforeCapture = { strings -> onNodeWithText(strings[Res.string.import_state_retrying]).assertIsDisplayed() },
            ),
            stopping(),
        )

    private fun previewImport(): ScreenshotScenario {
        val intents = mutableListOf<ImportDeckIntent>()
        return ScreenshotScenario(
            PREVIEW_IMPORT_ID,
            render = { RenderImportScreen(preview, onIntent = { intent -> intents += intent }) },
            expectedStrings = listOf(ScreenshotString(Res.string.import_button_import)),
            beforeCapture = { strings -> onNodeWithText(strings[Res.string.import_button_import]).assertIsDisplayed() },
            afterCapture = { strings ->
                onNodeWithText(strings[Res.string.import_button_import]).performClick()
                check(intents == listOf(ImportDeckIntent.ClickImport)) { "Import must emit exactly one action" }
            },
        )
    }

    /** 멈추는 중에는 [가져오기 멈추기]가 꺼져 있어야 한다. */
    private fun stopping(): ScreenshotScenario {
        val stoppingText = ScreenshotString(Res.string.import_state_stopping, listOf("1,000"))
        return ScreenshotScenario(
            STOPPING_ID,
            render = { RenderImportScreen(saving(SavingStatus.Stopping)) },
            expectedStrings = listOf(stoppingText, ScreenshotString(Res.string.import_button_stop_import)),
            beforeCapture = { strings ->
                onNodeWithText(strings[Res.string.import_state_stopping]).assertIsDisplayed()
                onNodeWithText(strings[Res.string.import_button_stop_import]).assertIsNotEnabled()
            },
        )
    }

    private fun saving(status: SavingStatus): ImportDeckState =
        ImportDeckState(
            step = ImportStep.Saving,
            deckName = DECK_NAME,
            deckDescription = DESCRIPTION,
            saving = SavingProgress(deckName = DECK_NAME, totalCards = TOTAL_CARDS, savedCards = SAVED_CARDS, status = status),
        )

    /** 입력 거절은 Route가 화면 안 스낵바 호스트로 띄우므로 같은 호스트에 같은 모양으로 띄운다. */
    @Composable
    private fun RenderWithInputError(state: ImportDeckState) {
        val hostState = remember { SnackbarHostState() }
        val message = stringResource(Res.string.import_snackbar_deck_invalid_input)
        LaunchedEffect(hostState, message) { hostState.showSnackbar(CaroSnackbarVisuals(message, style = CaroSnackbarStyle.Error)) }
        CaroTheme { ImportDeckScreen(state = state, onIntent = {}, snackbarHostState = hostState) }
    }
}
