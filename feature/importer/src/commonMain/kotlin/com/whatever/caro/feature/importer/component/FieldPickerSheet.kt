package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_a11y_field_moved
import caromobile.core.designsystem.generated.resources.import_a11y_field_order_state
import caromobile.core.designsystem.generated.resources.import_button_done
import caromobile.core.designsystem.generated.resources.import_hint_pick_order
import caromobile.core.designsystem.generated.resources.import_label_in_side
import caromobile.core.designsystem.generated.resources.import_label_order
import com.whatever.caro.core.designsystem.components.CaroCheckbox
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.logic.PickerRow
import com.whatever.caro.feature.importer.logic.truncateChip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * 창을 띄우지 않고 내용만 확인하는 테스트가 틀을 바꿔 끼울 수 있도록 분리했다. 밀어 닫기와 뒤로 가기는 모두 [onDismissRequest]로 들어온다.
 * 내용 안의 닫기 버튼은 시트를 밀어 내린 뒤에 닫아야 하므로 틀이 만든 `close`를 받아 쓴다.
 */
internal typealias FieldPickerSheetFrame =
    @Composable (
        onDismissRequest: () -> Unit,
        content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
    ) -> Unit

private val RowMinHeight = 50.dp
private val HandleWidth = 40.dp
private val HandleHeight = 4.dp
private const val NAME_MAX_LINES = 2

@OptIn(ExperimentalMaterial3Api::class)
private val ModalFieldPickerSheetFrame: FieldPickerSheetFrame = { onDismissRequest, content ->
    // 반만 열리면 완료 버튼이 화면 밖에 남을 수 있어 항상 끝까지 펼친다.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CaroTheme.spacing.xl2, topEnd = CaroTheme.spacing.xl2),
        containerColor = CaroTheme.color.surface.primary,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(color = CaroTheme.color.surface.primary)
                        .padding(vertical = CaroTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(HandleWidth)
                            .height(HandleHeight)
                            .background(color = CaroTheme.color.divider.primary, shape = CaroTheme.shape.xxs),
                )
            }
        },
    ) {
        content {
            scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismissRequest() }
        }
    }
}

/**
 * 고른 결과는 줄을 누를 때마다 [onToggleField]로 바로 전달하므로 [onDismiss]는 닫기만 한다.
 * [rows]의 [PickerRow.order]가 있는 줄이 이 면에 이미 고른 항목이고, 다른 면의 항목을 고르면 옮겨졌다고 알린다.
 */
@Composable
internal fun FieldPickerSheet(
    title: String,
    sideLabel: String,
    otherSideLabel: String,
    rows: ImmutableList<PickerRow>,
    onToggleField: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    frame: FieldPickerSheetFrame = ModalFieldPickerSheetFrame,
) {
    var movedOrder by remember { mutableStateOf<Int?>(null) }
    val movedMessage = movedOrder?.let { order -> stringResource(Res.string.import_a11y_field_moved, otherSideLabel, sideLabel, order) }

    frame(onDismiss) { close ->
        Column(modifier = modifier.weight(1f, fill = false).fillMaxWidth()) {
            PickerSheetHeader(title = title, movedMessage = movedMessage)
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(rows, key = { row -> row.name }) { row ->
                    PickerRowItem(
                        row = row,
                        sideLabel = sideLabel,
                        otherSideLabel = otherSideLabel,
                        onClick = {
                            movedOrder = if (row.isInOtherSide) rows.count { other -> other.order != null } + 1 else null
                            onToggleField(row.name)
                        },
                    )
                }
            }
            ImportBottomBar(buttonText = stringResource(Res.string.import_button_done), onClick = close)
        }
    }
}

/** 시트 창은 화면의 제목 포커스 대상과 별개라 [ImportStepTitle]을 쓰지 않고 열릴 때 제목에 직접 포커스를 둔다. */
@Composable
private fun PickerSheetHeader(
    title: String,
    movedMessage: String?,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = CaroTheme.spacing.xl2,
                    top = CaroTheme.spacing.m,
                    end = CaroTheme.spacing.xl2,
                    bottom = CaroTheme.spacing.l,
                ),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .semantics { heading() },
            text = title,
            style = CaroTheme.typography.heading2,
            color = CaroTheme.color.text.primary,
        )
        // 보이지 않는 live region이 안내를 맡는다. 줄의 오른쪽 문구는 화면에서 이미 바뀌므로 보이는 글자를 더하지 않는다.
        Box {
            Text(
                text = stringResource(Res.string.import_hint_pick_order),
                style = CaroTheme.typography.label1,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(
                modifier =
                    Modifier.size(1.dp).semantics {
                        liveRegion = LiveRegionMode.Polite
                        if (movedMessage != null) contentDescription = movedMessage
                    },
            )
        }
    }
}

/**
 * 상태 semantics는 줄 전체의 toggleable이 맡으므로 체크박스는 장식이다.
 * 고른 줄은 이름만 글자로 남기고 선택됨과 면, 순서를 상태 설명 하나로 읽어 선택됨이 두 번 읽히지 않게 한다.
 */
@Composable
private fun PickerRowItem(
    row: PickerRow,
    sideLabel: String,
    otherSideLabel: String,
    onClick: () -> Unit,
) {
    val name = truncateChip(row.name)
    val chosenState = row.order?.let { order -> stringResource(Res.string.import_a11y_field_order_state, sideLabel, order) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .toggleable(value = row.order != null, role = Role.Checkbox, onValueChange = { onClick() })
                .then(
                    if (chosenState != null) {
                        Modifier.clearAndSetSemantics {
                            text = AnnotatedString(name)
                            stateDescription = chosenState
                        }
                    } else {
                        Modifier
                    },
                ).padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaroCheckbox(state = if (row.order != null) ToggleableState.On else ToggleableState.Off)
        Text(
            modifier = Modifier.weight(1f),
            text = name,
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.primary,
            maxLines = NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        val trailing =
            when {
                row.order != null -> stringResource(Res.string.import_label_order, row.order)
                row.isInOtherSide -> stringResource(Res.string.import_label_in_side, otherSideLabel)
                else -> null
            }
        if (trailing != null) {
            Text(text = trailing, style = CaroTheme.typography.caption1.regular, color = CaroTheme.color.text.secondary)
        }
    }
}
