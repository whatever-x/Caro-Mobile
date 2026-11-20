package com.whatever.caro.feature.importer.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_label_source_paste
import caromobile.core.designsystem.generated.resources.import_subtitle_source
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState

internal object ImportSourceScreenshotScenario {
    const val ID = "importer.source.default"

    fun scenarios(): List<ScreenshotScenario> {
        val intents = mutableListOf<ImportDeckIntent>()
        return listOf(
            ScreenshotScenario(
                ID,
                render = { RenderImportScreen(ImportDeckState(), onIntent = { intent -> intents += intent }) },
                expectedStrings =
                    listOf(
                        ScreenshotString(Res.string.import_subtitle_source),
                        ScreenshotString(Res.string.import_label_source_paste),
                    ),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.import_subtitle_source]).assertIsDisplayed()
                },
                afterCapture = { strings ->
                    onNodeWithText(strings[Res.string.import_label_source_paste]).performClick()
                    check(intents == listOf(ImportDeckIntent.ClickPaste)) { "Paste source must emit exactly one action" }
                },
            ),
        )
    }
}
