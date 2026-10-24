package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_tip_label
import com.whatever.caro.core.designsystem.foundation.CaroTipDotSize
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

/** [lines]는 List 그대로 받는다. strong skipping은 인스턴스 동일성으로 비교하므로 호출부가 remember로 같은 목록을 유지한다. */
@Composable
fun CaroTipSection(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    val lineStyle = CaroTheme.typography.caption1.regular

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.deck_tip_label),
            style = lineStyle,
            color = CaroTheme.color.text.secondary,
        )
        lines.forEach { line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                verticalAlignment = Alignment.Top,
            ) {
                // 줄이 감겨도 점은 첫 줄 가운데에 놓여야 한다. 첫 줄 높이는 같은 스타일의 빈 Text로 얻는다.
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "",
                        modifier = Modifier.clearAndSetSemantics {},
                        style = lineStyle,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(CaroTipDotSize)
                                .clip(CircleShape)
                                .background(CaroTheme.color.text.tertiary),
                    )
                }
                Text(
                    text = line,
                    style = lineStyle,
                    color = CaroTheme.color.text.tertiary,
                )
            }
        }
    }
}
