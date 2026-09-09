package com.whatever.caro.feature.profile.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.profile_button_create
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_body
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_button_leave
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_button_stay
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_title
import caromobile.core.designsystem.generated.resources.profile_content_description_back
import caromobile.core.designsystem.generated.resources.profile_title_create
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.components.NicknameField
import com.whatever.caro.feature.profile.components.ProfileCtaButton
import com.whatever.caro.feature.profile.create.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PageHorizontalPadding = 28.dp

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
                            .noRippleClickable(enabled = state.isLoading.not()) {
                                onIntent(CreateProfileIntent.ClickBack)
                            },
                    painter = painterResource(Res.drawable.ic_arrow_left_24),
                    contentDescription = stringResource(Res.string.profile_content_description_back),
                    tint =
                        if (state.isLoading) {
                            CaroTheme.color.icon.disabled
                        } else {
                            CaroTheme.color.icon.secondary
                        },
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
            value = state.nickname,
            onValueChange = { onIntent(CreateProfileIntent.UpdateNickname(it)) },
            onRefreshClick = { onIntent(CreateProfileIntent.ClickRefresh) },
            characterCount = state.characterCount,
            enabled = state.isLoading.not(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.m,
                    ),
        )

        Spacer(modifier = Modifier.weight(1f))

        ProfileCtaButton(
            text = stringResource(Res.string.profile_button_create),
            enabled = state.isConfirmEnabled,
            isLoading = state.isLoading,
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

        if (state.isCancelDialogVisible) {
            CancelSignUpDialog(
                onLeave = { onIntent(CreateProfileIntent.ConfirmCancel) },
                onStay = { onIntent(CreateProfileIntent.DismissCancelDialog) },
            )
        }
    }
}

@Composable
private fun CancelSignUpDialog(
    onLeave: () -> Unit,
    onStay: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onStay,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.profile_cancel_dialog_title),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.profile_cancel_dialog_body),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(modifier = Modifier.size(CaroTheme.spacing.l))
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.profile_cancel_dialog_button_leave),
                    backgroundColor = CaroTheme.color.surface.dangerous,
                    textColor = CaroTheme.color.text.dangerous,
                    onClick = onLeave,
                )
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.profile_cancel_dialog_button_stay),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    onClick = onStay,
                )
            }
        },
    )
}
