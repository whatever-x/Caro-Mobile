package com.whatever.caro.feature.card.delete.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_content_description_checkbox
import com.whatever.caro.core.designsystem.components.CaroCheckbox
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import org.jetbrains.compose.resources.stringResource

private val CardItemHeight = 81.dp
private val HairlineThickness = 1.dp
private const val CARD_TEXT_MAX_LINES = 1

@Composable
internal fun DeleteCardListItem(
    item: DeleteCardItem,
    onClick: () -> Unit,
) {
    val selectedDescription = stringResource(Res.string.card_content_description_checkbox)

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
                .semantics {
                    if (item.isSelected) {
                        contentDescription = selectedDescription
                    }
                }.padding(
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
        CaroCheckbox(state = if (item.isSelected) ToggleableState.On else ToggleableState.Off)
    }
}
