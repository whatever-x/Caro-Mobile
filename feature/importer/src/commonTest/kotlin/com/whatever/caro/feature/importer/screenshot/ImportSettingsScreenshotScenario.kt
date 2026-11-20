package com.whatever.caro.feature.importer.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_banner_over_limit_first
import caromobile.core.designsystem.generated.resources.import_button_apply
import caromobile.core.designsystem.generated.resources.import_button_change
import caromobile.core.designsystem.generated.resources.import_button_change_delimiter
import caromobile.core.designsystem.generated.resources.import_button_preview
import caromobile.core.designsystem.generated.resources.import_content_description_image_occlusion
import caromobile.core.designsystem.generated.resources.import_label_cloze_banner
import caromobile.core.designsystem.generated.resources.import_label_first_card_preview
import caromobile.core.designsystem.generated.resources.import_label_import_toggle
import caromobile.core.designsystem.generated.resources.import_label_select_all
import caromobile.core.designsystem.generated.resources.import_label_split_result
import caromobile.core.designsystem.generated.resources.import_loading_resplit
import caromobile.core.designsystem.generated.resources.import_reason_sheet_empty
import caromobile.core.designsystem.generated.resources.import_reason_sheet_over_limit
import caromobile.core.designsystem.generated.resources.import_reason_subdeck_over_limit
import caromobile.core.designsystem.generated.resources.import_subtitle_mapping
import caromobile.core.designsystem.generated.resources.import_subtitle_sheet
import caromobile.core.designsystem.generated.resources.import_subtitle_subdeck
import caromobile.core.designsystem.generated.resources.import_summary_over
import caromobile.core.designsystem.generated.resources.import_summary_selected_label
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.core.screenshot.testing.ScreenshotVariant
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.support.SettingsSamples
import org.jetbrains.compose.resources.StringResource

/** 설정 변경 화면과 먼저 고칠 것 화면이다. 상태는 ViewModel과 같은 함수로 채운 고정 값이다. */
internal object ImportSettingsScreenshotScenario {
    const val SUBDECK_ID = "importer.settings.subdeck"
    const val SUBDECK_PARTIAL_ID = "importer.settings.subdeck.partial"
    const val SHEET_ID = "importer.settings.sheet"
    const val SHEET_EMPTY_ID = "importer.settings.sheet.empty"
    const val DELIMITER_ID = "importer.settings.delimiter"
    const val DELIMITER_RESPLIT_ID = "importer.settings.delimiter.resplit"
    const val MAPPING_ID = "importer.settings.mapping"
    const val MAPPING_IMAGE_OCCLUSION_ID = "importer.settings.mapping.image_occlusion"
    const val MAPPING_SINGLE_ID = "importer.settings.mapping.single"
    const val MAPPING_MANY_FIELDS_ID = "importer.settings.mapping.many_fields"
    const val GATE_SUBDECK_ID = "importer.gate.subdeck"
    const val GATE_SUBDECK_LARGE_FONT_ID = "importer.gate.subdeck.small.large-font"
    const val GATE_SHEET_ID = "importer.gate.sheet"
    const val PREVIEW_CHANGE_ID = "importer.preview.change"

    private val maxCards get() = ImportLimits.MAX_CARDS.formatWithComma()

    fun scenarios(): List<ScreenshotScenario> =
        listOf(
            displayed(
                SUBDECK_ID,
                SettingsSamples.subdeckState(),
                string(Res.string.import_subtitle_subdeck),
                string(Res.string.import_button_apply),
            ),
            displayed(
                SUBDECK_PARTIAL_ID,
                SettingsSamples.subdeckState(SettingsSamples.partialSubdeckTable, selected = setOf("words", "day01")),
                string(Res.string.import_label_select_all),
                string(Res.string.import_summary_selected_label),
            ),
            displayed(
                SHEET_ID,
                SettingsSamples.sheetState(),
                string(Res.string.import_subtitle_sheet),
                string(Res.string.import_button_apply),
            ),
            disabled(
                SHEET_EMPTY_ID,
                SettingsSamples.sheetState(sheetIndex = 2),
                string(Res.string.import_reason_sheet_empty),
                Res.string.import_button_apply,
            ),
            displayed(
                DELIMITER_ID,
                SettingsSamples.delimiterState(),
                string(Res.string.import_label_first_card_preview),
                string(Res.string.import_label_split_result),
            ),
            disabled(
                DELIMITER_RESPLIT_ID,
                SettingsSamples.delimiterState(pendingDelimiter = ','),
                string(Res.string.import_loading_resplit),
                Res.string.import_button_apply,
                hasLoadingAnimation = true,
            ),
            displayed(
                MAPPING_ID,
                SettingsSamples.mappingState(),
                string(Res.string.import_subtitle_mapping),
                string(Res.string.import_label_cloze_banner),
            ),
            imageOcclusion(),
            displayed(
                MAPPING_SINGLE_ID,
                SettingsSamples.mappingState(SettingsSamples.singleTypeTable),
                string(Res.string.import_subtitle_mapping),
                string(Res.string.import_label_first_card_preview),
            ),
            displayed(
                MAPPING_MANY_FIELDS_ID,
                SettingsSamples.mappingState(SettingsSamples.manyFieldsTable, SettingsSamples.manyFieldsMapping),
                string(Res.string.import_subtitle_mapping),
                string(Res.string.import_label_first_card_preview),
            ),
            disabled(
                GATE_SUBDECK_ID,
                SettingsSamples.subdeckState(SettingsSamples.overLimitSubdeckTable, entry = SettingsEntry.Gate),
                string(Res.string.import_reason_subdeck_over_limit),
                Res.string.import_button_preview,
                string(Res.string.import_banner_over_limit_first, maxCards),
                string(Res.string.import_summary_over, "12,400"),
            ),
            ScreenshotScenario(
                GATE_SUBDECK_LARGE_FONT_ID,
                variant = ScreenshotVariant.SMALL_LARGE_FONT,
                render = {
                    RenderImportScreen(
                        SettingsSamples.subdeckState(SettingsSamples.overLimitSubdeckTable, entry = SettingsEntry.Gate),
                    )
                },
                expectedStrings = listOf(string(Res.string.import_reason_subdeck_over_limit), string(Res.string.import_button_preview)),
                beforeCapture = { strings -> onNodeWithText(strings[Res.string.import_reason_subdeck_over_limit]).assertIsDisplayed() },
            ),
            disabled(
                GATE_SHEET_ID,
                SettingsSamples.sheetState(SettingsSamples.overLimitSheetTable, entry = SettingsEntry.Gate),
                string(Res.string.import_reason_sheet_over_limit, maxCards),
                Res.string.import_button_preview,
                string(Res.string.import_banner_over_limit_first, maxCards),
            ),
            previewChange(),
        )

    private fun string(
        resource: StringResource,
        vararg args: Any,
    ): ScreenshotString = ScreenshotString(resource, args.toList())

    private fun displayed(
        id: String,
        state: ImportDeckState,
        vararg expected: ScreenshotString,
    ) = ScreenshotScenario(
        id,
        render = { RenderImportScreen(state) },
        expectedStrings = expected.toList(),
        beforeCapture = { strings ->
            expected.forEach { string ->
                onNodeWithText(strings[string.resource], useUnmergedTree = true).assertIsDisplayed()
            }
        },
    )

    /** 사유가 있거나 처리 중이면 하단 버튼이 꺼져 있어야 한다. */
    private fun disabled(
        id: String,
        state: ImportDeckState,
        shown: ScreenshotString,
        button: StringResource,
        vararg expected: ScreenshotString,
        hasLoadingAnimation: Boolean = false,
    ) = ScreenshotScenario(
        id,
        render = { if (hasLoadingAnimation) RenderImportScreenWithoutLoadingAnimation(state) else RenderImportScreen(state) },
        expectedStrings = listOf(shown, string(button)) + expected,
        beforeCapture = { strings ->
            onNodeWithText(strings[shown.resource], useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText(strings[button]).assertIsNotEnabled()
        },
    )

    private fun imageOcclusion(): ScreenshotScenario {
        val description =
            string(Res.string.import_content_description_image_occlusion, "Image Occlusion", SettingsSamples.IMAGE_OCCLUSION_CARDS)
        return ScreenshotScenario(
            MAPPING_IMAGE_OCCLUSION_ID,
            render = { RenderImportScreen(SettingsSamples.mappingState(SettingsSamples.imageOcclusionMappingTable)) },
            expectedStrings = listOf(description, string(Res.string.import_label_import_toggle)),
            beforeCapture = { strings -> onNodeWithContentDescription(strings[description.resource]).assertIsDisplayed() },
        )
    }

    private fun previewChange(): ScreenshotScenario {
        val intents = mutableListOf<ImportDeckIntent>()
        val state = PreviewSamples.state(PreviewSamples.singleColumnTable, PreviewSamples.singleColumnPreview)
        return ScreenshotScenario(
            PREVIEW_CHANGE_ID,
            render = { RenderImportScreen(state, onIntent = { intent -> intents += intent }) },
            expectedStrings = listOf(string(Res.string.import_button_change), string(Res.string.import_button_change_delimiter)),
            beforeCapture = { strings -> onNodeWithText(strings[Res.string.import_button_change_delimiter]).assertIsDisplayed() },
            afterCapture = { strings ->
                onNodeWithText(strings[Res.string.import_button_change_delimiter]).performClick()
                check(
                    intents == listOf(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter)),
                ) { "Delimiter change must emit exactly one action" }
            },
        )
    }
}
