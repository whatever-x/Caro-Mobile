package com.whatever.caro.feature.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_file_text_24
import caromobile.core.designsystem.generated.resources.ic_lock_24
import caromobile.core.designsystem.generated.resources.ic_log_out_24
import caromobile.core.designsystem.generated.resources.ic_logo_apple_small
import caromobile.core.designsystem.generated.resources.ic_logo_google_small
import caromobile.core.designsystem.generated.resources.ic_tool_24
import caromobile.core.designsystem.generated.resources.ic_x_circle_24
import caromobile.core.designsystem.generated.resources.setting_description_app_version
import caromobile.core.designsystem.generated.resources.setting_menu_delete_account
import caromobile.core.designsystem.generated.resources.setting_menu_logout
import caromobile.core.designsystem.generated.resources.setting_menu_privacy_policy
import caromobile.core.designsystem.generated.resources.setting_menu_report_bug
import caromobile.core.designsystem.generated.resources.setting_menu_terms_of_service
import caromobile.core.designsystem.generated.resources.setting_title
import caromobile.core.designsystem.generated.resources.setting_userinfo_nickname_change
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.ui.modifier.noRippleClickable
import com.whatever.caro.feature.setting.component.MenuSection
import com.whatever.caro.feature.setting.model.AppConfig
import com.whatever.caro.feature.setting.model.SettingMenu
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingState
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private val firstMenu =
    persistentListOf(
        SettingMenu.Menu(
            content = Res.string.setting_menu_terms_of_service,
            leadingIcon = Res.drawable.ic_file_text_24,
            action = SettingIntent.ClickTermsOfService,
        ),
        SettingMenu.Divider,
        SettingMenu.Menu(
            content = Res.string.setting_menu_privacy_policy,
            leadingIcon = Res.drawable.ic_lock_24,
            action = SettingIntent.ClickPrivacyPolicy,
        ),
        SettingMenu.Divider,
        SettingMenu.Menu(
            content = Res.string.setting_menu_report_bug,
            leadingIcon = Res.drawable.ic_tool_24,
            action = SettingIntent.ClickReportBug,
        ),
    )

private val secondMenu =
    persistentListOf(
        SettingMenu.Menu(
            content = Res.string.setting_menu_logout,
            leadingIcon = Res.drawable.ic_log_out_24,
            action = SettingIntent.ClickLogOut,
        ),
        SettingMenu.Divider,
        SettingMenu.Menu(
            content = Res.string.setting_menu_delete_account,
            leadingIcon = Res.drawable.ic_x_circle_24,
            action = SettingIntent.ClickDeleteAccount,
            highlight = true,
        ),
    )

@Composable
internal fun SettingScreen(
    state: SettingState,
    onIntent: (SettingIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = CaroTheme.color.background.primary)
                .padding(bottom = CaroTheme.spacing.xl2),
    ) {
        CaroTopBar(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CaroTheme.spacing.xl2),
            leadingContent = {
                Row {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_arrow_left_24),
                        tint = CaroTheme.color.icon.primary,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
                    Text(
                        text = stringResource(Res.string.setting_title),
                        style = CaroTheme.typography.heading2,
                        color = CaroTheme.color.text.primary,
                    )
                }
            },
        )
        UserInfo(
            modifier =
                Modifier
                    .background(CaroTheme.color.surface.primary),
            nickname = state.nickname,
            emailAddress = state.emailAddress,
            socialLoginType = state.socialLoginType,
            onNicknameChangeClick = { onIntent(SettingIntent.ClickNicknameChange) },
        )
        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
        MenuSection(
            modifier = Modifier.fillMaxWidth(),
            items = firstMenu,
            onClickMenu = onIntent,
        )
        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
        MenuSection(
            modifier = Modifier.fillMaxWidth(),
            items = secondMenu,
            onClickMenu = onIntent,
        )
        Spacer(modifier = Modifier.weight(1f))
        // FIXME: 임시 디자인 적용
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text =
                    stringResource(
                        Res.string.setting_description_app_version,
                        AppConfig.appVersion,
                    ),
                style = CaroTheme.typography.caption2,
                color = CaroTheme.color.text.tertiary,
            )
        }
    }
}

private val ProfileImageSize = 32.dp

@Composable
private fun UserInfo(
    modifier: Modifier = Modifier,
    nickname: String,
    emailAddress: String,
    socialLoginType: SocialLoginType?,
    onNicknameChangeClick: () -> Unit,
) {
    if (socialLoginType == null) return
    val socialIcon =
        when (socialLoginType) {
            SocialLoginType.GOOGLE -> Res.drawable.ic_logo_google_small
            SocialLoginType.APPLE -> Res.drawable.ic_logo_apple_small
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = CaroTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier =
                Modifier.size(size = ProfileImageSize),
            painter = painterResource(socialIcon),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.m))
        Column(
            modifier = Modifier.weight(weight = 1f),
        ) {
            Text(
                text = nickname,
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = emailAddress,
                style = CaroTheme.typography.body4,
                color = CaroTheme.color.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
        Box(
            modifier =
                Modifier
                    .background(color = CaroTheme.color.surface.secondary, shape = CaroTheme.shape.xxl)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .noRippleClickable(onNicknameChangeClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.setting_userinfo_nickname_change),
                style = CaroTheme.typography.caption1,
                color = CaroTheme.color.text.brand,
            )
        }
    }
}

@Preview
@Composable
private fun SettingScreenPreview() {
    CaroTheme {
        SettingScreen(
            state =
                SettingState(
                    nickname = "승우",
                    emailAddress = "rsw1452@gmail.com",
                    socialLoginType = SocialLoginType.GOOGLE,
                ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun UserInfoPreview() {
    CaroTheme {
        UserInfo(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFFFFFFF)),
            nickname = "승우",
            emailAddress = "rsw1452@gmail.com",
            socialLoginType = SocialLoginType.GOOGLE,
            onNicknameChangeClick = {},
        )
    }
}
