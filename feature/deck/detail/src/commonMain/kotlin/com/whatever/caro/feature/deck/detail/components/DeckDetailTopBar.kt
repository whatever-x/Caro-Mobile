package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_label_screen_name
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_more_vertical_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeckDetailTopBar(
    onBack: () -> Unit,
    onEditDeck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = CaroTheme.color.background.primary)
                .padding(horizontal = CaroTheme.spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left_24),
                contentDescription = null,
                modifier =
                    Modifier
                        .clickable(onClick = onBack),
                tint = CaroTheme.color.icon.primary,
            )

            Text(
                text = stringResource(Res.string.deck_detail_label_screen_name),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.heading2,
            )
        }

        Icon(
            painter = painterResource(Res.drawable.ic_more_vertical_24),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(onClick = onEditDeck),
            tint = CaroTheme.color.icon.primary,
        )
    }
}
