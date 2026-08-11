package com.whatever.caro.feature.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import caromobile.core.designsystem.generated.resources.ic_renew_16
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import caromobile.core.designsystem.generated.resources.profile_button_random
import caromobile.core.designsystem.generated.resources.profile_content_description_clear
import caromobile.core.designsystem.generated.resources.profile_field_label_nickname
import caromobile.core.designsystem.generated.resources.profile_field_placeholder_random_nickname
import caromobile.core.designsystem.generated.resources.profile_field_required
import caromobile.core.designsystem.generated.resources.profile_field_rule_nickname
import com.whatever.caro.core.designsystem.components.CaroTextField
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val HeaderMinHeight = 17.dp

/**
 * 닉네임 입력 필드. create/edit 화면이 공유하는 공통 컴포넌트로, MVI 타입에 의존하지 않고
 * 원시 값과 콜백만 받는다.
 */
@Composable
internal fun NicknameField(
    value: String,
    onValueChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    characterCount: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CaroTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = stringResource(Res.string.profile_field_placeholder_random_nickname),
        header = {
            NicknameHeader(
                enabled = enabled,
                onRefreshClick = onRefreshClick,
            )
        },
        footer = {
            NicknameFooter(characterCount = characterCount)
        },
        trailingIcon =
            if (value.isNotEmpty()) {
                {
                    Icon(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .noRippleClickable(enabled = enabled) { onValueChange("") },
                        painter = painterResource(Res.drawable.ic_x_circle_24),
                        contentDescription = stringResource(Res.string.profile_content_description_clear),
                        tint =
                            if (enabled) {
                                CaroTheme.color.icon.tertiary
                            } else {
                                CaroTheme.color.icon.disabled
                            },
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun NicknameHeader(
    enabled: Boolean,
    onRefreshClick: () -> Unit,
) {
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
                style = CaroTheme.typography.body1,
                color = CaroTheme.color.text.primary,
            )
            Text(
                text = stringResource(Res.string.profile_field_required),
                style = CaroTheme.typography.label2,
                color = CaroTheme.color.text.dangerous,
            )
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CaroTheme.shape.xxl)
                    .noRippleClickable(enabled = enabled, onClick = onRefreshClick),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(Res.drawable.ic_renew_16),
                contentDescription = null,
                tint =
                    if (enabled) {
                        CaroTheme.color.icon.brand
                    } else {
                        CaroTheme.color.icon.disabled
                    },
            )
            Text(
                text = stringResource(Res.string.profile_button_random),
                style = CaroTheme.typography.caption1.regular,
                color =
                    if (enabled) {
                        CaroTheme.color.text.brand
                    } else {
                        CaroTheme.color.text.disabled
                    },
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
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = characterCount,
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}
