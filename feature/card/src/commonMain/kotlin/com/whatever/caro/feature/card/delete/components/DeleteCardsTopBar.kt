package com.whatever.caro.feature.card.delete.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_content_description_back
import caromobile.core.designsystem.generated.resources.card_text_select_all
import caromobile.core.designsystem.generated.resources.card_title_edit
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val TopBarIconSize = 24.dp

@Composable
internal fun DeleteCardsTopBarTitle(onBackClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier =
                Modifier
                    .size(TopBarIconSize)
                    .noRippleClickable(onClick = onBackClick),
            painter = painterResource(Res.drawable.ic_chevron_left_24),
            contentDescription = stringResource(Res.string.card_content_description_back),
            tint = CaroTheme.color.icon.brand,
        )
        Text(
            text = stringResource(Res.string.card_title_edit),
            style = CaroTheme.typography.heading2,
            color = CaroTheme.color.text.primary,
        )
    }
}

@Composable
internal fun SelectAllTextButton(onClick: () -> Unit) {
    Text(
        modifier = Modifier.noRippleClickable(onClick = onClick),
        text = stringResource(Res.string.card_text_select_all),
        style = CaroTheme.typography.body1SemiBold,
        color = CaroTheme.color.text.brand,
    )
}
