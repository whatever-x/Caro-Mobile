package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.whatever.caro.core.designsystem.components.CaroCtaButton
import com.whatever.caro.core.designsystem.components.CaroCtaButtonStyle
import com.whatever.caro.core.designsystem.foundation.CaroPageHorizontalPadding
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
internal fun ImportBottomBar(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reason: String? = null,
    style: CaroCtaButtonStyle = CaroCtaButtonStyle.Brand,
    summary: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .padding(
                    horizontal = CaroPageHorizontalPadding,
                    vertical = CaroTheme.spacing.l,
                ),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        summary?.invoke()
        if (reason != null) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                text = reason,
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
        CaroCtaButton(
            text = buttonText,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            style = style,
            disabledReason = reason,
        )
    }
}
