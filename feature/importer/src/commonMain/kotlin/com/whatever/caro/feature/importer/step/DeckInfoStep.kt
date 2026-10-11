package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_field_label_description
import caromobile.core.designsystem.generated.resources.deck_field_label_name
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_description
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_name
import caromobile.core.designsystem.generated.resources.import_button_import
import caromobile.core.designsystem.generated.resources.import_hint_deck_info_summary
import com.whatever.caro.core.designsystem.components.CaroFieldCounter
import com.whatever.caro.core.designsystem.components.CaroRequiredFieldHeader
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.component.ImportBottomBar
import org.jetbrains.compose.resources.stringResource

/**
 * 입력칸에는 처음 그릴 때 포커스를 두지 않는다. 키보드가 열리면 내용이 줄어든 높이 안에서 스크롤되고 [가져오기]는 키보드 바로 위에 붙는다.
 * 글자 수 상한은 ViewModel이 자르므로 이 화면은 값을 그대로 보여준다.
 */
@Composable
internal fun DeckInfoStep(
    name: String,
    description: String,
    cardCount: Int,
    isConfirmEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // 입력칸이 소비하지 않은 탭만 여기로 오므로 입력칸 바깥을 누르면 키보드가 닫힌다.
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl2),
        ) {
            CaroTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = stringResource(Res.string.deck_field_placeholder_name),
                header = { CaroRequiredFieldHeader(label = stringResource(Res.string.deck_field_label_name)) },
                footer = { CaroFieldCounter(count = "${name.length}/${DeckInputLimits.NAME_MAX}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            CaroTextArea(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = stringResource(Res.string.deck_field_placeholder_description),
                header = { CaroRequiredFieldHeader(label = stringResource(Res.string.deck_field_label_description)) },
                footer = { CaroFieldCounter(count = "${description.length}/${DeckInputLimits.DESC_MAX}") },
            )
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(CaroTheme.color.surface.secondary, CaroTheme.shape.m)
                        .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
                text = stringResource(Res.string.import_hint_deck_info_summary, cardCount.formatWithComma()),
                style = CaroTheme.typography.label1,
                color = CaroTheme.color.text.primary,
            )
        }
        ImportBottomBar(
            buttonText = stringResource(Res.string.import_button_import),
            onClick = onConfirm,
            enabled = isConfirmEnabled,
        )
    }
}
