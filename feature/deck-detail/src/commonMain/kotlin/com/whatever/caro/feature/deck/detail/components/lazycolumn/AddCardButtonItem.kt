package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_add_card
import caromobile.core.designsystem.generated.resources.ic_add_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AddCardButtonItem(
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.xl),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        color = CaroTheme.color.surface.brand,
                        shape = CaroTheme.shape.m,
                    ).clip(shape = CaroTheme.shape.m)
                    .clickable(onClick = onAddCard)
                    .padding(
                        horizontal = CaroTheme.spacing.xl,
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = CaroTheme.spacing.xs,
                    alignment = Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_add_24),
                contentDescription = null,
                tint = CaroTheme.color.icon.inverse,
            )

            Text(
                text = stringResource(Res.string.deck_detail_button_add_card),
                color = CaroTheme.color.text.inverse,
                style = CaroTheme.typography.heading2,
            )
        }
    }
}
