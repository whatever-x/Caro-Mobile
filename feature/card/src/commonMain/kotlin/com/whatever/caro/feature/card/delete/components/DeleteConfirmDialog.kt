package com.whatever.caro.feature.card.delete.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_delete_dialog_body
import caromobile.core.designsystem.generated.resources.card_delete_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.card_delete_dialog_button_delete
import caromobile.core.designsystem.generated.resources.card_delete_dialog_title
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

private val DialogButtonHeight = 38.dp

@Composable
internal fun DeleteConfirmDialog(
    selectedCount: Int,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.card_delete_dialog_title, selectedCount),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.heading2,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.card_delete_dialog_body),
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.body3,
            )
            Spacer(modifier = Modifier.size(CaroTheme.spacing.l))
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                DialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.card_delete_dialog_button_delete),
                    backgroundColor = CaroTheme.color.surface.error,
                    textColor = CaroTheme.color.text.accent,
                    onClick = onDelete,
                )
                DialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.card_delete_dialog_button_cancel),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    onClick = onCancel,
                )
            }
        },
    )
}

@Composable
internal fun DialogButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(DialogButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CaroTheme.typography.caption1.regular,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
