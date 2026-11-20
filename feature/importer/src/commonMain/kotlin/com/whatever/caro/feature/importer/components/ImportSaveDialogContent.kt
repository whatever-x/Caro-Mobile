package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.Composable
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.import_dialog_button_continue
import caromobile.core.designsystem.generated.resources.import_dialog_button_go_home
import caromobile.core.designsystem.generated.resources.import_dialog_button_retry
import caromobile.core.designsystem.generated.resources.import_dialog_button_stop
import caromobile.core.designsystem.generated.resources.import_dialog_button_stop_and_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_button_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_check_in_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_check_network
import caromobile.core.designsystem.generated.resources.import_dialog_cause_retry_or_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_retry_or_view_empty_deck
import caromobile.core.designsystem.generated.resources.import_dialog_cause_try_later
import caromobile.core.designsystem.generated.resources.import_dialog_cause_view_deck
import caromobile.core.designsystem.generated.resources.import_dialog_deck_create_failed_body
import caromobile.core.designsystem.generated.resources.import_dialog_deck_create_failed_title
import caromobile.core.designsystem.generated.resources.import_dialog_deck_unavailable_body
import caromobile.core.designsystem.generated.resources.import_dialog_deck_unavailable_title
import caromobile.core.designsystem.generated.resources.import_dialog_key_conflict_title
import caromobile.core.designsystem.generated.resources.import_dialog_partial_failure_title
import caromobile.core.designsystem.generated.resources.import_dialog_paused_title
import caromobile.core.designsystem.generated.resources.import_dialog_saved_cards
import caromobile.core.designsystem.generated.resources.import_dialog_saved_none
import caromobile.core.designsystem.generated.resources.import_dialog_saved_none_unconfirmed
import caromobile.core.designsystem.generated.resources.import_dialog_saved_unconfirmed
import caromobile.core.designsystem.generated.resources.import_dialog_stop_body
import caromobile.core.designsystem.generated.resources.import_dialog_stop_title
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.CauseLine
import com.whatever.caro.feature.importer.logic.SaveDialogBody
import com.whatever.caro.feature.importer.logic.SavedCountLine
import com.whatever.caro.feature.importer.logic.saveDialogBody
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun deckCreateFailedContent(): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(Res.string.import_dialog_deck_create_failed_title),
        body = stringResource(Res.string.import_dialog_deck_create_failed_body),
        buttons =
            persistentListOf(
                button(Res.string.import_dialog_button_retry, ImportDialogButtonStyle.Brand, ImportDeckIntent.RetryCreateDeck),
                button(Res.string.import_dialog_button_cancel, ImportDialogButtonStyle.Tertiary, ImportDeckIntent.DismissDialog),
            ),
    )

@Composable
internal fun stopConfirmContent(): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(Res.string.import_dialog_stop_title),
        body = stringResource(Res.string.import_dialog_stop_body),
        buttons =
            persistentListOf(
                button(Res.string.import_dialog_button_stop, ImportDialogButtonStyle.Dangerous, ImportDeckIntent.ConfirmStop),
                button(Res.string.import_dialog_button_continue, ImportDialogButtonStyle.Tertiary, ImportDeckIntent.DismissDialog),
            ),
    )

/** 일시 정지와 일부 저장 실패는 다시 보낼 수 있는 실패라 [다시 시도]와 [멈추고 덱 보기]를 둔다. */
@Composable
internal fun retryOrViewDeckContent(dialog: ImportDialog): ImportDialogContent {
    val title = if (dialog is ImportDialog.Paused) Res.string.import_dialog_paused_title else Res.string.import_dialog_partial_failure_title
    return ImportDialogContent(
        title = stringResource(title),
        body = saveBodyText(dialog),
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
}

@Composable
internal fun keyConflictContent(dialog: ImportDialog.KeyConflict): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(Res.string.import_dialog_key_conflict_title),
        body = saveBodyText(dialog),
        buttons =
            persistentListOf(
                button(Res.string.import_dialog_button_view_deck, ImportDialogButtonStyle.Brand, ImportDeckIntent.ClickViewDeck),
            ),
    )

@Composable
internal fun deckUnavailableContent(): ImportDialogContent =
    ImportDialogContent(
        title = stringResource(Res.string.import_dialog_deck_unavailable_title),
        body = stringResource(Res.string.import_dialog_deck_unavailable_body),
        buttons =
            persistentListOf(
                button(Res.string.import_dialog_button_go_home, ImportDialogButtonStyle.Brand, ImportDeckIntent.ClickGoHome),
            ),
    )

/** 어떤 문장을 고를지는 [saveDialogBody]가 정하고 여기서는 문구 자원으로만 바꾼다. */
@Composable
private fun saveBodyText(dialog: ImportDialog): String {
    val body = checkNotNull(saveDialogBody(dialog)) { "저장 장수를 담지 않는 대화상자다: $dialog" }
    val savedLine = savedLineText(body)
    val causeLine = body.causeLine?.let { cause -> stringResource(cause.resource()) }
    return listOfNotNull(savedLine, causeLine).joinToString("\n")
}

@Composable
private fun savedLineText(body: SaveDialogBody): String {
    val saved = body.saved.formatWithComma()
    return when (body.savedLine) {
        SavedCountLine.SavedCards -> stringResource(Res.string.import_dialog_saved_cards, saved)
        SavedCountLine.NoCardsSaved -> stringResource(Res.string.import_dialog_saved_none)
        SavedCountLine.SavedThenUnconfirmed -> stringResource(Res.string.import_dialog_saved_unconfirmed, saved)
        SavedCountLine.NoneUnconfirmed -> stringResource(Res.string.import_dialog_saved_none_unconfirmed)
    }
}

private fun CauseLine.resource(): StringResource =
    when (this) {
        CauseLine.CheckNetwork -> Res.string.import_dialog_cause_check_network
        CauseLine.TryLater -> Res.string.import_dialog_cause_try_later
        CauseLine.RetryOrViewDeck -> Res.string.import_dialog_cause_retry_or_view_deck
        CauseLine.RetryOrViewEmptyDeck -> Res.string.import_dialog_cause_retry_or_view_empty_deck
        CauseLine.ViewDeck -> Res.string.import_dialog_cause_view_deck
        CauseLine.CheckInDeck -> Res.string.import_dialog_cause_check_in_deck
    }
