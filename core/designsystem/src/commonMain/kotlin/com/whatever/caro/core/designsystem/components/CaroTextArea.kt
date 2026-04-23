package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val TextAreaInputBoxHeight = 90.dp

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
) {
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
                        color = CaroTheme.color.border.secondary,
                        shape = CaroTheme.shape.m,
                    ).padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.l,
                    ),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(TextAreaInputBoxHeight),
                contentAlignment = Alignment.TopStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxSize(),
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
                    text = "소개",
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            placeholder = "자기 소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaFilledPreview() {
    PreviewContainer {
        CaroTextArea(
            value = "안녕하세요\n저는 카로입니다",
            onValueChange = {},
            header = {
                Text(
                    text = "소개",
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            placeholder = "자기 소개를 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextAreaWithCounterPreview() {
    PreviewContainer {
        val value = "안녕하세요"
        val max = 100
        CaroTextArea(
            value = value,
            onValueChange = {},
            header = {
                Text(
                    text = "소개",
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
            placeholder = "자기 소개를 입력해주세요",
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
                    text = "소개",
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
                    .background(CaroTheme.color.background.primary)
                    .padding(20.dp),
        ) {
            content()
        }
    }
}
