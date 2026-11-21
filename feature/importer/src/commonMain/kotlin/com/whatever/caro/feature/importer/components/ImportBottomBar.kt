package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import com.whatever.caro.core.designsystem.components.CaroCtaButton
import com.whatever.caro.core.designsystem.components.CaroCtaButtonStyle
import com.whatever.caro.core.designsystem.themes.CaroTheme

/** [focusRequester]는 대화상자를 닫은 뒤 버튼으로 포커스를 돌려받을 화면만 넘긴다. */
@Composable
internal fun ImportBottomBar(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledReason: String? = null,
    style: CaroCtaButtonStyle = CaroCtaButtonStyle.Brand,
    focusRequester: FocusRequester? = null,
) {
    val gradient = Brush.verticalGradient(listOf(CaroTheme.color.gradient.tertiaryStart, CaroTheme.color.gradient.tertiaryEnd))
    // clickable의 focus target은 터치 모드에서 포커스 요청을 받지 않아, 켜진 버튼만 canFocus를 켠다.
    val focusModifier = focusRequester?.let { requester -> Modifier.focusRequester(requester).focusProperties { canFocus = enabled } }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(start = CaroTheme.spacing.xl2, end = CaroTheme.spacing.xl2, bottom = CaroTheme.spacing.l),
    ) {
        CaroCtaButton(
            text = buttonText,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().importBottomButtonTop().then(focusModifier ?: Modifier),
            enabled = enabled,
            style = style,
            disabledReason = disabledReason,
        )
    }
}
