package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class CaroTypography(
    val display: TextStyle,
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val body1: TextStyle,
    val body2: Body2Style,
    val label1: TextStyle,
    val label2: TextStyle,
    val caption1: Caption1Style,
    val caption2: Caption2Style,
    val robotoLabel1: TextStyle,
) {
    @Immutable
    data class Body2Style(
        val semiBold: TextStyle,
        val medium: TextStyle,
    )

    @Immutable
    data class Caption1Style(
        val regular: TextStyle,
        val medium: TextStyle,
    )

    @Immutable
    data class Caption2Style(
        val regular: TextStyle,
        val medium: TextStyle,
    )

    companion object {
        fun defaultTypography(
            pretendard: FontFamily,
            roboto: FontFamily,
        ) = CaroTypography(
            display =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 24.sp,
                    letterSpacing = 0.em,
                ),
            heading1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 20.sp,
                    letterSpacing = 0.em,
                ),
            heading2 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W600,
                    fontSize = 18.sp,
                    letterSpacing = 0.em,
                ),
            heading3 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp,
                    letterSpacing = 0.em,
                ),
            body1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W600,
                    fontSize = 16.sp,
                    letterSpacing = 0.em,
                ),
            body2 =
                Body2Style(
                    semiBold =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W600,
                            fontSize = 14.sp,
                            letterSpacing = 0.em,
                        ),
                    medium =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                            letterSpacing = 0.em,
                        ),
                ),
            label1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 14.sp,
                    letterSpacing = 0.em,
                ),
            label2 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W400,
                    fontSize = 12.sp,
                    letterSpacing = 0.em,
                ),
            caption1 =
                Caption1Style(
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 12.sp,
                            letterSpacing = 0.em,
                        ),
                    medium =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 12.sp,
                            letterSpacing = 0.em,
                        ),
                ),
            caption2 =
                Caption2Style(
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 10.sp,
                            letterSpacing = 0.em,
                        ),
                    medium =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 10.sp,
                            letterSpacing = 0.em,
                        ),
                ),
            robotoLabel1 =
                TextStyle(
                    fontFamily = roboto,
                    fontWeight = FontWeight.W500,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
        )
    }
}
