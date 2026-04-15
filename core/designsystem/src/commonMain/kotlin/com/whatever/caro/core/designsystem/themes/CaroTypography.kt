package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class CaroTypography(
    val watermark: TextStyle,
    val display: TextStyle,
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val body1: TextStyle,
    val body2: Body2Style,
    val label1: Label1Style,
    val caption1: TextStyle,
    val caption2: Caption2Style,
    val robotoLabel1: TextStyle,
) {
    interface BoldStyle {
        val bold: TextStyle
    }

    interface ReadingStyle {
        val reading: TextStyle
    }

    interface RegularStyle {
        val regular: TextStyle
    }

    interface Body2Type :
        RegularStyle,
        ReadingStyle

    interface Label1Type :
        BoldStyle,
        RegularStyle

    interface Caption2Type :
        BoldStyle,
        RegularStyle

    @Immutable
    data class Body2Style(
        override val regular: TextStyle,
        override val reading: TextStyle,
    ) : Body2Type

    @Immutable
    data class Label1Style(
        override val bold: TextStyle,
        override val regular: TextStyle,
    ) : Label1Type

    @Immutable
    data class Caption2Style(
        override val bold: TextStyle,
        override val regular: TextStyle,
    ) : Caption2Type

    companion object {
        fun defaultTypography(
            pretendard: FontFamily,
            roboto: FontFamily,
        ) = CaraTypography(
            watermark =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W900,
                    fontSize = 75.sp,
                    letterSpacing = (-0.09).em,
                ),
            display =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 28.sp,
                ),
            heading1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 22.sp,
                ),
            heading2 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 18.sp,
                ),
            heading3 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 16.sp,
                ),
            body1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp,
                ),
            body2 =
                Body2Style(
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                        ),
                    reading =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        ),
                ),
            label1 =
                Label1Style(
                    bold =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W600,
                            fontSize = 14.sp,
                        ),
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                        ),
                ),
            caption1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 12.sp,
                ),
            caption2 =
                Caption2Style(
                    bold =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W600,
                            fontSize = 10.sp,
                        ),
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 10.sp,
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
