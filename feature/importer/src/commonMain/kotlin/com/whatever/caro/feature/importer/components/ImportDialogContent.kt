package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.Composable
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.import_dialog_button_confirm
import caromobile.core.designsystem.generated.resources.import_dialog_button_discard
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_apkg
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_body
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_title
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_xlsx
import caromobile.core.designsystem.generated.resources.import_dialog_discard_body
import caromobile.core.designsystem.generated.resources.import_dialog_discard_title
import caromobile.core.designsystem.generated.resources.import_dialog_empty_result_body
import caromobile.core.designsystem.generated.resources.import_dialog_empty_result_image_occlusion
import caromobile.core.designsystem.generated.resources.import_dialog_empty_result_title
import caromobile.core.designsystem.generated.resources.import_dialog_encoding_body
import caromobile.core.designsystem.generated.resources.import_dialog_encoding_title
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_apkg
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_body
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_text
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_title
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_xlsx
import caromobile.core.designsystem.generated.resources.import_dialog_paste_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_paste_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_preview_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_preview_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_read_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_read_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_cards_body
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_cards_paste
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_cards_title
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_columns_body
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_apkg
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_generic
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_title
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_xls
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_xlsx
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.NoCardsKind
import com.whatever.caro.feature.importer.logic.ParseErrorBody
import com.whatever.caro.feature.importer.logic.ParseErrorTitle
import com.whatever.caro.feature.importer.logic.parseErrorCopy
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class ImportDialogButtonStyle {
    Brand,
    Dangerous,
    Tertiary,
}

internal data class ImportDialogButton(
    val label: String,
    val style: ImportDialogButtonStyle,
    val intent: ImportDeckIntent,
)

/** [buttons]는 하려는 동작이 앞, 머무는 쪽이 뒤다. [body]의 문장은 줄바꿈 하나로 잇는다. */
internal data class ImportDialogContent(
    val title: String,
    val body: String,
    val buttons: ImmutableList<ImportDialogButton>,
)

@Composable
internal fun importDialogContent(dialog: ImportDialog): ImportDialogContent =
    when (dialog) {
        is ImportDialog.FileTooLarge -> {
            confirmContent(
                Res.string.import_dialog_file_too_large_title,
                Res.string.import_dialog_file_too_large_body,
                sizeHint(dialog.extension),
            )
        }

        is ImportDialog.ParseError -> {
            val copy = parseErrorCopy(dialog)
            confirmContent(copy.title.resource(), *copy.body.resources())
        }

        is ImportDialog.NoCards -> {
            val body =
                when (dialog.kind) {
                    NoCardsKind.ImageOcclusionOnly -> Res.string.import_dialog_empty_result_image_occlusion
                    NoCardsKind.NoContent -> Res.string.import_dialog_empty_result_body
                }
            confirmContent(Res.string.import_dialog_empty_result_title, body)
        }

        is ImportDialog.TooManyCards -> {
            tooManyCardsContent(isPaste = dialog.isPaste)
        }

        ImportDialog.PreviewFailed -> {
            confirmContent(Res.string.import_dialog_preview_failed_title, Res.string.import_dialog_preview_failed_body)
        }

        ImportDialog.DiscardConfirm -> {
            ImportDialogContent(
                title = stringResource(Res.string.import_dialog_discard_title),
                body = stringResource(Res.string.import_dialog_discard_body),
                buttons =
                    persistentListOf(
                        button(Res.string.import_dialog_button_discard, ImportDialogButtonStyle.Dangerous, ImportDeckIntent.ConfirmDiscard),
                        button(Res.string.import_dialog_button_cancel, ImportDialogButtonStyle.Tertiary, ImportDeckIntent.DismissDialog),
                    ),
            )
        }

        ImportDialog.DeckCreateFailed -> {
            deckCreateFailedContent()
        }

        ImportDialog.StopConfirm -> {
            stopConfirmContent()
        }

        is ImportDialog.Paused,
        is ImportDialog.PartialFailure,
        -> {
            retryOrViewDeckContent(dialog)
        }

        is ImportDialog.KeyConflict -> {
            keyConflictContent(dialog)
        }

        ImportDialog.DeckUnavailable -> {
            deckUnavailableContent()
        }
    }

/** 장수는 [ImportLimits] 정책 값이라, 테스트에서 상한을 낮춰 주입해도 안내 문구는 정책 값을 보인다. 둘째 줄은 같은 글자의 파일 크기 안내를 다시 쓴다. */
@Composable
private fun tooManyCardsContent(isPaste: Boolean): ImportDialogContent {
    val hint = if (isPaste) Res.string.import_dialog_too_many_cards_paste else Res.string.import_dialog_file_too_large_text
    val limit = stringResource(Res.string.import_dialog_too_many_cards_body, ImportLimits.MAX_CARDS.formatWithComma())

    return ImportDialogContent(
        title = stringResource(Res.string.import_dialog_too_many_cards_title),
        body = limit + "\n" + stringResource(hint),
        buttons = persistentListOf(confirmButton()),
    )
}

@Composable
private fun confirmContent(
    title: StringResource,
    vararg bodyLines: StringResource,
): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(title),
        body = bodyLines.map { line -> stringResource(line) }.joinToString("\n"),
        buttons = persistentListOf(confirmButton()),
    )

@Composable
private fun confirmButton(): ImportDialogButton =
    button(Res.string.import_dialog_button_confirm, ImportDialogButtonStyle.Brand, ImportDeckIntent.DismissDialog)

@Composable
internal fun button(
    label: StringResource,
    style: ImportDialogButtonStyle,
    intent: ImportDeckIntent,
): ImportDialogButton = ImportDialogButton(stringResource(label), style, intent)

private fun sizeHint(extension: String): StringResource =
    when (extension) {
        "apkg" -> Res.string.import_dialog_file_too_large_apkg
        "xlsx" -> Res.string.import_dialog_file_too_large_xlsx
        else -> Res.string.import_dialog_file_too_large_text
    }

private fun ParseErrorTitle.resource(): StringResource =
    when (this) {
        ParseErrorTitle.Unsupported -> Res.string.import_dialog_unsupported_title
        ParseErrorTitle.Corrupt -> Res.string.import_dialog_corrupt_title
        ParseErrorTitle.UnreadableText -> Res.string.import_dialog_encoding_title
        ParseErrorTitle.ReadFailedFile -> Res.string.import_dialog_read_failed_title
        ParseErrorTitle.ReadFailedPaste -> Res.string.import_dialog_paste_failed_title
    }

/** 손상 안내만 원인과 해결 두 문장을 이어 붙인다. */
private fun ParseErrorBody.resources(): Array<StringResource> =
    when (this) {
        ParseErrorBody.UnsupportedXls -> arrayOf(Res.string.import_dialog_unsupported_xls)
        ParseErrorBody.UnsupportedXlsx -> arrayOf(Res.string.import_dialog_unsupported_xlsx)
        ParseErrorBody.UnsupportedApkg -> arrayOf(Res.string.import_dialog_unsupported_apkg)
        ParseErrorBody.UnsupportedGeneric -> arrayOf(Res.string.import_dialog_unsupported_generic)
        ParseErrorBody.CorruptApkg -> arrayOf(Res.string.import_dialog_corrupt_body, Res.string.import_dialog_corrupt_apkg)
        ParseErrorBody.CorruptXlsx -> arrayOf(Res.string.import_dialog_corrupt_body, Res.string.import_dialog_corrupt_xlsx)
        ParseErrorBody.EncodingUndetermined -> arrayOf(Res.string.import_dialog_encoding_body)
        ParseErrorBody.ReadFailedFile -> arrayOf(Res.string.import_dialog_read_failed_body)
        ParseErrorBody.ReadFailedPaste -> arrayOf(Res.string.import_dialog_paste_failed_body)
        ParseErrorBody.TooManyColumns -> arrayOf(Res.string.import_dialog_too_many_columns_body)
    }
