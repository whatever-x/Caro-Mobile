package com.whatever.caro.feature.card.delete.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_content_description_checkbox
import caromobile.core.designsystem.generated.resources.ic_check_16
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardItemHeight = 81.dp
private val CheckboxSize = 22.dp
private val HairlineThickness = 1.dp
private const val CARD_TEXT_MAX_LINES = 1

@Composable
internal fun DeleteCardListItem(
    item: DeleteCardItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CardItemHeight)
                .clip(CaroTheme.shape.m)
                .background(CaroTheme.color.surface.primary)
                .border(
                    width = HairlineThickness,
                    color = CaroTheme.color.border.primary,
                    shape = CaroTheme.shape.m,
                ).noRippleClickable(onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.xl2,
                    vertical = CaroTheme.spacing.xl,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.card.content.front,
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
                maxLines = CARD_TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.card.content.back,
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.tertiary,
                maxLines = CARD_TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SelectCheckbox(isSelected = item.isSelected)
    }
}

@Composable
internal fun SelectCheckbox(isSelected: Boolean) {
    val backgroundColor =
        if (isSelected) {
            CaroTheme.color.surface.brand
        } else {
            Color.Transparent
        }
    val borderColor =
        if (isSelected) {
            CaroTheme.color.border.brand
        } else {
            CaroTheme.color.border.primary
        }

    Box(
        modifier =
            Modifier
                .size(CheckboxSize)
                .clip(CaroTheme.shape.xs)
                .background(backgroundColor)
                .border(
                    width = HairlineThickness,
                    color = borderColor,
                    shape = CaroTheme.shape.xs,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                modifier = Modifier.size(CaroTheme.spacing.l),
                painter = painterResource(Res.drawable.ic_check_16),
                contentDescription = stringResource(Res.string.card_content_description_checkbox),
                tint = CaroTheme.color.icon.inverse,
            )
        }
    }
}
