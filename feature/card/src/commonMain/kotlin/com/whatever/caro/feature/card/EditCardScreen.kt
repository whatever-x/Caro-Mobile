package com.whatever.caro.feature.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_button_update
import caromobile.core.designsystem.generated.resources.card_content_description_back
import caromobile.core.designsystem.generated.resources.card_content_description_swap
import caromobile.core.designsystem.generated.resources.card_field_label_back
import caromobile.core.designsystem.generated.resources.card_field_label_front
import caromobile.core.designsystem.generated.resources.card_field_placeholder_back
import caromobile.core.designsystem.generated.resources.card_field_placeholder_front
import caromobile.core.designsystem.generated.resources.card_field_required
import caromobile.core.designsystem.generated.resources.card_title_edit_content
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import caromobile.core.designsystem.generated.resources.ic_switch_16
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.loading.CaroLoadingOverlay
import com.whatever.caro.feature.card.mvi.EditCardIntent
import com.whatever.caro.feature.card.mvi.EditCardState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PageHorizontalPadding = 28.dp
private val CtaButtonHeight = 56.dp
private val TopBarIconSize = 24.dp
private val SwapButtonSize = 32.dp
private val SmallIconSize = 16.dp
private val HairlineThickness = 1.dp

@Composable
internal fun EditCardScreen(
    state: EditCardState,
    onIntent: (EditCardIntent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(CaroTheme.color.background.primary),
        ) {
            CaroTopBar(
                modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl),
                leadingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier =
                                Modifier
                                    .size(TopBarIconSize)
                                    .clickable { onIntent(EditCardIntent.ClickBack) },
                            painter = painterResource(Res.drawable.ic_chevron_left_24),
                            contentDescription = stringResource(Res.string.card_content_description_back),
                            tint = CaroTheme.color.icon.brand,
                        )
                        Text(
                            text = stringResource(Res.string.card_title_edit_content),
                            style = CaroTheme.typography.heading2,
                            color = CaroTheme.color.text.primary,
                        )
                    }
                },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = CaroTheme.spacing.xl,
                            vertical = CaroTheme.spacing.m,
                        ),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
            ) {
                CaroTextArea(
                    value = state.front,
                    onValueChange = { onIntent(EditCardIntent.UpdateFront(it)) },
                    placeholder = stringResource(Res.string.card_field_placeholder_front),
                    header = { RequiredFieldHeader(label = stringResource(Res.string.card_field_label_front)) },
                    footer = { FieldCounter(count = state.frontCount) },
                )

                SwapButton(onClick = { onIntent(EditCardIntent.ClickSwap) })

                CaroTextArea(
                    value = state.back,
                    onValueChange = { onIntent(EditCardIntent.UpdateBack(it)) },
                    placeholder = stringResource(Res.string.card_field_placeholder_back),
                    header = { RequiredFieldHeader(label = stringResource(Res.string.card_field_label_back)) },
                    footer = { FieldCounter(count = state.backCount) },
                )
            }

            EditBottomBar(
                enabled = state.isSaveEnabled,
                onClick = { onIntent(EditCardIntent.ClickSave) },
            )
        }
        if (state.isSaving) {
            CaroLoadingOverlay()
        }
    }
}

@Composable
private fun RequiredFieldHeader(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = CaroTheme.typography.heading3,
            color = CaroTheme.color.text.primary,
        )
        Text(
            text = stringResource(Res.string.card_field_required),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.dangerous,
        )
    }
}

@Composable
private fun FieldCounter(count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = count,
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SwapButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(SwapButtonSize)
                    .clip(CircleShape)
                    .background(CaroTheme.color.surface.primary)
                    .border(
                        width = HairlineThickness,
                        color = CaroTheme.color.border.secondary,
                        shape = CircleShape,
                    ).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(SmallIconSize),
                painter = painterResource(Res.drawable.ic_switch_16),
                contentDescription = stringResource(Res.string.card_content_description_swap),
                tint = CaroTheme.color.icon.brand,
            )
        }
    }
}

@Composable
private fun EditBottomBar(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (enabled) {
            CaroTheme.color.surface.brand
        } else {
            CaroTheme.color.surface.disabled
        }
    val textColor =
        if (enabled) {
            CaroTheme.color.text.inverse
        } else {
            CaroTheme.color.text.disabled
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .padding(
                    horizontal = PageHorizontalPadding,
                    vertical = CaroTheme.spacing.l,
                ).heightIn(min = CtaButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.xl,
                    vertical = CaroTheme.spacing.l,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.card_button_update),
            style = CaroTheme.typography.label1,
            color = textColor,
        )
    }
}

@Preview
@Composable
private fun EditCardScreenPreview() {
    CaroTheme {
        EditCardScreen(
            state = EditCardState(front = "Run", back = "달리다"),
            onIntent = {},
        )
    }
}
