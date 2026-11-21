package com.whatever.caro.feature.importer.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_loading_file
import caromobile.core.designsystem.generated.resources.import_loading_paste
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.PreviewSamples

/** 로딩 카드는 200ms 뒤에 보인다. 캡처 실행기가 시계를 1초 진행한 뒤 멈추므로 카드가 보이는 상태에서 찍힌다. */
internal object ImportLoadingScreenshotScenario {
    const val FILE_ID = "importer.loading.file"
    const val PASTE_ID = "importer.loading.paste"

    fun scenarios(): List<ScreenshotScenario> =
        listOf(
            ScreenshotScenario(
                FILE_ID,
                render = {
                    RenderImportScreenWithoutLoadingAnimation(
                        ImportDeckState(loading = ImportLoading.ReadingFile(PreviewSamples.ANKI_FILE)),
                    )
                },
                expectedStrings = listOf(ScreenshotString(Res.string.import_loading_file)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.import_loading_file], useUnmergedTree = true).assertIsDisplayed()
                },
            ),
            ScreenshotScenario(
                PASTE_ID,
                render = {
                    RenderImportScreenWithoutLoadingAnimation(
                        ImportDeckState(
                            step = ImportStep.Paste,
                            isPaste = true,
                            pasteText = "사과\tapple\n바나나\tbanana",
                            loading = ImportLoading.ReadingPaste,
                        ),
                    )
                },
                expectedStrings = listOf(ScreenshotString(Res.string.import_loading_paste)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.import_loading_paste], useUnmergedTree = true).assertIsDisplayed()
                },
            ),
        )
}
