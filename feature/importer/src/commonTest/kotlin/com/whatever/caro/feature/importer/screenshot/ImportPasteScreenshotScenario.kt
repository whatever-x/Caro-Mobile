package com.whatever.caro.feature.importer.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_hint_paste_sides_later
import caromobile.core.designsystem.generated.resources.import_reason_paste_too_long
import caromobile.core.designsystem.generated.resources.import_subtitle_paste
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.core.screenshot.testing.ScreenshotVariant
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep

internal object ImportPasteScreenshotScenario {
    const val EMPTY_ID = "importer.paste.empty"
    const val TOO_LONG_ID = "importer.paste.too_long"
    const val TOO_LONG_SMALL_ID = "importer.paste.too_long.small.large-font"

    private const val PASTED = "사과\tapple\n바나나\tbanana\n포도\tgrape"

    private val tooLongReason =
        ScreenshotString(Res.string.import_reason_paste_too_long, listOf(ImportLimits.MAX_PASTE_CHARS.formatWithComma()))

    fun scenarios(): List<ScreenshotScenario> =
        listOf(
            ScreenshotScenario(
                EMPTY_ID,
                render = { RenderImportScreen(pasteState(text = "", isOverflowed = false)) },
                expectedStrings =
                    listOf(
                        ScreenshotString(Res.string.import_subtitle_paste),
                        ScreenshotString(Res.string.import_hint_paste_sides_later),
                        ScreenshotString(Res.string.import_button_next),
                    ),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.import_hint_paste_sides_later]).assertIsDisplayed()
                    onNodeWithText(strings[Res.string.import_button_next]).assertIsNotEnabled()
                },
            ),
            tooLongScenario(TOO_LONG_ID, ScreenshotVariant.DEFAULT),
            // 작은 화면과 큰 글자에서도 오류 문구가 입력칸 아래에 보여야 한다. 키보드가 열린 모습은 테스트에서 띄울 수 없어 찍지 않는다.
            tooLongScenario(TOO_LONG_SMALL_ID, ScreenshotVariant.SMALL_LARGE_FONT),
        )

    private fun tooLongScenario(
        id: String,
        variant: ScreenshotVariant,
    ) = ScreenshotScenario(
        id,
        variant = variant,
        render = { RenderImportScreen(pasteState(text = PASTED, isOverflowed = true)) },
        expectedStrings = listOf(tooLongReason, ScreenshotString(Res.string.import_hint_paste_sides_later)),
        beforeCapture = { strings ->
            onNodeWithText(strings[Res.string.import_reason_paste_too_long]).assertIsDisplayed()
            onNodeWithText(strings[Res.string.import_hint_paste_sides_later]).assertDoesNotExist()
        },
    )

    private fun pasteState(
        text: String,
        isOverflowed: Boolean,
    ) = ImportDeckState(step = ImportStep.Paste, isPaste = true, pasteText = text, isPasteOverflowed = isOverflowed)
}
