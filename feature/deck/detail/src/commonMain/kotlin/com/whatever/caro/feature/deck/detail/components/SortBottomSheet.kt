package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_created
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_last_frequency
import caromobile.core.designsystem.generated.resources.deck_detail_button_bottom_sheet_last_reviewed
import caromobile.core.designsystem.generated.resources.ic_check_16
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortBottomSheet(
    selectedSortOption: DeckDetailSortOption,
    onSortOptionClick: (DeckDetailSortOption) -> Unit,
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
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SortBottomSheetItem(
                    text = stringResource(Res.string.deck_detail_button_bottom_sheet_created),
                    isSelected = selectedSortOption == DeckDetailSortOption.CREATED,
                    onClick = { onSortOptionClick(DeckDetailSortOption.CREATED) },
                )

                SortBottomSheetItem(
                    text = stringResource(Res.string.deck_detail_button_bottom_sheet_last_reviewed),
                    isSelected = selectedSortOption == DeckDetailSortOption.LAST_REVIEWED,
                    onClick = { onSortOptionClick(DeckDetailSortOption.LAST_REVIEWED) },
                )

                SortBottomSheetItem(
                    text = stringResource(Res.string.deck_detail_button_bottom_sheet_last_frequency),
                    isSelected = selectedSortOption == DeckDetailSortOption.FREQUENCY,
                    onClick = { onSortOptionClick(DeckDetailSortOption.FREQUENCY) },
                )
            }
        }
    }
}

@Composable
internal fun SortBottomSheetItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = CaroTheme.spacing.m),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color =
                if (isSelected) {
                    CaroTheme.color.text.accent
                } else {
                    CaroTheme.color.text.secondary
                },
            style = CaroTheme.typography.label1.bold,
        )

        if (isSelected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check_16),
                contentDescription = null,
                tint = CaroTheme.color.icon.accent,
            )
        }
    }
}
