package com.whatever.caro.core.screenshot.testing

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenshotDialogAndroidTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `다이얼로그 창 안에서 선택한 시맨틱스 노드의 소유 뷰를 찾는다`() {
        val capture =
            screenshotCaptures("android") {
                listOf(
                    ScreenshotScenario(
                        "fixture.dialog",
                        render = { Dialog(onDismissRequest = {}) { Box(Modifier.size(120.dp)) } },
                        expectedStrings = emptyList(),
                        target = ScreenshotTarget.DIALOG,
                    ),
                )
            }.first()
        composeRule.setContent { ScreenshotCanvas(capture) }
        val node = composeRule.onNode(isDialog()).fetchSemanticsNode()
        val owner = checkNotNull(node.root as? ViewRootForTest).view
        dialogScreenshotView(composeRule, capture, node).check { view, exception ->
            if (exception != null) throw exception
            assertSame(owner, view)
            assertNotSame(composeRule.activity.window.decorView, view.rootView)
        }
    }
}
