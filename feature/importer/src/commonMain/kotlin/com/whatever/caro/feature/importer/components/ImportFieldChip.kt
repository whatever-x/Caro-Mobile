package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val ChipBorderWidth = 1.dp
private val ChipVerticalPadding = 6.dp

/** 고르지 않아도 테두리 두께가 같아야 고를 때 칩 크기와 글자 위치가 흔들리지 않는다. */
@Composable
internal fun ImportFieldChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: String? = null,
) {
    val shape = CaroTheme.shape.xxl
    val containerColor = if (isSelected) CaroTheme.color.surface.brand else CaroTheme.color.surface.primary
    val borderColor = if (isSelected) CaroTheme.color.surface.brand else CaroTheme.color.border.secondary

    Row(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .border(ChipBorderWidth, borderColor, shape)
                .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
                .padding(horizontal = CaroTheme.spacing.m, vertical = ChipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = if (isSelected) CaroTheme.typography.body2.semiBold else CaroTheme.typography.body2.medium,
            color = if (isSelected) CaroTheme.color.text.inverse else CaroTheme.color.text.secondary,
        )
        if (count != null) {
            Text(
                text = count,
                style = CaroTheme.typography.body2.semiBold,
                color = if (isSelected) CaroTheme.color.text.inverse else CaroTheme.color.text.primary,
            )
        }
    }
}
