package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.support.PreviewSamples
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 미리보기 하단의 [가져오기]와 그 뒤 덱을 만드는 동안의 덮개를 본다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewImportButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = PreviewScreenHost(composeRule)
    private val preview = PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview)

    @Test
    fun `가져오기는 카드 목록을 끝까지 내려도 화면 아래에 남는다`() {
        host.show(preview, height = SHORT_HEIGHT)

        composeRule.onNodeWithText("가져오기").assertIsDisplayed()
    }

    @Test
    fun `덱을 만드는 중에는 덮개가 덱을 만든다고 알리고 밑에 깔린 가져오기에 닿지 않는다`() {
        host.show(preview.copy(loading = ImportLoading.CreatingDeck))
        composeRule.mainClock.advanceTimeBy(LOADING_CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("덱을 만들고 있어요", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("가져오기").assertDoesNotExist()
        host.intents shouldBe emptyList()
    }

    private companion object {
        val SHORT_HEIGHT = 480.dp
        const val LOADING_CARD_DELAY_MILLIS = 300L
    }
}
