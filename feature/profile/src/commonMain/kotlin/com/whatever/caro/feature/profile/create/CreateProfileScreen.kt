package com.whatever.caro.feature.profile.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_body
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_button_leave
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_button_stay
import caromobile.core.designsystem.generated.resources.profile_cancel_dialog_title
import caromobile.core.designsystem.generated.resources.profile_content_description_back
import caromobile.core.designsystem.generated.resources.profile_title_create
import caromobile.feature.profile.generated.resources.create_profile_birth_day
import caromobile.feature.profile.generated.resources.create_profile_birth_description
import caromobile.feature.profile.generated.resources.create_profile_birth_heading
import caromobile.feature.profile.generated.resources.create_profile_birth_invalid
import caromobile.feature.profile.generated.resources.create_profile_birth_label
import caromobile.feature.profile.generated.resources.create_profile_birth_month
import caromobile.feature.profile.generated.resources.create_profile_birth_year
import caromobile.feature.profile.generated.resources.create_profile_finish
import caromobile.feature.profile.generated.resources.create_profile_next
import caromobile.feature.profile.generated.resources.create_profile_nickname_check_failed
import caromobile.feature.profile.generated.resources.create_profile_nickname_checking
import caromobile.feature.profile.generated.resources.create_profile_nickname_description
import caromobile.feature.profile.generated.resources.create_profile_nickname_duplicate
import caromobile.feature.profile.generated.resources.create_profile_nickname_heading
import caromobile.feature.profile.generated.resources.create_profile_nickname_retry
import caromobile.feature.profile.generated.resources.create_profile_registration_failed
import caromobile.feature.profile.generated.resources.create_profile_relogin
import caromobile.feature.profile.generated.resources.create_profile_relogin_body
import caromobile.feature.profile.generated.resources.create_profile_relogin_cancel
import caromobile.feature.profile.generated.resources.create_profile_relogin_failed
import caromobile.feature.profile.generated.resources.create_profile_relogin_title
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.components.NicknameField
import com.whatever.caro.feature.profile.components.ProfileCtaButton
import com.whatever.caro.feature.profile.create.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState
import com.whatever.caro.feature.profile.create.mvi.CreateProfileStep
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import caromobile.feature.profile.generated.resources.Res as ProfileRes

private val PageHorizontalPadding = 24.dp

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

        val isNickname = state.step == CreateProfileStep.Nickname
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PageHorizontalPadding)) {
            Spacer(Modifier.height(32.dp))
            Text(
                text =
                    stringResource(
                        if (isNickname) {
                            ProfileRes.string.create_profile_nickname_heading
                        } else {
                            ProfileRes.string.create_profile_birth_heading
                        },
                    ),
                style = CaroTheme.typography.heading1,
                color = CaroTheme.color.text.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text =
                    stringResource(
                        if (isNickname) {
                            ProfileRes.string.create_profile_nickname_description
                        } else {
                            ProfileRes.string.create_profile_birth_description
                        },
                    ),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(Modifier.height(36.dp))
            if (isNickname) {
                NicknameField(
                    value = state.nickname,
                    onValueChange = { onIntent(CreateProfileIntent.UpdateNickname(it)) },
                    onRefreshClick = { onIntent(CreateProfileIntent.ClickRefresh) },
                    characterCount = state.characterCount,
                    enabled = !state.isLoading,
                )
                if (state.validationResult == NicknameValidationResult.Checking ||
                    state.validationResult == NicknameValidationResult.Duplicate
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text =
                            stringResource(
                                if (state.validationResult == NicknameValidationResult.Checking) {
                                    ProfileRes.string.create_profile_nickname_checking
                                } else {
                                    ProfileRes.string.create_profile_nickname_duplicate
                                },
                            ),
                        style = CaroTheme.typography.caption1.regular,
                        color =
                            if (state.validationResult == NicknameValidationResult.Duplicate) {
                                CaroTheme.color.text.dangerous
                            } else {
                                CaroTheme.color.text.secondary
                            },
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            } else {
                Text(
                    stringResource(ProfileRes.string.create_profile_birth_label),
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.primary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BirthDateField(
                        state.year,
                        stringResource(ProfileRes.string.create_profile_birth_year),
                        !state.isLoading,
                        Modifier.weight(1.25f),
                    ) { onIntent(CreateProfileIntent.UpdateYear(it)) }
                    BirthDateField(
                        state.month,
                        stringResource(ProfileRes.string.create_profile_birth_month),
                        !state.isLoading,
                        Modifier.weight(1f),
                    ) { onIntent(CreateProfileIntent.UpdateMonth(it)) }
                    BirthDateField(
                        state.day,
                        stringResource(ProfileRes.string.create_profile_birth_day),
                        !state.isLoading,
                        Modifier.weight(1f),
                        isLastField = true,
                    ) { onIntent(CreateProfileIntent.UpdateDay(it)) }
                }
                if (state.isBirthDateInvalid || state.registrationFailed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text =
                            stringResource(
                                if (state.isBirthDateInvalid) {
                                    ProfileRes.string.create_profile_birth_invalid
                                } else if (state.reloginFailed) {
                                    ProfileRes.string.create_profile_relogin_failed
                                } else {
                                    ProfileRes.string.create_profile_registration_failed
                                },
                            ),
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.dangerous,
                    )
                }
                if (state.registrationFailed) {
                    Text(
                        text = stringResource(ProfileRes.string.create_profile_relogin),
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.brand,
                        modifier =
                            Modifier
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !state.isLoading, role = Role.Button) {
                                    onIntent(CreateProfileIntent.ClickRelogin)
                                }.padding(vertical = 16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isNickname && state.validationResult == NicknameValidationResult.CheckFailed) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .background(CaroTheme.color.surface.inverse, CaroTheme.shape.s)
                        .padding(start = CaroTheme.spacing.l)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(ProfileRes.string.create_profile_nickname_check_failed),
                    style = CaroTheme.typography.body2.medium,
                    color = CaroTheme.color.text.inverse,
                )
                Text(
                    text = stringResource(ProfileRes.string.create_profile_nickname_retry),
                    style = CaroTheme.typography.label2,
                    color = CaroTheme.color.text.brand,
                    modifier =
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { onIntent(CreateProfileIntent.RetryNicknameCheck) }
                            .padding(horizontal = CaroTheme.spacing.s, vertical = 16.dp),
                )
            }
        }

        ProfileCtaButton(
            text = stringResource(if (isNickname) ProfileRes.string.create_profile_next else ProfileRes.string.create_profile_finish),
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

        if (state.isReloginDialogVisible) {
            ReloginDialog(
                onConfirm = { onIntent(CreateProfileIntent.ConfirmRelogin) },
                onDismiss = { onIntent(CreateProfileIntent.DismissRelogin) },
            )
        }

        if (state.isCancelDialogVisible) {
            CancelSignUpDialog(
                onLeave = { onIntent(CreateProfileIntent.ConfirmCancel) },
                onStay = { onIntent(CreateProfileIntent.DismissCancelDialog) },
            )
        }
    }
}

@Composable
private fun BirthDateField(
    value: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    isLastField: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    CaroTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { contentDescription = label },
        placeholder = label,
        enabled = enabled,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (isLastField) ImeAction.Done else ImeAction.Next,
            ),
    )
}

@Composable
private fun ReloginDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(ProfileRes.string.create_profile_relogin_title),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
            Text(
                text = stringResource(ProfileRes.string.create_profile_relogin_body),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(modifier = Modifier.size(CaroTheme.spacing.l))
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(ProfileRes.string.create_profile_relogin_cancel),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    onClick = onDismiss,
                )
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(ProfileRes.string.create_profile_relogin),
                    backgroundColor = CaroTheme.color.surface.dangerous,
                    textColor = CaroTheme.color.text.dangerous,
                    onClick = onConfirm,
                )
            }
        },
    )
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
