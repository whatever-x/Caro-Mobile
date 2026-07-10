package com.whatever.caro.feature.deck.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import caromobile.core.designsystem.generated.resources.deck_button_edit
import caromobile.core.designsystem.generated.resources.deck_content_description_back
import caromobile.core.designsystem.generated.resources.deck_content_description_clear
import caromobile.core.designsystem.generated.resources.deck_edit_title
import caromobile.core.designsystem.generated.resources.deck_field_label_description
import caromobile.core.designsystem.generated.resources.deck_field_label_name
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_description
import caromobile.core.designsystem.generated.resources.deck_field_placeholder_name
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.component.CtaButton
import com.whatever.caro.feature.deck.component.DeckTipSection
import com.whatever.caro.feature.deck.component.FieldCounter
import com.whatever.caro.feature.deck.component.PageHorizontalPadding
import com.whatever.caro.feature.deck.component.RequiredFieldHeader
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
                    text = stringResource(Res.string.deck_edit_title),
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
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)
        ) {
            CaroTextField(
                value = state.name,
                onValueChange = { onIntent(CreateDeckIntent.UpdateName(it)) },
                placeholder = stringResource(Res.string.deck_field_placeholder_name),
                header = { RequiredFieldHeader(label = stringResource(Res.string.deck_field_label_name)) },
                footer = { FieldCounter(count = state.nameCount) },
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
            CaroTextArea(
                value = state.description,
                onValueChange = { onIntent(CreateDeckIntent.UpdateDescription(it)) },
                placeholder = stringResource(Res.string.deck_field_placeholder_description),
                header = { RequiredFieldHeader(label = stringResource(Res.string.deck_field_label_description)) },
                footer = { FieldCounter(count = state.descriptionCount) },
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
            text = stringResource(Res.string.deck_button_edit)
        )
    }
}
