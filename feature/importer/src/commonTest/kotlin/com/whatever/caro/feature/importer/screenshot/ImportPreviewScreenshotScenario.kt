package com.whatever.caro.feature.importer.screenshot

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_banner_delimiter_check
import caromobile.core.designsystem.generated.resources.import_chip_filter_all
import caromobile.core.designsystem.generated.resources.import_chip_filter_truncated
import caromobile.core.designsystem.generated.resources.import_content_description_dismiss_banner
import caromobile.core.designsystem.generated.resources.import_hint_filter_truncated
import caromobile.core.designsystem.generated.resources.import_label_setting_card_layout
import caromobile.core.designsystem.generated.resources.import_label_setting_deck_name
import caromobile.core.designsystem.generated.resources.import_tag_excluded_other_sheets
import caromobile.core.designsystem.generated.resources.import_tag_header_row_off
import caromobile.core.designsystem.generated.resources.import_tag_sides_auto
import caromobile.core.designsystem.generated.resources.import_title_settings
import caromobile.core.designsystem.generated.resources.import_warning_xlsx_formula
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.core.screenshot.testing.ScreenshotStrings
import com.whatever.caro.core.screenshot.testing.ScreenshotVariant
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.support.PreviewSamples
import org.jetbrains.compose.resources.StringResource

internal object ImportPreviewScreenshotScenario {
    const val ANKI_ID = "importer.preview.anki"
    const val XLSX_ID = "importer.preview.xlsx"
    const val TEXT_ID = "importer.preview.text"
    const val FILTERED_ID = "importer.preview.filtered"
    const val WARNINGS_ID = "importer.preview.warnings"
    const val BANNER_ID = "importer.preview.banner"
    const val LARGE_FONT_ID = "importer.preview.anki.small.large-font"

    private val ankiState get() = PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview)

    fun scenarios(): List<ScreenshotScenario> =
        listOf(
            displayed(
                ANKI_ID,
                ankiState,
                ScreenshotString(Res.string.import_title_settings),
                ScreenshotString(Res.string.import_tag_sides_auto),
            ),
            displayed(
                XLSX_ID,
                PreviewSamples.state(PreviewSamples.xlsxTable, PreviewSamples.xlsxPreview),
                ScreenshotString(Res.string.import_title_settings),
                ScreenshotString(Res.string.import_tag_excluded_other_sheets, listOf(2)),
            ),
            displayed(
                TEXT_ID,
                PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.textPreview),
                ScreenshotString(Res.string.import_label_setting_card_layout),
                ScreenshotString(Res.string.import_tag_header_row_off),
            ),
            filtered(),
            displayed(
                WARNINGS_ID,
                PreviewSamples.state(
                    PreviewSamples.xlsxTable,
                    PreviewSamples.preview(PreviewSamples.xlsxCards, excluded = 3, warnings = PreviewSamples.xlsxWarnings),
                ),
                ScreenshotString(Res.string.import_title_settings),
                ScreenshotString(Res.string.import_warning_xlsx_formula, listOf(3)),
                substring = Res.string.import_warning_xlsx_formula,
            ),
            banner(),
            ScreenshotScenario(
                LARGE_FONT_ID,
                variant = ScreenshotVariant.SMALL_LARGE_FONT,
                render = { RenderImportScreen(ankiState) },
                expectedStrings =
                    listOf(
                        ScreenshotString(Res.string.import_title_settings),
                        ScreenshotString(Res.string.import_label_setting_deck_name),
                    ),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.import_label_setting_deck_name], useUnmergedTree = true).assertIsDisplayed()
                },
            ),
        )

    /** [substring]은 `\n`이 든 문구라 일부만 맞춰 찾는다. */
    private fun displayed(
        id: String,
        state: ImportDeckState,
        vararg expected: ScreenshotString,
        substring: StringResource? = null,
    ) = ScreenshotScenario(
        id,
        render = { RenderImportScreen(state) },
        expectedStrings = expected.toList(),
        beforeCapture = { strings ->
            expected.forEach { string -> assertDisplayed(strings, string.resource, string.resource == substring) }
        },
    )

    private fun filtered(): ScreenshotScenario {
        val intents = mutableListOf<ImportDeckIntent>()
        val state = PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview, filter = ImportPreviewFilter.Truncated)
        return ScreenshotScenario(
            FILTERED_ID,
            render = { RenderImportScreen(state, onIntent = { intent -> intents += intent }) },
            expectedStrings =
                listOf(
                    ScreenshotString(Res.string.import_hint_filter_truncated, listOf(CardInputLimits.FIELD_MAX.formatWithComma())),
                    ScreenshotString(Res.string.import_chip_filter_truncated),
                    ScreenshotString(Res.string.import_chip_filter_all),
                ),
            beforeCapture = { strings ->
                onNodeWithText(strings[Res.string.import_hint_filter_truncated]).assertIsDisplayed()
                onNodeWithText(strings[Res.string.import_chip_filter_truncated]).assertIsSelected()
            },
            afterCapture = { strings ->
                onNodeWithText(strings[Res.string.import_chip_filter_all]).performClick()
                check(
                    intents == listOf(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.All)),
                ) { "Filter chip must emit exactly one action" }
            },
        )
    }

    private fun banner(): ScreenshotScenario {
        val intents = mutableListOf<ImportDeckIntent>()
        val state = PreviewSamples.state(PreviewSamples.singleColumnTable, PreviewSamples.singleColumnPreview)
        return ScreenshotScenario(
            BANNER_ID,
            render = { RenderImportScreen(state, onIntent = { intent -> intents += intent }) },
            expectedStrings =
                listOf(
                    ScreenshotString(Res.string.import_banner_delimiter_check, listOf(PreviewSamples.singleColumnCards.size)),
                    ScreenshotString(Res.string.import_content_description_dismiss_banner),
                ),
            beforeCapture = { strings ->
                onNodeWithText(strings[Res.string.import_banner_delimiter_check]).assertIsDisplayed()
            },
            afterCapture = { strings ->
                onNodeWithContentDescription(strings[Res.string.import_content_description_dismiss_banner]).performClick()
                check(intents == listOf(ImportDeckIntent.DismissDelimiterBanner)) { "Banner close must emit exactly one action" }
            },
        )
    }

    private fun SemanticsNodeInteractionsProvider.assertDisplayed(
        strings: ScreenshotStrings,
        resource: StringResource,
        substring: Boolean,
    ) {
        val text = strings[resource]
        val target = if (substring) text.substringBefore('\n') else text
        onNodeWithText(target, substring = substring, useUnmergedTree = true).assertIsDisplayed()
    }
}
