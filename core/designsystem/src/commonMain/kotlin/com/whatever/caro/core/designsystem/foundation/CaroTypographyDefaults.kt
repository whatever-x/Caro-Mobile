package com.whatever.caro.core.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.pretendard_black
import caromobile.core.designsystem.generated.resources.pretendard_bold
import caromobile.core.designsystem.generated.resources.pretendard_medium
import caromobile.core.designsystem.generated.resources.pretendard_regular
import caromobile.core.designsystem.generated.resources.pretendard_semiBold
import org.jetbrains.compose.resources.Font

@Composable
internal fun Pretendard(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.pretendard_black,
            weight = FontWeight.W900,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.pretendard_bold,
            weight = FontWeight.W700,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.pretendard_semiBold,
            weight = FontWeight.W600,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.pretendard_regular,
            weight = FontWeight.W500,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.pretendard_medium,
            weight = FontWeight.W500,
            style = FontStyle.Normal,
        ),
    )

@Composable
internal fun Roboto(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.roboto_medium,
            weight = FontWeight.W500,
            style = FontStyle.Normal,
        ),
    )
