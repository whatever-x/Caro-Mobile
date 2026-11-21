package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_field_label_description
import caromobile.core.designsystem.generated.resources.deck_field_label_name
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_description
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_name
import caromobile.core.designsystem.generated.resources.import_button_apply
import caromobile.core.designsystem.generated.resources.import_title_deck_info
import com.whatever.caro.core.designsystem.components.CaroCtaButton
import com.whatever.caro.core.designsystem.components.CaroFieldCounter
import com.whatever.caro.core.designsystem.components.CaroRequiredFieldHeader
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.feature.importer.components.ImportSheetHandle
import com.whatever.caro.feature.importer.mvi.DeckInfoDraft
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * 바깥 누름, 끌어내림, 시트 창의 뒤로 가기는 모두 [onDismissRequest]로 들어와 바꾼 내용 없이 닫는다.
 * [적용]은 시트를 밀어 내린 뒤에 보내야 시트가 내려가는 모습이 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckInfoSheet(
    draft: DeckInfoDraft,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onApply: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 반만 열리면 [적용]이 화면 밖에 남을 수 있어 항상 끝까지 펼친다.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CaroTheme.spacing.xl2, topEnd = CaroTheme.spacing.xl2),
        containerColor = CaroTheme.color.surface.primary,
        dragHandle = { ImportSheetHandle() },
    ) {
        DeckInfoSheetContent(
            draft = draft,
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onApply = { scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onApply() } },
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/**
 * 키보드가 열리면 시트 창의 inset만큼 높이가 줄어, 제목과 입력칸만 스크롤되고 [적용]은 키보드 바로 위에 남는다.
 * 글자 수 상한은 ViewModel이 자르므로 이 화면은 값을 그대로 보인다.
 */
@Composable
internal fun DeckInfoSheetContent(
    draft: DeckInfoDraft,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descriptionFocus = remember { FocusRequester() }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CaroTheme.spacing.xl2)
                    .padding(top = CaroTheme.spacing.m, bottom = CaroTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
        ) {
            DeckInfoSheetTitle(modifier = Modifier.padding(bottom = CaroTheme.spacing.xs))
            CaroTextField(
                value = draft.name,
                onValueChange = onNameChange,
                placeholder = stringResource(Res.string.deck_field_placeholder_name),
                header = { CaroRequiredFieldHeader(label = stringResource(Res.string.deck_field_label_name)) },
                footer = { CaroFieldCounter(count = "${draft.name.length}/${DeckInputLimits.NAME_MAX}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { descriptionFocus.requestFocus() }),
            )
            CaroTextArea(
                value = draft.description,
                onValueChange = onDescriptionChange,
                placeholder = stringResource(Res.string.deck_field_placeholder_description),
                header = { CaroRequiredFieldHeader(label = stringResource(Res.string.deck_field_label_description)) },
                footer = { CaroFieldCounter(count = "${draft.description.length}/${DeckInputLimits.DESC_MAX}") },
                focusRequester = descriptionFocus,
            )
        }
        CaroCtaButton(
            text = stringResource(Res.string.import_button_apply),
            onClick = onApply,
            modifier =
                Modifier.fillMaxWidth().padding(
                    start = CaroTheme.spacing.xl2,
                    end = CaroTheme.spacing.xl2,
                    bottom = CaroTheme.spacing.l,
                ),
            enabled = draft.isApplyEnabled,
        )
    }
}

/** 시트 창은 화면의 제목 포커스 대상과 별개라 열릴 때 제목에 직접 포커스를 둔다. 입력칸에 두면 열자마자 키보드가 올라온다. */
@Composable
private fun DeckInfoSheetTitle(modifier: Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Text(
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .semantics { heading() },
        text = stringResource(Res.string.import_title_deck_info),
        style = CaroTheme.typography.heading2,
        color = CaroTheme.color.text.primary,
    )
}
