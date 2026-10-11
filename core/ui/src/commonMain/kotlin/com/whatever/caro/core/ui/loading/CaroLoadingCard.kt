package com.whatever.caro.core.ui.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
fun CaroLoadingCard(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Column(
        modifier =
            modifier
                .background(color = CaroTheme.color.surface.primary, shape = CaroTheme.shape.m)
                .padding(CaroTheme.spacing.xl2)
                // 문구가 여러 Text로 나뉘어도 live region이 한 번만 알리도록 합친다.
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenLoadingLottie()
        Spacer(modifier = Modifier.height(CaroTheme.spacing.m))
        Text(
            text = message,
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
        )
        if (detail != null) {
            Spacer(modifier = Modifier.height(CaroTheme.spacing.xs))
            Text(
                text = detail,
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
