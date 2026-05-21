package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_renew_16
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun CaroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
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
            TextFieldBox(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                trailingIcon = trailingIcon,
                interactionSource = interactionSource,
            )

            footer?.invoke()
        }
    }
}

@Composable
private fun TextFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    enabled: Boolean,
    readOnly: Boolean,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource,
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
            !enabled -> CaroTheme.color.border.secondary
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = mergedTextStyle,
                    singleLine = singleLine,
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

            if (trailingIcon != null) {
                Box(modifier = Modifier.size(24.dp)) {
                    trailingIcon()
                }
            }
        }
    }
}

@Preview
@Composable
private fun CaroTextFieldDefaultPreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            placeholder = "Placeholder",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldWithTitlePreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "닉네임",
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            placeholder = "닉네임을 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldRequiredPreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            header = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "닉네임",
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
            placeholder = "닉네임을 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldWithCtaPreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            header = {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 17.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "닉네임",
                        style = CaroTheme.typography.label1.bold,
                        color = CaroTheme.color.text.primary,
                    )
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .clip(CaroTheme.shape.xxl)
                                .clickable(onClick = {}),
                        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_renew_16),
                            contentDescription = null,
                            tint = CaroTheme.color.icon.brand,
                        )
                        Text(
                            text = "랜덤 생성",
                            style = CaroTheme.typography.caption1,
                            color = CaroTheme.color.text.brand,
                        )
                    }
                }
            },
            placeholder = "닉네임을 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldWithCaptionPreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "닉네임",
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Text(
                    text = "2자 이상 10자 이하로 입력해주세요",
                    style = CaroTheme.typography.caption1,
                    color = CaroTheme.color.text.secondary,
                )
            },
            placeholder = "닉네임을 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldWithCounterPreview() {
    PreviewContainer {
        val value = "카로"
        val max = 10
        CaroTextField(
            value = value,
            onValueChange = {},
            header = {
                Text(
                    text = "닉네임",
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
            placeholder = "닉네임을 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldFilledPreview() {
    PreviewContainer {
        val value = "카로"
        val max = 10
        CaroTextField(
            value = value,
            onValueChange = {},
            header = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "닉네임",
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
                        text = "2자 이상 10자 이하로 입력해주세요",
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
            placeholder = "닉네임을 입력해주세요",
            trailingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_x_circle_24),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.tertiary,
                )
            },
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldCustomFooterPreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "닉네임",
                    style = CaroTheme.typography.label1.bold,
                    color = CaroTheme.color.text.primary,
                )
            },
            footer = {
                Text(
                    text = "이미 사용 중인 닉네임이에요",
                    style = CaroTheme.typography.caption1,
                    color = CaroTheme.color.text.accent,
                )
            },
            placeholder = "닉네임을 입력해주세요",
        )
    }
}

@Preview
@Composable
private fun CaroTextFieldDisabledPreview() {
    PreviewContainer {
        CaroTextField(
            value = "",
            onValueChange = {},
            header = {
                Text(
                    text = "닉네임",
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
