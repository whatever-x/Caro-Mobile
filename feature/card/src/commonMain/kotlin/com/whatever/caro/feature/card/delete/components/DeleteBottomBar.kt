package com.whatever.caro.feature.card.delete.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_button_cancel
import caromobile.core.designsystem.generated.resources.card_button_delete_selected
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

private val BottomBarVerticalPadding = 16.dp
private val DeleteButtonHeight = 52.dp
private val CancelButtonWidth = 70.dp

@Composable
internal fun DeleteBottomBar(
    enabled: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CaroTheme.color.background.primary)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                    horizontal = CaroTheme.spacing.l,
                    vertical = BottomBarVerticalPadding,
                ),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeleteSelectedButton(
            modifier = Modifier.weight(1f),
            enabled = enabled,
            onClick = onDelete,
        )
        CancelButton(
            modifier = Modifier.width(CancelButtonWidth),
            onClick = onCancel,
        )
    }
}

@Composable
internal fun DeleteSelectedButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (enabled) {
            CaroTheme.color.surface.brand
        } else {
            CaroTheme.color.surface.disabled
        }
    val textColor =
        if (enabled) {
            CaroTheme.color.text.inverse
        } else {
            CaroTheme.color.text.disabled
        }

    Box(
        modifier =
            modifier
                .height(DeleteButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.card_button_delete_selected),
            style = CaroTheme.typography.caption1.regular,
            color = textColor,
        )
    }
}

@Composable
internal fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(DeleteButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(CaroTheme.color.surface.inverse)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.card_button_cancel),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.inverse,
        )
    }
}
