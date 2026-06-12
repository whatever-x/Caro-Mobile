package com.whatever.caro.feature.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_button_create
import caromobile.core.designsystem.generated.resources.deck_content_description_back
import caromobile.core.designsystem.generated.resources.deck_content_description_clear
import caromobile.core.designsystem.generated.resources.deck_field_label_description
import caromobile.core.designsystem.generated.resources.deck_field_label_name
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_description
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_name
import caromobile.core.designsystem.generated.resources.deck_field_required
import caromobile.core.designsystem.generated.resources.deck_tip_label
import caromobile.core.designsystem.generated.resources.deck_tip_max_cards
import caromobile.core.designsystem.generated.resources.deck_title_create
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.mvi.CreateDeckState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PageHorizontalPadding = 28.dp
private val CtaButtonHeight = 56.dp
private val TipDotSize = 4.dp
private const val DISABLED_ALPHA = 0.4f

@Composable
internal fun CreateDeckScreen(
    state: CreateDeckState,
    onIntent: (CreateDeckIntent) -> Unit,
) {
    val nameFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
    ) {
        CaroTopBar(
            leadingContent = {
                Icon(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onIntent(CreateDeckIntent.ClickBack) },
                    painter = painterResource(Res.drawable.ic_chevron_left_24),
                    contentDescription = stringResource(Res.string.deck_content_description_back),
                    tint = CaroTheme.color.icon.brand,
                )
            },
            centerContent = {
                Text(
                    text = stringResource(Res.string.deck_title_create),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.primary,
                )
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
            DeckNameField(
                state = state,
                onIntent = onIntent,
                focusRequester = nameFocusRequester,
                onNext = { descriptionFocusRequester.requestFocus() },
            )
            DeckDescriptionField(
                state = state,
                onIntent = onIntent,
                focusRequester = descriptionFocusRequester,
            )
            DeckTipSection()
        }

        CtaButton(
            enabled = state.isConfirmEnabled,
            onClick = { onIntent(CreateDeckIntent.ClickConfirm) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                    .padding(
                        horizontal = PageHorizontalPadding,
                        vertical = CaroTheme.spacing.l,
                    ),
        )
    }
}

@Composable
private fun DeckNameField(
    state: CreateDeckState,
    onIntent: (CreateDeckIntent) -> Unit,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CaroTextField(
        modifier = modifier,
        value = state.name,
        onValueChange = { onIntent(CreateDeckIntent.UpdateName(it)) },
        placeholder = stringResource(Res.string.deck_field_placeholder_name),
        header = { RequiredFieldHeader(label = stringResource(Res.string.deck_field_label_name)) },
        footer = { FieldCounter(count = state.nameCount) },
        focusRequester = focusRequester,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        trailingIcon =
            if (state.name.isNotEmpty()) {
                {
                    Icon(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clickable { onIntent(CreateDeckIntent.UpdateName("")) },
                        painter = painterResource(Res.drawable.ic_x_circle_24),
                        contentDescription = stringResource(Res.string.deck_content_description_clear),
                        tint = CaroTheme.color.icon.tertiary,
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun DeckDescriptionField(
    state: CreateDeckState,
    onIntent: (CreateDeckIntent) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    CaroTextArea(
        modifier = modifier,
        value = state.description,
        onValueChange = { onIntent(CreateDeckIntent.UpdateDescription(it)) },
        placeholder = stringResource(Res.string.deck_field_placeholder_description),
        header = { RequiredFieldHeader(label = stringResource(Res.string.deck_field_label_description)) },
        footer = { FieldCounter(count = state.descriptionCount) },
        focusRequester = focusRequester,
    )
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
            text = stringResource(Res.string.deck_field_required),
            style = CaroTheme.typography.label1.bold,
            color = CaroTheme.color.text.accent,
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
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DeckTipSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.deck_tip_label),
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.secondary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TipDotSize)
                        .clip(CircleShape)
                        .background(CaroTheme.color.text.tertiary),
            )
            Text(
                text = stringResource(Res.string.deck_tip_max_cards),
                style = CaroTheme.typography.caption1,
                color = CaroTheme.color.text.tertiary,
            )
        }
    }
}

@Composable
private fun CtaButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (enabled) {
            CaroTheme.color.surface.brand
        } else {
            CaroTheme.color.surface.brand
                .copy(alpha = DISABLED_ALPHA)
        }

    Box(
        modifier =
            modifier
                .heightIn(min = CtaButtonHeight)
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
            text = stringResource(Res.string.deck_button_create),
            style = CaroTheme.typography.label1.regular,
            color = CaroTheme.color.text.inverse,
        )
    }
}
