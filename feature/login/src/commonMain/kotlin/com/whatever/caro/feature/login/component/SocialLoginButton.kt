package com.whatever.caro.feature.login.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_logo_google
import caromobile.core.designsystem.generated.resources.login_button_google
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SocialLoginButton(
    modifier: Modifier = Modifier,
    iconRes: DrawableResource,
    contentRes: StringResource,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = 52.dp)
                .padding(vertical = CaroTheme.spacing.xs)
                .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
            )
            Text(
                text = stringResource(resource = contentRes),
                style = CaroTheme.typography.robotoLabel1,
                color = textColor,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0x00FFFFFF)
@Composable
private fun SocialLoginButtonPreview() {
    CaroTheme {
        SocialLoginButton(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF000000),
                        shape = CaroTheme.shape.xxl,
                    ).clip(CaroTheme.shape.xxl)
                    .background(color = Color(0xFFFFFFFF)),
            iconRes = Res.drawable.ic_logo_google,
            contentRes = Res.string.login_button_google,
            textColor = Color(0xFF000000),
            onClick = {},
        )
    }
}
