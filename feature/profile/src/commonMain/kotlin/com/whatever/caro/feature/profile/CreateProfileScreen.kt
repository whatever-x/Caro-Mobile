package com.whatever.caro.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_renew_16
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import caromobile.core.designsystem.generated.resources.profile_button_create
import caromobile.core.designsystem.generated.resources.profile_button_random
import caromobile.core.designsystem.generated.resources.profile_content_description_back
import caromobile.core.designsystem.generated.resources.profile_content_description_clear
import caromobile.core.designsystem.generated.resources.profile_field_label_nickname
import caromobile.core.designsystem.generated.resources.profile_field_placeholder_random_nickname
import caromobile.core.designsystem.generated.resources.profile_field_required
import caromobile.core.designsystem.generated.resources.profile_field_rule_nickname
import caromobile.core.designsystem.generated.resources.profile_title_create
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.mvi.CreateProfileState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PageHorizontalPadding = 28.dp
private val CtaButtonHeight = 56.dp
private val HeaderMinHeight = 17.dp
private const val DISABLED_ALPHA = 0.4f

@Composable
internal fun CreateProfileScreen(
    state: CreateProfileState,
    onIntent: (CreateProfileIntent) -> Unit,
) {
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
                            .clickable { onIntent(CreateProfileIntent.ClickBack) },
                    painter = painterResource(Res.drawable.ic_arrow_left_24),
                    contentDescription = stringResource(Res.string.profile_content_description_back),
                    tint = CaroTheme.color.icon.secondary,
                )
            },
            centerContent = {
                Text(
                    text = stringResource(Res.string.profile_title_create),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.primary,
                )
            },
        )

        NicknameField(
            state = state,
            onIntent = onIntent,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.m,
                    ),
        )

        Spacer(modifier = Modifier.weight(1f))

        CtaButton(
            enabled = state.isConfirmEnabled,
            onClick = { onIntent(CreateProfileIntent.ClickConfirm) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(
                        horizontal = PageHorizontalPadding,
                        vertical = CaroTheme.spacing.l,
                    ),
        )
    }
}

@Composable
private fun NicknameField(
    state: CreateProfileState,
    onIntent: (CreateProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CaroTextField(
        modifier = modifier,
        value = state.nickname,
        onValueChange = { onIntent(CreateProfileIntent.UpdateNickname(it)) },
        placeholder = stringResource(Res.string.profile_field_placeholder_random_nickname),
        header = {
            NicknameHeader(
                onRefreshClick = { onIntent(CreateProfileIntent.ClickRefresh) },
            )
        },
        footer = {
            NicknameFooter(characterCount = state.characterCount)
        },
        trailingIcon =
            if (state.nickname.isNotEmpty()) {
                {
                    Icon(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clickable {
                                    onIntent(CreateProfileIntent.UpdateNickname(""))
                                },
                        painter = painterResource(Res.drawable.ic_x_circle_24),
                        contentDescription = stringResource(Res.string.profile_content_description_clear),
                        tint = CaroTheme.color.icon.tertiary,
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun NicknameHeader(onRefreshClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = HeaderMinHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.profile_field_label_nickname),
                style = CaroTheme.typography.heading3,
                color = CaroTheme.color.text.primary,
            )
            Text(
                text = stringResource(Res.string.profile_field_required),
                style = CaroTheme.typography.label1.bold,
                color = CaroTheme.color.text.accent,
            )
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CaroTheme.shape.xxl)
                    .clickable(onClick = onRefreshClick),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(Res.drawable.ic_renew_16),
                contentDescription = null,
                tint = CaroTheme.color.icon.brand,
            )
            Text(
                text = stringResource(Res.string.profile_button_random),
                style = CaroTheme.typography.caption1,
                color = CaroTheme.color.text.brand,
            )
        }
    }
}

@Composable
private fun NicknameFooter(characterCount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.profile_field_rule_nickname),
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.secondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = characterCount,
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
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
            text = stringResource(Res.string.profile_button_create),
            style = CaroTheme.typography.label1.regular,
            color = CaroTheme.color.text.inverse,
        )
    }
}
