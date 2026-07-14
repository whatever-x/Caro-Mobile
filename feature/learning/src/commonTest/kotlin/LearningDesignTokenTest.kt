import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatever.caro.core.designsystem.themes.CaroColor
import com.whatever.caro.core.designsystem.themes.CaroSpacing
import com.whatever.caro.core.designsystem.themes.CaroTypography
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LearningDesignTokenTest :
    FunSpec({
        test("학습 화면 공용 색상 토큰이 Figma 값과 일치한다") {
            val color = CaroColor.defaultColor()

            color.surface.tertiary.toArgb() shouldBe 0xFFEDF0FE.toInt()
            color.border.primary.toArgb() shouldBe 0xFFF6F8FF.toInt()
            color.border.secondary.toArgb() shouldBe 0xFFD8DADD.toInt()
            color.icon.tertiary.toArgb() shouldBe 0xFFA4B5FB.toInt()
            color.icon.disable.toArgb() shouldBe 0xFFF8F8F9.toInt()
            color.text.disable.toArgb() shouldBe 0xFFB0B2B7.toInt()
            color.text.info.toArgb() shouldBe 0xFF4A6CF7.toInt()
            color.overlay.dim.toArgb() shouldBe 0x99000000.toInt()
            color.gradient.tertiaryStart.toArgb() shouldBe 0x00F7F8FB
            color.gradient.tertiaryEnd.toArgb() shouldBe 0xFFF7F8FB.toInt()
            color.button.surface.easy
                .toArgb() shouldBe 0xFFC9D3FD.toInt()
            color.button.surface.fair
                .toArgb() shouldBe 0xFFFFEFCD.toInt()
            color.button.surface.hard
                .toArgb() shouldBe 0xFFFFD7D4.toInt()
            color.button.pressed.easy
                .toArgb() shouldBe 0xFFA4B5FB.toInt()
            color.button.pressed.fair
                .toArgb() shouldBe 0xFFFFE3AB.toInt()
            color.button.pressed.hard
                .toArgb() shouldBe 0xFFFFBCB7.toInt()
        }

        test("학습 완료 화면 간격 토큰이 Figma 값과 일치한다") {
            CaroSpacing.defaultSpacing().xl2_2 shouldBe 28.dp
        }

        test("학습 화면 공용 타이포그래피 토큰이 Figma 값과 일치한다") {
            val typography =
                CaroTypography.defaultTypography(
                    pretendard = FontFamily.Default,
                    roboto = FontFamily.Default,
                )

            typography.display.fontSize shouldBe 24.sp
            typography.heading1.fontSize shouldBe 20.sp
            typography.heading2.fontWeight shouldBe FontWeight.W600
            typography.heading2.lineHeight shouldBe 24.sp
            typography.heading3.fontWeight shouldBe FontWeight.W600
            typography.body2.semiBold.lineHeight shouldBe 20.sp
            typography.body3.fontWeight shouldBe FontWeight.W500
            typography.body3.lineHeight shouldBe 20.sp
            typography.label1.regular.fontWeight shouldBe FontWeight.W500
            typography.label1.regular.lineHeight shouldBe 20.sp
        }
    })
