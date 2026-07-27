package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_more_vertical_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun DeckDetailTopBar(
    title: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onEditDeck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = CaroTheme.color.background.brand)
                .padding(horizontal = CaroTheme.spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left_24),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack),
                tint = CaroTheme.color.icon.inverse,
            )

            if (isLoading) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 100.dp, height = 21.dp)
                            .background(
                                color = CaroTheme.color.skeleton.inverse,
                                shape = CaroTheme.shape.xs,
                            ),
                )
            } else {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = CaroTheme.color.text.inverse,
                    style = CaroTheme.typography.heading2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Icon(
            painter = painterResource(Res.drawable.ic_more_vertical_24),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .noRippleClickable(onClick = onEditDeck),
            tint = CaroTheme.color.icon.inverse,
        )
    }
}
