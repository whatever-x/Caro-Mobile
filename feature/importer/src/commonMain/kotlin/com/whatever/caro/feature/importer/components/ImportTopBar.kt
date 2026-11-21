package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.import_content_description_back
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val BackButtonTouchSize = 48.dp
private val BackIconSize = 24.dp
private val BackRippleRadius = 24.dp

// 아이콘 왼쪽 끝이 화면 가장자리에서 24dp에 놓이도록 눌리는 영역(48dp)의 시작 여백을 12dp로 둔다.
private val BackButtonStartPadding = 12.dp

/** [backFocusRequester]는 대화상자를 닫은 뒤 포커스를 돌려받을 때 쓴다. */
@Composable
internal fun ImportTopBar(
    title: String,
    onBack: () -> Unit,
    backFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    CaroTopBar(
        modifier = modifier,
        leadingContent = {
            // clickable의 focus target은 터치 모드에서 포커스 요청을 받지 않아 canFocus를 켠다.
            // focusable을 따로 두면 focusRequester가 그쪽을 잡아 Enter가 clickable에 닿지 않는다.
            Box(
                modifier =
                    Modifier
                        .padding(start = BackButtonStartPadding)
                        .size(BackButtonTouchSize)
                        .focusRequester(backFocusRequester)
                        .focusProperties { canFocus = true }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false, radius = BackRippleRadius),
                            role = Role.Button,
                            onClick = onBack,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(BackIconSize),
                    painter = painterResource(Res.drawable.ic_arrow_left_24),
                    contentDescription = stringResource(Res.string.import_content_description_back),
                    tint = CaroTheme.color.icon.brand,
                )
            }
        },
        centerContent = {
            Text(
                text = title,
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
        },
    )
}
