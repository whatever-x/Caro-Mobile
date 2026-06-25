package com.whatever.caro.feature.profile.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import caromobile.core.designsystem.generated.resources.profile_button_done
import caromobile.core.designsystem.generated.resources.profile_content_description_back
import caromobile.core.designsystem.generated.resources.profile_title_create
import caromobile.core.designsystem.generated.resources.profile_title_edit
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.components.NicknameField
import com.whatever.caro.feature.profile.components.ProfileCtaButton
import com.whatever.caro.feature.profile.edit.mvi.EditProfileIntent
import com.whatever.caro.feature.profile.edit.mvi.EditProfileState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditProfileScreen(
    state: EditProfileState,
    onIntent: (EditProfileIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary)
                .padding(horizontal = CaroTheme.spacing.xl2),
    ) {
        CaroTopBar(
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                Icon(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onIntent(EditProfileIntent.ClickBack) },
                    painter = painterResource(Res.drawable.ic_arrow_left_24),
                    contentDescription = stringResource(Res.string.profile_content_description_back),
                    tint = CaroTheme.color.icon.secondary,
                )
            },
            centerContent = {
                Text(
                    text = stringResource(Res.string.profile_title_edit),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.primary,
                )
            },
        )

        NicknameField(
            value = state.nickname,
            onValueChange = { onIntent(EditProfileIntent.UpdateNickname(it)) },
            onRefreshClick = { onIntent(EditProfileIntent.ClickRefresh) },
            characterCount = state.characterCount,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = CaroTheme.spacing.m,),
        )

        Spacer(modifier = Modifier.weight(1f))

        ProfileCtaButton(
            text = stringResource(Res.string.profile_button_done),
            enabled = state.isConfirmEnabled,
            onClick = { onIntent(EditProfileIntent.ClickConfirm) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(vertical = CaroTheme.spacing.l),
        )
    }
}
