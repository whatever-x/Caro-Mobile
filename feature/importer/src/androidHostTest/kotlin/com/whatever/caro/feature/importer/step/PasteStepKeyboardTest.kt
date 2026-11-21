package com.whatever.caro.feature.importer.step

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.components.isHeading
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

// 키보드는 화면에 ime insets를 보내 흉내 낸다. 화면 높이는 상단 바 아래 남는 높이이고, 작은 화면은 Figma의 360 x 640 기준이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PasteStepKeyboardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var hostView: View

    private fun show(
        width: Dp,
        height: Dp,
        isOverflowed: Boolean,
    ) {
        composeRule.setContent {
            hostView = LocalView.current
            CaroTheme {
                Box(modifier = Modifier.size(width, height)) {
                    PasteStep(
                        text = if (isOverflowed) "가".repeat(11) else "학교\t학교",
                        maxPasteChars = 10,
                        isOverflowed = isOverflowed,
                        isNextEnabled = !isOverflowed,
                        onTextChange = {},
                        onNext = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun openKeyboard(height: Dp) {
        composeRule.runOnIdle {
            val bottom = (height.value * hostView.resources.displayMetrics.density).roundToInt()
            val insets =
                WindowInsetsCompat
                    .Builder()
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, bottom))
                    .setVisible(WindowInsetsCompat.Type.ime(), true)
                    .build()
            ViewCompat.dispatchApplyWindowInsets(hostView, insets)
        }
        composeRule.waitForIdle()
    }

    private fun assertErrorAboveKeyboard(
        screenHeight: Dp,
        keyboardHeight: Dp,
    ) {
        val error = composeRule.onNodeWithText(TOO_LONG).assertIsDisplayed().getUnclippedBoundsInRoot()
        val field = composeRule.onNode(hasSetTextAction()).getUnclippedBoundsInRoot()

        error.bottom shouldBeLessThanOrEqualTo screenHeight - keyboardHeight
        field.bottom shouldBeLessThanOrEqualTo error.top
    }

    @Test
    fun `보통 화면에서 키보드가 열려도 길이 초과 오류 문구는 키보드 위에 보이고 제목도 남는다`() {
        show(width = 393.dp, height = REGULAR_HEIGHT, isOverflowed = true)

        openKeyboard(KEYBOARD_HEIGHT)

        assertErrorAboveKeyboard(REGULAR_HEIGHT, KEYBOARD_HEIGHT)
        composeRule.onNode(hasText(TITLE) and isHeading).assertIsDisplayed()
    }

    @Test
    fun `작은 화면에서 키보드가 열리면 제목은 숨지만 길이 초과 오류 문구는 키보드 위에 보인다`() {
        show(width = 360.dp, height = SMALL_HEIGHT, isOverflowed = true)

        openKeyboard(KEYBOARD_HEIGHT)

        assertErrorAboveKeyboard(SMALL_HEIGHT, KEYBOARD_HEIGHT)
        composeRule.onNodeWithText(TITLE).assertDoesNotExist()
    }

    @Test
    fun `작은 화면에서 키보드가 열리면 오류가 없을 때 제목과 안내 목록을 숨긴다`() {
        show(width = 360.dp, height = SMALL_HEIGHT, isOverflowed = false)

        openKeyboard(KEYBOARD_HEIGHT)

        composeRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(FIRST_GUIDE).assertDoesNotExist()
    }

    @Test
    fun `보통 화면에서 키보드가 열려도 오류가 없으면 제목과 안내 목록이 남는다`() {
        show(width = 393.dp, height = REGULAR_HEIGHT, isOverflowed = false)

        openKeyboard(KEYBOARD_HEIGHT)

        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(FIRST_GUIDE).assertIsDisplayed()
    }

    private companion object {
        const val TITLE = "복사한 내용을 붙여 넣어 주세요"
        const val FIRST_GUIDE = "한 줄이 카드 한 장이 돼요."
        const val TOO_LONG = "한 번에 10자까지 붙여 넣을 수 있어요. 나눠서 가져와 주세요."

        // 상태 표시줄 50과 상단 바 56을 뺀 높이다.
        val REGULAR_HEIGHT = 746.dp
        val SMALL_HEIGHT = 534.dp
        val KEYBOARD_HEIGHT = 336.dp
    }
}
