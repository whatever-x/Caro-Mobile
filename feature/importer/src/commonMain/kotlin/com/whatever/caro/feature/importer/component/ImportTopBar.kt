package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import caromobile.core.designsystem.generated.resources.import_a11y_back
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val BackButtonTouchSize = 48.dp
private val BackIconSize = 24.dp

// 아이콘 왼쪽 끝이 화면 가장자리에서 24dp에 놓이도록 눌리는 영역(48dp)의 시작 여백을 12dp로 둔다.
private val BackButtonStartPadding = 12.dp

@Composable
internal fun ImportTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CaroTopBar(
        modifier = modifier,
        leadingContent = {
            Box(
                modifier =
                    Modifier
                        .padding(start = BackButtonStartPadding)
                        .size(BackButtonTouchSize)
                        .noRippleClickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(BackIconSize),
                    painter = painterResource(Res.drawable.ic_chevron_left_24),
                    contentDescription = stringResource(Res.string.import_a11y_back),
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
