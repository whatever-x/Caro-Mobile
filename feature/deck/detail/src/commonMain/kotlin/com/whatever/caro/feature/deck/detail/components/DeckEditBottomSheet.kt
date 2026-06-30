package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_deck_delete
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_deck_edit
import caromobile.core.designsystem.generated.resources.ic_edit_24
import caromobile.core.designsystem.generated.resources.ic_trash_24
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.modifier.noRippleClickable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckEditBottomSheet(
    onEditDeck: () -> Unit,
    onDeleteDeck: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape =
            RoundedCornerShape(
                topStart = CaroTheme.spacing.xl2,
                topEnd = CaroTheme.spacing.xl2,
            ),
        containerColor = CaroTheme.color.surface.primary,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(color = CaroTheme.color.surface.primary)
                        .padding(vertical = CaroTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = CaroTheme.color.divider.primary,
                                shape = CaroTheme.shape.xxs,
                            ),
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color = CaroTheme.color.surface.primary)
                    .padding(
                        start = CaroTheme.spacing.l,
                        top = CaroTheme.spacing.l,
                        end = CaroTheme.spacing.l,
                        bottom = CaroTheme.spacing.xl3,
                    ),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            DeckEditBottomSheetItem(
                text = stringResource(Res.string.deck_detail_button_bottom_sheet_deck_edit),
                icon = Res.drawable.ic_edit_24,
                contentColor = CaroTheme.color.text.primary,
                onClick = onEditDeck,
            )

            DeckEditBottomSheetItem(
                text = stringResource(Res.string.deck_detail_button_bottom_sheet_deck_delete),
                icon = Res.drawable.ic_trash_24,
                contentColor = CaroTheme.color.text.warning,
                onClick = onDeleteDeck,
            )
        }
    }
}

@Composable
private fun DeckEditBottomSheetItem(
    text: String,
    icon: DrawableResource,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick)
                .padding(vertical = CaroTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
        )

        Text(
            text = text,
            color = contentColor,
            style = CaroTheme.typography.body1,
        )
    }
}
