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
    val body1: Body1Style,
    val body2: Body2Style,
    val body3: TextStyle,
    val body4: Body4Style,
    val label1: Label1Style,
    val label2: Label2Style,
    val caption1: Caption1Style,
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

    interface MediumStyle {
        val medium: TextStyle
    }

    @Immutable
    data class Body1Style(
        val semiBold: TextStyle,
        override val regular: TextStyle,
    ) : RegularStyle

    @Immutable
    data class Body2Style(
        val semiBold: TextStyle,
        override val regular: TextStyle,
        override val reading: TextStyle,
        override val medium: TextStyle,
    ) : ReadingStyle,
        RegularStyle,
        MediumStyle

    @Immutable
    data class Body4Style(
        override val regular: TextStyle,
    ) : RegularStyle

    @Immutable
    data class Label1Style(
        override val bold: TextStyle,
        override val regular: TextStyle,
        override val medium: TextStyle,
    ) : BoldStyle,
        RegularStyle,
        MediumStyle

    @Immutable
    data class Label2Style(
        override val regular: TextStyle,
    ) : RegularStyle

    @Immutable
    data class Caption1Style(
        override val regular: TextStyle,
        override val medium: TextStyle,
    ) : RegularStyle,
        MediumStyle

    @Immutable
    data class Caption2Style(
        override val bold: TextStyle,
        override val medium: TextStyle,
        override val regular: TextStyle,
    ) : BoldStyle,
        RegularStyle,
        MediumStyle

    companion object {
        fun defaultTypography(
            pretendard: FontFamily,
            roboto: FontFamily,
        ) = CaroTypography(
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
                    fontSize = 24.sp,
                ),
            heading1 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 20.sp,
                ),
            heading2 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W600,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                ),
            heading3 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W600,
                    fontSize = 16.sp,
                ),
            body1 =
                Body1Style(
                    semiBold =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W600,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 16.sp,
                        ),
                ),
            body2 =
                Body2Style(
                    semiBold =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W600,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 14.sp,
                        ),
                    reading =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        ),
                    medium =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                        ),
                ),
            body3 =
                TextStyle(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            body4 =
                Body4Style(
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
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
                            lineHeight = 20.sp,
                        ),
                    medium =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                        ),
                ),
            label2 =
                Label2Style(
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 12.sp,
                        ),
                ),
            caption1 =
                Caption1Style(
                    regular =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 12.sp,
                        ),
                    medium =
                        TextStyle(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 12.sp,
                        ),
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
                            fontWeight = FontWeight.W400,
                            fontSize = 10.sp,
                        ),
                    medium =
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
