package com.whatever.caro.feature.deck.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_button_create
import caromobile.core.designsystem.generated.resources.deck_content_description_back
import caromobile.core.designsystem.generated.resources.deck_content_description_clear
import caromobile.core.designsystem.generated.resources.deck_field_label_description
import caromobile.core.designsystem.generated.resources.deck_field_label_name
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_description
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_name
import caromobile.core.designsystem.generated.resources.deck_tip_max_cards
import caromobile.core.designsystem.generated.resources.deck_title_create
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import com.whatever.caro.core.designsystem.components.CaroCtaButton
import com.whatever.caro.core.designsystem.components.CaroFieldCounter
import com.whatever.caro.core.designsystem.components.CaroRequiredFieldHeader
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.components.CaroTipSection
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.foundation.CaroPageHorizontalPadding
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.loading.CaroLoadingOverlayBox
import com.whatever.caro.feature.deck.create.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.create.mvi.CreateDeckState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CreateDeckScreen(
    state: CreateDeckState,
    onIntent: (CreateDeckIntent) -> Unit,
) {
    val nameFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val tipMaxCards = stringResource(Res.string.deck_tip_max_cards)
    val tipLines = remember(tipMaxCards) { listOf(tipMaxCards) }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    CaroLoadingOverlayBox(isLoading = state.isLoading) {
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
                                .padding(start = TopBarLeadingIconStartPadding)
                                .size(24.dp)
                                .noRippleClickable { onIntent(CreateDeckIntent.ClickBack) },
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
                CaroTextField(
                    value = state.name,
                    onValueChange = { onIntent(CreateDeckIntent.UpdateName(it)) },
                    placeholder = stringResource(Res.string.deck_field_placeholder_name),
                    header = { CaroRequiredFieldHeader(label = stringResource(Res.string.deck_field_label_name)) },
                    footer = { CaroFieldCounter(count = state.nameCount) },
                    focusRequester = nameFocusRequester,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { descriptionFocusRequester.requestFocus() }),
                    trailingIcon =
                        if (state.name.isNotEmpty()) {
                            {
                                Icon(
                                    modifier =
                                        Modifier
                                            .size(24.dp)
                                            .noRippleClickable { onIntent(CreateDeckIntent.UpdateName("")) },
                                    painter = painterResource(Res.drawable.ic_x_circle_24),
                                    contentDescription = stringResource(Res.string.deck_content_description_clear),
                                    tint = CaroTheme.color.icon.tertiary,
                                )
                            }
                        } else {
                            null
                        },
                )
                CaroTextArea(
                    value = state.description,
                    onValueChange = { onIntent(CreateDeckIntent.UpdateDescription(it)) },
                    placeholder = stringResource(Res.string.deck_field_placeholder_description),
                    header = { CaroRequiredFieldHeader(label = stringResource(Res.string.deck_field_label_description)) },
                    footer = { CaroFieldCounter(count = state.descriptionCount) },
                    focusRequester = descriptionFocusRequester,
                )
                CaroTipSection(lines = tipLines)
            }

            CaroCtaButton(
                text = stringResource(Res.string.deck_button_create),
                onClick = { onIntent(CreateDeckIntent.ClickConfirm) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                        .padding(
                            horizontal = CaroPageHorizontalPadding,
                            vertical = CaroTheme.spacing.l,
                        ),
                enabled = state.isConfirmEnabled,
            )
        }
    }
}

private val TopBarLeadingIconStartPadding = 24.dp
