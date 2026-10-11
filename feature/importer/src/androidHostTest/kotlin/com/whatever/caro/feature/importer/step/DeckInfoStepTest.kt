package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.whatever.caro.feature.importer.step.DeckInfoStepHost.Companion.CONFIRM
import com.whatever.caro.feature.importer.step.DeckInfoStepHost.Companion.DESCRIPTION
import com.whatever.caro.feature.importer.step.DeckInfoStepHost.Companion.NAME
import com.whatever.caro.feature.importer.step.DeckInfoStepHost.Companion.SUMMARY
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구와 글자 수는 Figma 프레임의 표시 값이다. 상한(50, 500)은 덱 만들기와 같은 DeckInputLimits다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeckInfoStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = DeckInfoStepHost(composeRule)

    @Test
    fun `이름 입력칸에 받은 이름을 보여준다`() {
        host.show()

        composeRule.onNode(inputFieldWith(NAME)).assertIsDisplayed()
    }

    @Test
    fun `설명 입력칸에 받은 설명을 보여준다`() {
        host.show()

        composeRule.onNode(inputFieldWith(DESCRIPTION)).assertIsDisplayed()
    }

    @Test
    fun `이름 머리글에 덱 이름을 보여준다`() {
        host.show()

        composeRule.onNodeWithText("덱 이름").assertIsDisplayed()
    }

    @Test
    fun `설명 머리글에 설명을 보여준다`() {
        host.show()

        composeRule.onNodeWithText("설명").assertIsDisplayed()
    }

    @Test
    fun `두 머리글 모두 필수 표시를 붙인다`() {
        host.show()

        composeRule.onAllNodesWithText("*").assertCountEquals(2)
    }

    @Test
    fun `10자 이름이면 이름 글자 수가 10 나누기 50이다`() {
        host.show(name = "가".repeat(10))

        composeRule.onNodeWithText("10/50").assertIsDisplayed()
    }

    @Test
    fun `50자 이름이면 이름 글자 수가 50 나누기 50이다`() {
        host.show(name = "가".repeat(50))

        composeRule.onNodeWithText("50/50").assertIsDisplayed()
    }

    @Test
    fun `23자 설명이면 설명 글자 수가 23 나누기 500이다`() {
        host.show(description = "가".repeat(23))

        composeRule.onNodeWithText("23/500").assertIsDisplayed()
    }

    @Test
    fun `500자 설명이면 설명 글자 수가 500 나누기 500이다`() {
        host.show(description = "가".repeat(500))

        composeRule.onNodeWithText("500/500").assertIsDisplayed()
    }

    @Test
    fun `카드 수는 천 단위 구분 기호를 넣어 새 덱 안내 줄에 보여준다`() {
        host.show(cardCount = 1_512)

        composeRule.onNodeWithText(SUMMARY).assertIsDisplayed()
    }

    @Test
    fun `카드가 한 자리 수여도 새 덱 안내 줄에 그 장수를 보여준다`() {
        host.show(cardCount = 8)

        composeRule.onNodeWithText("카드 8장으로 새 덱을 만들어요.").assertIsDisplayed()
    }

    @Test
    fun `가져오기 버튼을 누르면 확인을 한 번 요청한다`() {
        host.show()

        composeRule.onNodeWithText(CONFIRM).performClick()

        host.confirmClicks shouldBe 1
    }

    @Test
    fun `확인이 켜져 있으면 가져오기 버튼이 활성이다`() {
        host.show(isConfirmEnabled = true)

        composeRule.onNodeWithText(CONFIRM).assertIsEnabled()
    }

    @Test
    fun `확인이 꺼져 있으면 가져오기 버튼이 비활성이다`() {
        host.show(isConfirmEnabled = false)

        composeRule.onNodeWithText(CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun `비활성 가져오기 버튼을 눌러도 확인을 요청하지 않는다`() {
        host.show(isConfirmEnabled = false)

        composeRule.onNodeWithText(CONFIRM).performClick()

        host.confirmClicks shouldBe 0
    }

    @Test
    fun `이름을 바꾸면 바뀐 이름을 알린다`() {
        host.show()

        composeRule.onNode(inputFieldWith(NAME)).performTextReplacement("TOEIC 3000")

        host.nameChanges shouldBe listOf("TOEIC 3000")
    }

    @Test
    fun `이름을 바꿔도 설명 변경은 알리지 않는다`() {
        host.show()

        composeRule.onNode(inputFieldWith(NAME)).performTextReplacement("TOEIC 3000")

        host.descriptionChanges shouldBe emptyList()
    }

    @Test
    fun `설명을 바꾸면 바뀐 설명을 알린다`() {
        host.show()

        composeRule.onNode(inputFieldWith(DESCRIPTION)).performTextReplacement("토익 단어")

        host.descriptionChanges shouldBe listOf("토익 단어")
    }

    @Test
    fun `처음 그릴 때 이름 입력칸에 포커스를 두지 않는다`() {
        host.show()

        composeRule.onNode(inputFieldWith(NAME)).assertIsNotFocused()
    }

    @Test
    fun `처음 그릴 때 설명 입력칸에 포커스를 두지 않는다`() {
        host.show()

        composeRule.onNode(inputFieldWith(DESCRIPTION)).assertIsNotFocused()
    }

    @Test
    fun `이름 입력칸을 누르면 포커스가 간다`() {
        host.show()

        composeRule.onNode(inputFieldWith(NAME)).performClick()

        composeRule.onNode(inputFieldWith(NAME)).assertIsFocused()
    }

    @Test
    fun `입력칸 바깥을 누르면 이름 입력칸의 포커스를 푼다`() {
        host.show()
        composeRule.onNode(inputFieldWith(NAME)).performClick()

        composeRule.onNodeWithText(SUMMARY).performClick()

        composeRule.onNode(inputFieldWith(NAME)).assertIsNotFocused()
    }

    @Test
    fun `입력칸 바깥을 누르면 설명 입력칸의 포커스를 푼다`() {
        host.show()
        composeRule.onNode(inputFieldWith(DESCRIPTION)).performClick()

        composeRule.onNodeWithText(SUMMARY).performClick()

        composeRule.onNode(inputFieldWith(DESCRIPTION)).assertIsNotFocused()
    }
}
