package com.whatever.caro.feature.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_right_16
import caromobile.core.designsystem.generated.resources.ic_logo_apple
import caromobile.core.designsystem.generated.resources.login_button_google
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import com.whatever.caro.feature.setting.model.SettingMenu
import com.whatever.caro.feature.setting.mvi.SettingIntent
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun MenuSection(
    modifier: Modifier = Modifier,
    items: ImmutableList<SettingMenu>,
    onClickMenu: (SettingIntent) -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.primary)
                .padding(horizontal = CaroTheme.spacing.xl2),
    ) {
        items.forEach { menu ->
            when (menu) {
                SettingMenu.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color(0xFFEBEBEB),
                    )
                }

                is SettingMenu.Menu -> {
                    MenuItem(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = CaroTheme.spacing.l),
                        leadingIcon = menu.leadingIcon,
                        content = menu.content,
                        highlight = menu.highlight,
                        onClickItem = { onClickMenu(menu.action) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    modifier: Modifier = Modifier,
    leadingIcon: DrawableResource,
    content: StringResource,
    highlight: Boolean,
    onClickItem: () -> Unit,
) {
    val (iconColor, contentColor) =
        if (highlight) {
            CaroTheme.color.icon.warning to CaroTheme.color.text.warning
        } else {
            CaroTheme.color.icon.secondary to CaroTheme.color.text.primary
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .noRippleClickable(onClickItem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(leadingIcon),
            tint = iconColor,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.size(size = 8.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(content),
            style = CaroTheme.typography.heading3,
            color = contentColor,
        )
        Icon(
            imageVector = vectorResource(Res.drawable.ic_arrow_right_16),
            tint = CaroTheme.color.icon.secondary,
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun MenuItemPreview() {
    CaroTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MenuItem(
                leadingIcon = Res.drawable.ic_logo_apple,
                content = Res.string.login_button_google,
                highlight = false,
                onClickItem = {},
            )

            MenuItem(
                leadingIcon = Res.drawable.ic_logo_apple,
                content = Res.string.login_button_google,
                highlight = true,
                onClickItem = {},
            )
        }
    }
}
