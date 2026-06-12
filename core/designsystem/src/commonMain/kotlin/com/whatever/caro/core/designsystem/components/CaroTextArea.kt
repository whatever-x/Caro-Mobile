package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
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

    val textColor =
        if (enabled) {
            CaroTheme.color.text.primary
        } else {
            CaroTheme.color.text.disable
        }
    val surfaceColor =
        if (enabled) {
            CaroTheme.color.surface.primary
        } else {
            CaroTheme.color.surface.secondary
        }
    val borderColor =
        when {
            !enabled -> CaroTheme.color.border.disable
            isFocused -> CaroTheme.color.border.brand
            else -> CaroTheme.color.border.secondary
        }
    val mergedTextStyle = CaroTheme.typography.body1.copy(color = textColor)
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
                    ).padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.l,
                    ),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(INPUT_BOX_HEIGHT.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
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
}

private const val INPUT_BOX_HEIGHT = 90

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
                    style = CaroTheme.typography.label1.bold,
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
                        style = CaroTheme.typography.label1.bold,
                        color = CaroTheme.color.text.primary,
                    )
                    Text(
                        text = "*",
                        style = CaroTheme.typography.label1.bold,
                        color = CaroTheme.color.text.accent,
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
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Text(
                    text = "최대 200자까지 입력할 수 있어요",
                    style = CaroTheme.typography.caption1,
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
                    style = CaroTheme.typography.label1.bold,
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
                        style = CaroTheme.typography.caption1,
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
                        style = CaroTheme.typography.label1.bold,
                        color = CaroTheme.color.text.primary,
                    )
                    Text(
                        text = "*",
                        style = CaroTheme.typography.label1.bold,
                        color = CaroTheme.color.text.accent,
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
                        style = CaroTheme.typography.caption1,
                        color = CaroTheme.color.text.secondary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${value.length}/$max",
                        style = CaroTheme.typography.caption1,
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
                    style = CaroTheme.typography.label1.bold,
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
                        style = CaroTheme.typography.caption1,
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
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Text(
                    text = "필수 입력 항목이에요",
                    style = CaroTheme.typography.caption1,
                    color = CaroTheme.color.text.accent,
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
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.disable,
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
