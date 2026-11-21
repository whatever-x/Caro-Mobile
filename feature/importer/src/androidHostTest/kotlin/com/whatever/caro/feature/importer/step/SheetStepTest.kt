package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
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
class SheetStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val headerRow = hasText("첫 줄을 열 이름으로 쓰기")

    @Test
    fun `시트마다 줄 수를 보이고 빈 시트는 비어 있음, 상한을 넘는 시트는 넘음으로 보인다`() {
        host.show(SettingsSamples.sheetState(SettingsSamples.overLimitSheetTable, entry = SettingsEntry.Gate))

        composeRule.onNode(hasText("전체 단어") and hasText("10,000줄 넘음")).assertIsDisplayed()
        composeRule.onNode(hasText("N5 단어") and hasText("820줄")).assertIsDisplayed()
        composeRule.onNode(hasText("메모") and hasText("비어 있음")).assertIsDisplayed()
    }

    @Test
    fun `고른 시트만 선택 상태이고 다른 시트를 누르면 시트 고르기 입력을 보낸다`() {
        host.show(SettingsSamples.sheetState())

        composeRule.onNode(hasText("Day1") and hasText("864줄")).assertIsSelected()
        composeRule.onNode(hasText("Day2") and hasText("802줄")).assertIsNotSelected().performClick()

        host.intents shouldBe listOf(ImportDeckIntent.SelectSheet(1))
    }

    @Test
    fun `시트 라디오는 한 선택 묶음 안에서 읽힌다`() {
        host.show(SettingsSamples.sheetState())

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .onChildren()
            .filter(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertCountEquals(SettingsSamples.sheetTable.sheets.size)
    }

    @Test
    fun `먼저 고칠 것으로 열리면 제목이 위쪽 띠 문구까지 읽는다`() {
        host.show(SettingsSamples.sheetState(SettingsSamples.overLimitSheetTable, entry = SettingsEntry.Gate))

        composeRule
            .onNode(isHeading() and hasText("가져올 시트를 골라 주세요"))
            .assert(hasContentDescription("가져올 시트를 골라 주세요, 한 번에 10,000장까지 가져올 수 있어서 미리보기 전에 이 화면을 열었어요."))
    }

    @Test
    fun `다시 읽는 중에는 방금 고른 시트를 선택 상태로 보인다`() {
        val state = SettingsSamples.sheetState()
        host.show(state.copy(pendingDraftSelection = state.draftSelection.copy(sheetIndex = 1)))

        composeRule.onNode(hasText("Day2") and hasText("802줄")).assertIsSelected()
    }

    @Test
    fun `첫 줄 체크는 설명과 함께 한 덩어리로 읽고 누르면 반대 값을 보낸다`() {
        host.show(SettingsSamples.sheetState())

        composeRule
            .onNode(headerRow and hasText("첫 줄은 카드로 가져오지 않고 열 이름으로 써요."))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .performClick()

        host.intents shouldBe listOf(ImportDeckIntent.ChangeHeaderRow(false))
    }

    @Test
    fun `첫 줄 체크는 누르는 높이가 48 이상이다`() {
        host.show(SettingsSamples.sheetState())

        (composeRule.onNode(headerRow).bounds().height >= SettingsScreenHost.MIN_TOUCH_HEIGHT) shouldBe true
    }

    @Test
    fun `먼저 고칠 것으로 열리면 상한을 넘는 시트의 사유와 꺼진 미리보기를 보이고 다른 시트를 고르면 켠다`() {
        host.show(SettingsSamples.sheetState(SettingsSamples.overLimitSheetTable, entry = SettingsEntry.Gate))
        composeRule.onNodeWithText("10,000줄을 넘는 시트예요. 다른 시트를 골라 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("미리보기").assertIsNotEnabled()

        host.update(SettingsSamples.sheetState(SettingsSamples.overLimitSheetTable, sheetIndex = 1, entry = SettingsEntry.Gate))

        composeRule.onNodeWithText("10,000줄을 넘는 시트예요. 다른 시트를 골라 주세요.").assertDoesNotExist()
        composeRule.onNodeWithText("미리보기").assertIsEnabled()
    }
}
