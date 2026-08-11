package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val basePretendardStyle = TextStyle(letterSpacing = 0.em)

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
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 24.sp,
                ),
            heading1 =
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W700,
                    fontSize = 20.sp,
                ),
            heading2 =
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W600,
                    fontSize = 18.sp,
                ),
            heading3 =
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp,
                ),
            body1 =
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W600,
                    fontSize = 16.sp,
                ),
            body2 =
                Body2Style(
                    semiBold =
                        basePretendardStyle.copy(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W600,
                            fontSize = 14.sp,
                        ),
                    medium =
                        basePretendardStyle.copy(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                        ),
                ),
            label1 =
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W500,
                    fontSize = 14.sp,
                ),
            label2 =
                basePretendardStyle.copy(
                    fontFamily = pretendard,
                    fontWeight = FontWeight.W400,
                    fontSize = 12.sp,
                ),
            caption1 =
                Caption1Style(
                    regular =
                        basePretendardStyle.copy(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 12.sp,
                        ),
                    medium =
                        basePretendardStyle.copy(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W500,
                            fontSize = 12.sp,
                        ),
                ),
            caption2 =
                Caption2Style(
                    regular =
                        basePretendardStyle.copy(
                            fontFamily = pretendard,
                            fontWeight = FontWeight.W400,
                            fontSize = 10.sp,
                        ),
                    medium =
                        basePretendardStyle.copy(
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
