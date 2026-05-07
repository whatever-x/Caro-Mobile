package com.whatever.caro.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_logo
import caromobile.core.designsystem.generated.resources.ic_logo_apple
import caromobile.core.designsystem.generated.resources.ic_logo_google
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.ui.image.CaroAsyncImage

@Preview
@Composable
private fun CaroAsyncImagePreview() {
    CaroTheme {
        Box(
            modifier =
                Modifier
                    .background(CaroTheme.color.background.primary)
                    .padding(24.dp),
        ) {
            CaroAsyncImage(
                imageUrl = "https://picsum.photos/200/300",
                contentDescription = null,
                modifier =
                    Modifier
                        .size(120.dp)
                        .clip(shape = CaroTheme.shape.xl),
                placeholder = Res.drawable.ic_logo_google,
                error = Res.drawable.ic_logo,
                fallback = Res.drawable.ic_logo_apple,
                contentScale = ContentScale.Fit,
            )
        }
    }
}
