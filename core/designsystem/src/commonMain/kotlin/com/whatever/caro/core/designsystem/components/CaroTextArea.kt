package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

/**
 * Large 사이즈 Text Field Box.
 * 내부 Input Box 의 고정 높이(기본 90dp) 만큼 상단 정렬되어 멀티라인 입력을 받는다.
 * Figma: node-id 2582-4870 (대기), 2582-4874 (입력 중), 2582-4882 (완료)
 */
@Composable
fun CaroTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        header?.invoke()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            TextAreaBox(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                enabled = enabled,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                focusRequester = focusRequester,
            )

            footer?.invoke()
        }
    }
}

@Composable
private fun TextAreaBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    enabled: Boolean,
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    interactionSource: MutableInteractionSource,
    focusRequester: FocusRequester?,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val textColor =
        if (enabled) {
            CaroTheme.color.text.primary
        } else {
            CaroTheme.color.text.disabled
        }
    val surfaceColor =
        if (enabled) {
            CaroTheme.color.surface.primary
        } else {
            CaroTheme.color.surface.secondary
        }
    val borderColor =
        if (isFocused) {
            CaroTheme.color.border.brand
        } else {
            CaroTheme.color.border.secondary
        }
    val mergedTextStyle =
        CaroTheme.typography.body1
            .copy(color = textColor)
    val selectionColors =
        TextSelectionColors(
            handleColor = CaroTheme.color.text.brand,
            backgroundColor =
                CaroTheme.color.text.brand
                    .copy(alpha = 0.2f),
        )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CaroTheme.shape.m)
                    .background(color = surfaceColor, shape = CaroTheme.shape.m)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = CaroTheme.shape.m,
                    )
                    // 눌리는 영역을 명시적으로 보여주기 위해 press 상태에만 반응하는 오버레이를 얹는다.
                    // Material ripple(LocalIndication)은 focus 상태에도 계속 칠해져 입력 중에도 회색으로 보이므로 쓰지 않는다.
                    .background(
                        color =
                            if (isPressed) {
                                CaroTheme.color.text.primary
                                    .copy(alpha = PRESSED_OVERLAY_ALPHA)
                            } else {
                                Color.Transparent
                            },
                        shape = CaroTheme.shape.m,
                    ).padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.l,
                    ),
            contentAlignment = Alignment.TopStart,
        ) {
            // 입력이 늘어나면 최대 높이까지 커지고, 그 뒤로는 BasicTextField 내부 스크롤이 커서를 따라간다.
            // 크기 제약은 BasicTextField 자체에 준다. 바깥 Box 로 감싸면 필드가 첫 줄 높이만 차지해
            // 나머지 영역이 터치 死영역이 되고, 외부 verticalScroll 로 감싸면 높이가 무한이 되어
            // 내부 커서 추적 스크롤이 죽는다.
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = InputBoxMinHeight, max = InputBoxMaxHeight)
                        .then(
                            focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                        ),
                enabled = enabled,
                readOnly = readOnly,
                textStyle = mergedTextStyle,
                singleLine = false,
                cursorBrush = SolidColor(CaroTheme.color.text.brand),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
            )
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    style = CaroTheme.typography.body1,
                    color = CaroTheme.color.text.tertiary,
                )
            }
        }
    }
}

private const val PRESSED_OVERLAY_ALPHA = 0.06f

private val InputBoxMinHeight = 90.dp

// 키보드가 올라온 상태의 스크롤 뷰포트 안에 라벨·카운터까지 함께 들어가는 상한.
// 이보다 크면 필드 하단(= 커서가 머무는 줄)이 뷰포트 밖으로 잘린다.
private val InputBoxMaxHeight = 140.dp

@Preview
@Composable
private fun CaroTextAreaDefaultPreview() {
    PreviewContainer {
        CaroTextArea(
            value = "",
            onValueChange = {},
            placeholder = "Placeholder",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaWithTitlePreview() {
    PreviewContainer {
        CaroTextArea(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "자기소개",
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.primary,
                )
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaRequiredPreview() {
    PreviewContainer {
        CaroTextArea(
            value = "",
            onValueChange = {},
            header = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "자기소개",
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.primary,
                    )
                    Text(
                        text = "*",
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.dangerous,
                    )
                }
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaWithCaptionPreview() {
    PreviewContainer {
        CaroTextArea(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "자기소개",
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Text(
                    text = "최대 200자까지 입력할 수 있어요",
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.secondary,
                )
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaWithCounterPreview() {
    PreviewContainer {
        val value = "안녕하세요, 카로입니다."
        val max = 200
        CaroTextArea(
            value = value,
            onValueChange = {},
            header = {
                Text(
                    text = "자기소개",
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${value.length}/$max",
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                        textAlign = TextAlign.End,
                    )
                }
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaFilledPreview() {
    PreviewContainer {
        val value =
            "안녕하세요, 저는 카로예요.\n새로운 사람들과 만나는 걸 좋아해요.\n잘 부탁드립니다 :)"
        val max = 200
        CaroTextArea(
            value = value,
            onValueChange = {},
            header = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "자기소개",
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.primary,
                    )
                    Text(
                        text = "*",
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.dangerous,
                    )
                }
            },
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "최대 ${max}자까지 입력할 수 있어요",
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${value.length}/$max",
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                        textAlign = TextAlign.End,
                    )
                }
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaOverflowPreview() {
    PreviewContainer {
        val value =
            "안녕하세요, 저는 카로예요.\n" +
                "최근에는 새로운 사람들과 만나는 데 관심이 많아졌어요.\n" +
                "취미는 영화 감상과 요리입니다.\n" +
                "주말에는 종종 산책을 하거나 카페에서 책을 읽어요.\n" +
                "잘 부탁드립니다 :)\n" +
                "추가로 적자면, 운동도 좋아하고 새벽 러닝을 즐깁니다."
        val max = 200
        CaroTextArea(
            value = value,
            onValueChange = {},
            header = {
                Text(
                    text = "자기소개",
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${value.length}/$max",
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                        textAlign = TextAlign.End,
                    )
                }
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaCustomFooterPreview() {
    PreviewContainer {
        CaroTextArea(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "자기소개",
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Text(
                    text = "필수 입력 항목이에요",
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.dangerous,
                )
            },
            placeholder = "자기소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaDisabledPreview() {
    PreviewContainer {
        CaroTextArea(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "자기소개",
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.disabled,
                )
            },
            placeholder = "입력 불가",
            enabled = false,
        )
    }
}

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    CaroTheme {
        Box(
            modifier =
                Modifier
                    .width(360.dp)
                    .heightIn(min = 200.dp)
                    .background(CaroTheme.color.background.primary)
                    .padding(20.dp),
        ) {
            content()
        }
    }
}
