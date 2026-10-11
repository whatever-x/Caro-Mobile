package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.components.CaroCtaButton
import com.whatever.caro.core.designsystem.components.CaroCtaButtonStyle
import com.whatever.caro.core.designsystem.foundation.CaroPageHorizontalPadding
import com.whatever.caro.core.designsystem.themes.CaroTheme

/** [focusRequester]는 팝업을 닫은 뒤 포커스를 돌려받을 버튼이 필요한 화면만 넘긴다. */
@Composable
internal fun ImportBottomBar(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reason: String? = null,
    style: CaroCtaButtonStyle = CaroCtaButtonStyle.Brand,
    focusRequester: FocusRequester? = null,
    summary: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .padding(
                    horizontal = CaroPageHorizontalPadding,
                    vertical = CaroTheme.spacing.l,
                ),
    ) {
        if (summary != null) {
            summary()
            Spacer(modifier = Modifier.height(CaroTheme.spacing.s))
        }
        // 새로 생긴 노드에는 live region 알림이 가지 않을 수 있어 사유 줄을 늘 두고 글자만 바꾼다. 사유가 없으면 높이와 간격을 차지하지 않는다.
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (reason == null) Modifier.height(0.dp) else Modifier.padding(bottom = CaroTheme.spacing.s))
                    .semantics { liveRegion = LiveRegionMode.Polite },
            text = reason.orEmpty(),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
        CaroCtaButton(
            text = buttonText,
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester).focusable(enabled = enabled) else Modifier),
            enabled = enabled,
            style = style,
            disabledReason = reason,
        )
    }
}
