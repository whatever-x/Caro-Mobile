package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.import_dialog_button_confirm
import caromobile.core.designsystem.generated.resources.import_dialog_button_discard
import caromobile.core.designsystem.generated.resources.import_dialog_discard_title
import caromobile.core.designsystem.generated.resources.import_dialog_empty_result_title
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_cards_title
import com.whatever.caro.core.designsystem.components.CARO_DIALOG_CONTENT_TAG
import com.whatever.caro.core.designsystem.components.LocalCaroDialogContentOnly
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.core.screenshot.testing.ScreenshotTarget
import com.whatever.caro.feature.importer.components.ImportDialogs
import com.whatever.caro.feature.importer.logic.NoCardsKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import org.jetbrains.compose.resources.StringResource

/** iOS는 같은 대화상자 내용을 창과 스크림 없이 그려 내용만 찍는다. */
internal object ImportDialogScreenshotScenario {
    const val DISCARD_ID = "importer.dialog.discard"
    const val TOO_MANY_CARDS_ID = "importer.dialog.too_many_cards"
    const val IMAGE_OCCLUSION_ONLY_ID = "importer.dialog.no_cards.image_occlusion"

    fun scenarios(): List<ScreenshotScenario> =
        listOf(
            dialogScenario(
                DISCARD_ID,
                ImportDialog.DiscardConfirm,
                title = Res.string.import_dialog_discard_title,
                buttons = listOf(Res.string.import_dialog_button_discard, Res.string.import_dialog_button_cancel),
                expectedIntents = listOf(ImportDeckIntent.ConfirmDiscard, ImportDeckIntent.DismissDialog),
            ),
            dialogScenario(
                TOO_MANY_CARDS_ID,
                ImportDialog.TooManyCards(isPaste = true),
                title = Res.string.import_dialog_too_many_cards_title,
                buttons = listOf(Res.string.import_dialog_button_confirm),
                expectedIntents = listOf(ImportDeckIntent.DismissDialog),
            ),
            dialogScenario(
                IMAGE_OCCLUSION_ONLY_ID,
                ImportDialog.NoCards(NoCardsKind.ImageOcclusionOnly),
                title = Res.string.import_dialog_empty_result_title,
                buttons = listOf(Res.string.import_dialog_button_confirm),
                expectedIntents = listOf(ImportDeckIntent.DismissDialog),
            ),
        )

    private fun dialogScenario(
        id: String,
        dialog: ImportDialog,
        title: StringResource,
        buttons: List<StringResource>,
        expectedIntents: List<ImportDeckIntent>,
    ): ScreenshotScenario {
        val intents = mutableListOf<ImportDeckIntent>()
        val record: (ImportDeckIntent) -> Unit = { intent -> intents += intent }
        return ScreenshotScenario(
            id,
            render = { RenderDialog(dialog, record) },
            target = ScreenshotTarget.DIALOG,
            iosDialogContentTag = CARO_DIALOG_CONTENT_TAG,
            iosDialogContent = {
                CompositionLocalProvider(LocalCaroDialogContentOnly provides true) { RenderDialog(dialog, record) }
            },
            expectedStrings = (listOf(title) + buttons).map { resource -> ScreenshotString(resource) },
            beforeCapture = { strings -> onNodeWithText(strings[title]).assertIsDisplayed() },
            afterCapture = { strings ->
                buttons.forEach { button -> onNodeWithText(strings[button]).performClick() }
                check(intents == expectedIntents) { "$id buttons must each emit their action once" }
            },
        )
    }

    @Composable
    private fun RenderDialog(
        dialog: ImportDialog,
        onIntent: (ImportDeckIntent) -> Unit,
    ) {
        CaroTheme { ImportDialogs(dialog = dialog, onIntent = onIntent) }
    }
}
