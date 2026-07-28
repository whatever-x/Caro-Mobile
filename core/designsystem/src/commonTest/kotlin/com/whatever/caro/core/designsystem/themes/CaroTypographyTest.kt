package com.whatever.caro.core.designsystem.themes

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CaroTypographyTest :
    FunSpec({
        test("typography matches the Figma token contract") {
            val typography =
                CaroTypography.defaultTypography(
                    pretendard = FontFamily.Default,
                    roboto = FontFamily.Default,
                )

            typography.display.fontSize shouldBe 24.sp
            typography.display.fontWeight shouldBe FontWeight.W700
            typography.heading1.fontSize shouldBe 20.sp
            typography.heading1.fontWeight shouldBe FontWeight.W700
            typography.heading2.fontSize shouldBe 18.sp
            typography.heading2.fontWeight shouldBe FontWeight.W600
            typography.heading3.fontSize shouldBe 16.sp
            typography.heading3.fontWeight shouldBe FontWeight.W500
            typography.body1.fontSize shouldBe 16.sp
            typography.body1.fontWeight shouldBe FontWeight.W600
            typography.body2.semiBold.fontSize shouldBe 14.sp
            typography.body2.semiBold.fontWeight shouldBe FontWeight.W600
            typography.body2.medium.fontSize shouldBe 14.sp
            typography.body2.medium.fontWeight shouldBe FontWeight.W500
            typography.label1.fontSize shouldBe 14.sp
            typography.label1.fontWeight shouldBe FontWeight.W500
            typography.label2.fontSize shouldBe 12.sp
            typography.label2.fontWeight shouldBe FontWeight.W400
            typography.caption1.regular.fontSize shouldBe 12.sp
            typography.caption1.regular.fontWeight shouldBe FontWeight.W400
            typography.caption1.medium.fontSize shouldBe 12.sp
            typography.caption1.medium.fontWeight shouldBe FontWeight.W500
            typography.caption2.regular.fontSize shouldBe 10.sp
            typography.caption2.regular.fontWeight shouldBe FontWeight.W400
            typography.caption2.medium.fontSize shouldBe 10.sp
            typography.caption2.medium.fontWeight shouldBe FontWeight.W500
            typography.robotoLabel1.fontSize shouldBe 14.sp
            typography.robotoLabel1.fontWeight shouldBe FontWeight.W500
            typography.robotoLabel1.lineHeight shouldBe 20.sp
        }

        test("Pretendard styles use zero letter spacing and automatic line height") {
            val typography =
                CaroTypography.defaultTypography(
                    pretendard = FontFamily.SansSerif,
                    roboto = FontFamily.Monospace,
                )
            val pretendardStyles =
                listOf(
                    typography.display,
                    typography.heading1,
                    typography.heading2,
                    typography.heading3,
                    typography.body1,
                    typography.body2.semiBold,
                    typography.body2.medium,
                    typography.label1,
                    typography.label2,
                    typography.caption1.regular,
                    typography.caption1.medium,
                    typography.caption2.regular,
                    typography.caption2.medium,
                )

            pretendardStyles.forEach { style ->
                style.fontFamily shouldBe FontFamily.SansSerif
                style.letterSpacing shouldBe 0.em
                style.lineHeight shouldBe TextUnit.Unspecified
            }
            typography.robotoLabel1.fontFamily shouldBe FontFamily.Monospace
        }
    })
