package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
fun CaroFieldCounter(
    count: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
