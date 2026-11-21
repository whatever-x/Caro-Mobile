package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.designsystem.themes.CaroColor
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.support.SettingsSamples
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SubdeckStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val overLimitReason = "고른 하위 덱을 줄이거나 Anki에서 하위 덱별로 나눠 내보내 주세요."

    private fun toggleState(state: ToggleableState) = SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)

    @Test
    fun `공통 상위 덱을 뺀 이름과 장수를 보이고 전체 선택 줄은 합계를 보인다`() {
        host.show(SettingsSamples.subdeckState())

        composeRule.onNodeWithText(SettingsSamples.ANKI_FILE).assertIsDisplayed()
        composeRule.onNode(hasText("전체 선택") and hasText("1,514장")).assertIsDisplayed()
        composeRule.onNode(hasText("단어") and hasText("1,204장")).assertIsDisplayed()
        composeRule.onNode(hasText("문법") and hasText("310장")).assertIsDisplayed()
    }

    @Test
    fun `일부만 고르면 전체 선택이 일부 선택 상태가 되고 고른 합계를 하단에 보인다`() {
        host.show(SettingsSamples.subdeckState(SettingsSamples.partialSubdeckTable, selected = setOf("words", "day01")))

        composeRule.onNode(hasText("전체 선택") and toggleState(ToggleableState.Indeterminate)).assertIsDisplayed()
        composeRule.onNode(hasText("문법") and toggleState(ToggleableState.Off)).assertIsDisplayed()
        composeRule.onNode(hasText("선택한 카드") and hasText("1,724장") and hasText("/ 최대 10,000장")).assertIsDisplayed()
    }

    @Test
    fun `긴 하위 덱 이름은 3줄까지 보이고 말줄임한다`() {
        host.show(SettingsSamples.subdeckState(SettingsSamples.partialSubdeckTable))

        val layout = composeRule.onNodeWithText(SettingsSamples.LONG_SUBDECK, useUnmergedTree = true).textLayout()
        layout.lineCount shouldBe 3
        layout.hasVisualOverflow shouldBe true
    }

    @Test
    fun `하위 덱 줄과 전체 선택 줄을 누르면 고르기 입력을 보낸다`() {
        host.show(SettingsSamples.subdeckState())

        composeRule.onNode(hasText("문법") and hasText("310장")).performClick()
        composeRule.onNode(hasText("전체 선택") and hasText("1,514장")).performClick()

        host.intents shouldBe listOf(ImportDeckIntent.ToggleSubdeck("grammar"), ImportDeckIntent.ToggleAllSubdecks)
    }

    @Test
    fun `적용을 누르면 적용 입력을 보낸다`() {
        host.show(SettingsSamples.subdeckState())

        composeRule.onNodeWithText("적용").assertIsEnabled().performClick()

        host.intents shouldBe listOf(ImportDeckIntent.ClickApplySettings)
    }

    @Test
    fun `하나도 고르지 않으면 사유 없이 적용만 꺼진다`() {
        host.show(SettingsSamples.subdeckState(selected = emptySet()))

        composeRule.onNodeWithText("적용").assertIsNotEnabled()
        composeRule.onNodeWithText(overLimitReason).assertDoesNotExist()
    }

    @Test
    fun `먼저 고칠 것으로 열리면 위쪽 안내 띠와 미리보기 버튼, 초과 합계와 위험 색 사유를 보이고 버튼을 끈다`() {
        host.show(SettingsSamples.subdeckState(SettingsSamples.overLimitSubdeckTable, entry = SettingsEntry.Gate))

        composeRule.onNodeWithText("한 번에 10,000장까지 가져올 수 있어서 미리보기 전에 이 화면을 열었어요.").assertIsDisplayed()
        composeRule.onNode(hasText("선택한 카드") and hasText("12,400장 (초과)")).assertIsDisplayed()
        composeRule.onNodeWithText("미리보기").assertIsNotEnabled()
        composeRule.onNodeWithText("적용").assertDoesNotExist()
        val reason = composeRule.onNodeWithText(overLimitReason).assertIsDisplayed()
        reason
            .textLayout()
            .layoutInput.style.color shouldBe CaroColor.defaultColor().text.dangerous
    }

    @Test
    fun `먼저 고칠 것으로 열리면 덮개가 걷힌 뒤 포커스가 가는 제목이 위쪽 띠 문구까지 읽는다`() {
        host.show(SettingsSamples.subdeckState(SettingsSamples.overLimitSubdeckTable, entry = SettingsEntry.Gate))

        composeRule
            .onNode(isHeading() and hasText("가져올 하위 덱을 골라 주세요"))
            .assert(hasContentDescription("가져올 하위 덱을 골라 주세요, $GATE_NOTICE"))
    }

    @Test
    fun `미리보기에서 열면 제목은 제목만 읽는다`() {
        host.show(SettingsSamples.subdeckState())

        val title = composeRule.onNode(isHeading() and hasText("가져올 하위 덱을 골라 주세요")).fetchSemanticsNode()
        (SemanticsProperties.ContentDescription in title.config) shouldBe false
    }

    @Test
    fun `꺼진 버튼은 사유를 상태 설명으로 함께 읽는다`() {
        host.show(SettingsSamples.subdeckState(SettingsSamples.overLimitSubdeckTable, entry = SettingsEntry.Gate))

        val button = composeRule.onNodeWithText("미리보기").fetchSemanticsNode()
        button.config[SemanticsProperties.StateDescription] shouldBe overLimitReason
    }

    @Test
    fun `큰 글자에서는 장수를 이름 아래로 내린다`() {
        host.show(SettingsSamples.subdeckState(), fontScale = 1.3f)

        val name = composeRule.onNodeWithText("문법", useUnmergedTree = true).bounds()
        val count = composeRule.onNodeWithText("310장", useUnmergedTree = true).bounds()
        (count.top >= name.bottom) shouldBe true
    }

    private companion object {
        const val GATE_NOTICE = "한 번에 10,000장까지 가져올 수 있어서 미리보기 전에 이 화면을 열었어요."
    }
}
