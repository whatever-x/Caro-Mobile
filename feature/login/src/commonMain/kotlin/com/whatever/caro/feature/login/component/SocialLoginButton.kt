package com.whatever.caro.feature.login.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.whatever.caro.core.designsystem.themes.CaroTheme
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
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(vertical = CaroTheme.spacing.xs)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
            )
            Text(
                text = stringResource(resource = contentRes),
                style = CaroTheme.typography.robotoLabel1,
                color = textColor
            )
        }
    }
}


