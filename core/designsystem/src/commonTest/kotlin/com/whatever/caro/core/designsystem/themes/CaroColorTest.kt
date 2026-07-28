package com.whatever.caro.core.designsystem.themes

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CaroColorTest :
    FunSpec({
        test("semantic colors match the Figma token contract") {
            val color = CaroColor.defaultColor()

            color.text.primary shouldBe Color(0xFF000000)
            color.text.secondary shouldBe Color(0xFF444751)
            color.text.tertiary shouldBe Color(0xFFC4C7CC)
            color.text.disabled shouldBe Color(0xFFC4C7CC)
            color.text.inverse shouldBe Color(0xFFFFFFFF)
            color.text.brand shouldBe Color(0xFF4A6CF7)
            color.text.warning shouldBe Color(0xFFFFC857)
            color.text.error shouldBe Color(0xFFFF7A70)
            color.text.info shouldBe Color(0xFF4A6CF7)
            color.text.review shouldBe Color(0xFF4A6CF7)
            color.text.new shouldBe Color(0xFFFFC857)
            color.text.dangerous shouldBe Color(0xFFFF7A70)
            color.text.accent shouldBe Color(0xFF4A6CF7)
            color.text.rest shouldBe Color(0xFFFF7A70)

            color.icon.primary shouldBe Color(0xFF000000)
            color.icon.secondary shouldBe Color(0xFFC4C7CC)
            color.icon.tertiary shouldBe Color(0xFFA4B5FB)
            color.icon.quaternary shouldBe Color(0xFF6B7280)
            color.icon.disabled shouldBe Color(0xFFC4C7CC)
            color.icon.inverse shouldBe Color(0xFFFFFFFF)
            color.icon.brand shouldBe Color(0xFF4A6CF7)
            color.icon.warning shouldBe Color(0xFFFFC857)
            color.icon.dangerous shouldBe Color(0xFFFF7A70)
            color.icon.accent shouldBe Color(0xFF4A6CF7)

            color.background.primary shouldBe Color(0xFFF8FBFD)
            color.background.brand shouldBe Color(0xFF4A6CF7)

            color.surface.primary shouldBe Color(0xFFFFFFFF)
            color.surface.secondary shouldBe Color(0xFFF6F8FF)
            color.surface.tertiary shouldBe Color(0xFFEDF0FE)
            color.surface.inverse shouldBe Color(0xFF000000)
            color.surface.brand shouldBe Color(0xFF4A6CF7)
            color.surface.new shouldBe Color(0xFFFFF9EE)
            color.surface.error shouldBe Color(0xFFFFF2F1)
            color.surface.dangerous shouldBe Color(0xFFFFF2F1)
            color.surface.disabled shouldBe Color(0xFFEDF0FE)
            color.surface.info shouldBe Color(0xFFEDF0FE)
            color.surface.review shouldBe Color(0xFFEDF0FE)
            color.surface.ready shouldBe Color(0xFFF6F8FF)
            color.surface.progress shouldBe Color(0xFFEDF0FE)
            color.surface.complete shouldBe Color(0xFFF8F8F9)
            color.surface.accent shouldBe Color(0xFFFF7A70)
            color.surface.rest shouldBe Color(0xFFFFF9EE)

            color.border.primary shouldBe Color(0xFFF6F8FF)
            color.border.secondary shouldBe Color(0xFFC4C7CC)
            color.border.tertiary shouldBe Color(0xFFC9D3FD)
            color.border.brand shouldBe Color(0xFF4A6CF7)
            color.border.ready shouldBe Color(0xFFEDF0FE)
            color.border.progress shouldBe Color(0xFFC9D3FD)
            color.border.complete shouldBe Color(0xFFC4C7CC)
            color.border.rest shouldBe Color(0xFFFFF9EE)

            color.divider.primary shouldBe Color(0xFFC9D3FD)
            color.divider.secondary shouldBe Color(0xFFEDF0FE)
            color.overlay.dim shouldBe Color(0x99000000)
            color.overlay.light shouldBe Color(0x33000000)

            color.gradient.tertiaryStart shouldBe Color(0x00F7F8FB)
            color.gradient.tertiaryEnd shouldBe Color(0xFFF7F8FB)
        }
    })
