package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val CaroDialogWidth = 330.dp

/**
 * 슬롯 API 기반의 공용 다이얼로그.
 *
 * 컴포넌트는 구조(제목 → 본문 → 버튼의 세로 배치, 고정 너비, 카드 표면/패딩)만 기여하고,
 * 각 영역에 무엇을 그릴지는 호출자가 슬롯으로 위임한다.
 *
 * - [title] : 상단 제목 영역. 없으면 공간을 차지하지 않는다.
 * - [content] : 가운데 메인 본문 영역(필수).
 * - [buttons] : 하단 버튼 영역. [RowScope] 를 받으므로 `Modifier.weight(1f)` 로 동일 너비 버튼을 배치할 수 있다.
 *   없으면 공간을 차지하지 않는다.
 *
 * @param onDismissRequest 스크림 터치/뒤로가기 등으로 다이얼로그가 닫혀야 할 때 호출된다.
 */
@Composable
fun CaroDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        CaroDialogContent(
            modifier = modifier,
            title = title,
            buttons = buttons,
            content = content,
        )
    }
}

@Composable
private fun CaroDialogContent(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .width(CaroDialogWidth)
                .background(
                    color = CaroTheme.color.surface.primary,
                    shape = CaroTheme.shape.l,
                ).padding(CaroTheme.spacing.xl2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        title?.let { titleContent ->
            titleContent()
            Spacer(modifier = Modifier.height(CaroTheme.spacing.m))
        }

        content()

        buttons?.let { buttonsContent ->
            Spacer(modifier = Modifier.height(CaroTheme.spacing.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                content = buttonsContent,
            )
        }
    }
}

@Preview(showBackground = true, name = "Title / Content / Buttons")
@Composable
private fun CaroDialogFullPreview() {
    CaroTheme {
        CaroDialogContent(
            title = { PreviewTitle() },
            buttons = {
                PreviewButton(
                    text = "취소",
                    modifier = Modifier.weight(1f),
                    backgroundColor = CaroTheme.color.surface.secondary,
                    textColor = CaroTheme.color.text.secondary,
                )
                PreviewButton(
                    text = "확인",
                    modifier = Modifier.weight(1f),
                    backgroundColor = CaroTheme.color.surface.brand,
                    textColor = CaroTheme.color.text.inverse,
                )
            },
        ) {
            PreviewBody()
        }
    }
}

@Preview(showBackground = true, name = "Title / Content (No Buttons)")
@Composable
private fun CaroDialogNoButtonsPreview() {
    CaroTheme {
        CaroDialogContent(
            title = { PreviewTitle() },
        ) {
            PreviewBody()
        }
    }
}

private val PreviewButtonHeight = 48.dp

@Composable
private fun PreviewTitle() {
    Text(
        text = "제목",
        style = CaroTheme.typography.heading2,
        color = CaroTheme.color.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PreviewBody() {
    Text(
        text = "가운데 메인 내용이 들어가는 슬롯입니다.",
        style = CaroTheme.typography.body3,
        color = CaroTheme.color.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PreviewButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(PreviewButtonHeight)
                .background(
                    color = backgroundColor,
                    shape = CaroTheme.shape.s,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CaroTheme.typography.body2.semiBold,
            color = textColor,
        )
    }
}
