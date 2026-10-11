package com.whatever.caro.feature.importer.component

import androidx.compose.runtime.Composable
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.import_dialog_button_confirm
import caromobile.core.designsystem.generated.resources.import_dialog_button_continue
import caromobile.core.designsystem.generated.resources.import_dialog_button_discard
import caromobile.core.designsystem.generated.resources.import_dialog_button_go_home
import caromobile.core.designsystem.generated.resources.import_dialog_button_retry
import caromobile.core.designsystem.generated.resources.import_dialog_button_stop
import caromobile.core.designsystem.generated.resources.import_dialog_button_stop_and_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_button_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_check_network
import caromobile.core.designsystem.generated.resources.import_dialog_cause_empty_deck_deletable
import caromobile.core.designsystem.generated.resources.import_dialog_cause_retry_or_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_retry_or_view_empty_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_try_later
import caromobile.core.designsystem.generated.resources.import_dialog_cause_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_apkg
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_body
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_title
import caromobile.core.designsystem.generated.resources.import_dialog_corrupt_xlsx
import caromobile.core.designsystem.generated.resources.import_dialog_deck_create_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_deck_create_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_deck_unavailable_body
import caromobile.core.designsystem.generated.resources.import_dialog_deck_unavailable_title
import caromobile.core.designsystem.generated.resources.import_dialog_discard_body
import caromobile.core.designsystem.generated.resources.import_dialog_discard_title
import caromobile.core.designsystem.generated.resources.import_dialog_empty_result_body
import caromobile.core.designsystem.generated.resources.import_dialog_empty_result_title
import caromobile.core.designsystem.generated.resources.import_dialog_encoding_body
import caromobile.core.designsystem.generated.resources.import_dialog_encoding_title
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_apkg
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_body
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_text
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_title
import caromobile.core.designsystem.generated.resources.import_dialog_file_too_large_xlsx
import caromobile.core.designsystem.generated.resources.import_dialog_key_conflict_title
import caromobile.core.designsystem.generated.resources.import_dialog_partial_failure_title
import caromobile.core.designsystem.generated.resources.import_dialog_paste_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_paste_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_paused_title
import caromobile.core.designsystem.generated.resources.import_dialog_preview_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_preview_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_read_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_read_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_saved_cards
import caromobile.core.designsystem.generated.resources.import_dialog_saved_none
import caromobile.core.designsystem.generated.resources.import_dialog_stop_body
import caromobile.core.designsystem.generated.resources.import_dialog_stop_title
import caromobile.core.designsystem.generated.resources.import_dialog_too_many_columns_body
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_apkg
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_generic
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_title
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_xls
import caromobile.core.designsystem.generated.resources.import_dialog_unsupported_xlsx
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.ParseErrorBody
import com.whatever.caro.feature.importer.logic.ParseErrorTitle
import com.whatever.caro.feature.importer.logic.parseErrorCopy
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
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

/** 카드 전체 보기는 이 틀로 그리지 않으므로 null이다. */
@Composable
internal fun importDialogContent(dialog: ImportDialog): ImportDialogContent? =
    when (dialog) {
        is ImportDialog.FileTooLarge -> {
            val sizeHint =
                when (dialog.extension) {
                    "apkg" -> Res.string.import_dialog_file_too_large_apkg
                    "xlsx" -> Res.string.import_dialog_file_too_large_xlsx
                    else -> Res.string.import_dialog_file_too_large_text
                }
            confirmContent(Res.string.import_dialog_file_too_large_title, Res.string.import_dialog_file_too_large_body, sizeHint)
        }

        is ImportDialog.ParseError -> {
            val copy = parseErrorCopy(dialog)
            confirmContent(copy.title.resource(), *copy.body.resources())
        }

        is ImportDialog.EmptyResult -> {
            confirmContent(Res.string.import_dialog_empty_result_title, Res.string.import_dialog_empty_result_body)
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
            ImportDialogContent(
                title = stringResource(Res.string.import_dialog_deck_create_failed_title),
                body = stringResource(Res.string.import_dialog_deck_create_failed_body),
                buttons =
                    persistentListOf(
                        button(Res.string.import_dialog_button_retry, ImportDialogButtonStyle.Brand, ImportDeckIntent.RetryCreateDeck),
                        button(Res.string.import_dialog_button_cancel, ImportDialogButtonStyle.Tertiary, ImportDeckIntent.DismissDialog),
                    ),
            )
        }

        ImportDialog.StopConfirm -> {
            ImportDialogContent(
                title = stringResource(Res.string.import_dialog_stop_title),
                body = stringResource(Res.string.import_dialog_stop_body),
                buttons =
                    persistentListOf(
                        button(Res.string.import_dialog_button_stop, ImportDialogButtonStyle.Dangerous, ImportDeckIntent.ConfirmStop),
                        button(Res.string.import_dialog_button_continue, ImportDialogButtonStyle.Tertiary, ImportDeckIntent.DismissDialog),
                    ),
            )
        }

        is ImportDialog.Paused -> {
            val cause =
                when (dialog.reason) {
                    PauseReason.NETWORK -> Res.string.import_dialog_cause_check_network
                    PauseReason.LATER -> Res.string.import_dialog_cause_try_later
                }
            retryOrViewDeckContent(Res.string.import_dialog_paused_title, dialog.saved, cause)
        }

        is ImportDialog.PartialFailure -> {
            val cause =
                if (dialog.saved == 0) {
                    Res.string.import_dialog_cause_retry_or_view_empty_deck
                } else {
                    Res.string.import_dialog_cause_retry_or_view_deck
                }
            retryOrViewDeckContent(Res.string.import_dialog_partial_failure_title, dialog.saved, cause)
        }

        is ImportDialog.KeyConflict -> {
            val cause =
                if (dialog.saved == 0) {
                    Res.string.import_dialog_cause_empty_deck_deletable
                } else {
                    Res.string.import_dialog_cause_view_deck
                }
            ImportDialogContent(
                title = stringResource(Res.string.import_dialog_key_conflict_title),
                body = savedCardsLine(dialog.saved) + "\n" + stringResource(cause),
                buttons =
                    persistentListOf(
                        button(Res.string.import_dialog_button_view_deck, ImportDialogButtonStyle.Brand, ImportDeckIntent.ClickViewDeck),
                    ),
            )
        }

        ImportDialog.DeckUnavailable -> {
            ImportDialogContent(
                title = stringResource(Res.string.import_dialog_deck_unavailable_title),
                body = stringResource(Res.string.import_dialog_deck_unavailable_body),
                buttons =
                    persistentListOf(
                        button(Res.string.import_dialog_button_go_home, ImportDialogButtonStyle.Brand, ImportDeckIntent.ClickGoHome),
                    ),
            )
        }

        is ImportDialog.CardDetail -> {
            null
        }
    }

@Composable
private fun confirmContent(
    title: StringResource,
    vararg bodyLines: StringResource,
): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(title),
        body = bodyLines.map { line -> stringResource(line) }.joinToString("\n"),
        buttons =
            persistentListOf(
                button(Res.string.import_dialog_button_confirm, ImportDialogButtonStyle.Brand, ImportDeckIntent.DismissDialog),
            ),
    )

@Composable
private fun retryOrViewDeckContent(
    title: StringResource,
    saved: Int,
    cause: StringResource,
): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(title),
        body = savedCardsLine(saved) + "\n" + stringResource(cause),
        buttons =
            persistentListOf(
                button(Res.string.import_dialog_button_retry, ImportDialogButtonStyle.Brand, ImportDeckIntent.RetrySave),
                button(
                    Res.string.import_dialog_button_stop_and_view_deck,
                    ImportDialogButtonStyle.Tertiary,
                    ImportDeckIntent.ClickViewDeck,
                ),
            ),
    )

@Composable
private fun savedCardsLine(saved: Int): String =
    if (saved == 0) {
        stringResource(Res.string.import_dialog_saved_none)
    } else {
        stringResource(Res.string.import_dialog_saved_cards, saved.formatWithComma())
    }

@Composable
private fun button(
    label: StringResource,
    style: ImportDialogButtonStyle,
    intent: ImportDeckIntent,
): ImportDialogButton = ImportDialogButton(stringResource(label), style, intent)

private fun ParseErrorTitle.resource(): StringResource =
    when (this) {
        ParseErrorTitle.Unsupported -> Res.string.import_dialog_unsupported_title
        ParseErrorTitle.Corrupt -> Res.string.import_dialog_corrupt_title
        ParseErrorTitle.UnreadableText -> Res.string.import_dialog_encoding_title
        ParseErrorTitle.ReadFailedFile -> Res.string.import_dialog_read_failed_title
        ParseErrorTitle.ReadFailedPaste -> Res.string.import_dialog_paste_failed_title
    }

/** 손상 안내만 두 문장(원인과 해결)이다. */
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
