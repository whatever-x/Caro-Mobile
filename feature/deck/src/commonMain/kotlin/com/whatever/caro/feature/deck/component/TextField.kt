package com.whatever.caro.feature.deck.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_field_required
import com.whatever.caro.core.designsystem.themes.CaroTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RequiredFieldHeader(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = CaroTheme.typography.heading3,
            color = CaroTheme.color.text.primary,
        )
        Text(
            text = stringResource(Res.string.deck_field_required),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.dangerous,
        )
    }
}

@Composable
internal fun FieldCounter(count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = count,
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}
