package com.whatever.caro.feature.importer.step

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.support.SettingsSamples
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 항목이 많은 유형의 고르기 버튼과 항목 고르기 시트를 화면에서 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class MappingFieldPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val manyFields = SettingsSamples.mappingState(SettingsSamples.manyFieldsTable, SettingsSamples.manyFieldsMapping)
    private val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    private fun backButton() = composeRule.onNodeWithContentDescription("뒷면에 넣을 항목 고르기, 20개 중 4개 선택됨")

    @Test
    fun `항목 고르기 시트를 완료로 닫으면 열었던 고르기 버튼으로 포커스가 돌아온다`() {
        host.show(manyFields)
        backButton().performClick()

        composeRule.onNodeWithText("완료").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("완료").assertDoesNotExist()
        backButton().assertIsFocused()
    }

    @Test
    fun `항목 고르기 시트를 밀어 닫아도 열었던 고르기 버튼으로 포커스가 돌아온다`() {
        host.show(manyFields)
        backButton().performClick()

        composeRule.onNode(hasText("뒷면에 넣을 항목") and isHeading()).performTouchInput {
            swipeDown(startY = top, endY = top + SWIPE_DISTANCE_PX)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("완료").assertDoesNotExist()
        backButton().assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `시트를 닫고 돌아온 포커스에서 Enter를 누르면 고르기 버튼이 눌려 시트를 다시 연다`() {
        host.show(manyFields)
        backButton().performClick()
        composeRule.onNodeWithText("완료").performClick()
        composeRule.waitForIdle()

        backButton().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("완료").assertIsDisplayed()
    }

    @Test
    fun `끈 유형의 고르기 버튼은 꺼진 상태로만 알리고 사용할 수 없다는 상태 설명을 겹쳐 붙이지 않는다`() {
        val excluded = SettingsSamples.manyFieldsMapping.mapValues { (_, mapping) -> mapping.copy(excluded = true) }
        host.show(SettingsSamples.mappingState(SettingsSamples.manyFieldsTable, excluded))

        val button = backButton().assertIsNotEnabled().fetchSemanticsNode()

        (SemanticsProperties.StateDescription in button.config) shouldBe false
    }

    @Test
    fun `다른 면의 항목을 고르면 옮긴 자리를 알리고 시간이 지나면 알림 문장을 비워 탐색에 남기지 않는다`() {
        host.show(manyFields)
        backButton().performClick()
        val moved = hasContentDescription("앞면에서 빼고 뒷면 5번째로 넣었어요")

        composeRule.onNode(hasText("단어") and hasText("앞면에 있어요") and isCheckbox).performClick()
        composeRule.onNode(moved).assertExists()

        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_SETTLE_MILLIS)

        composeRule.onNode(moved).assertDoesNotExist()
    }

    @Test
    fun `큰 글자에서는 고른 항목 요약을 위에, 고르기 버튼을 아래에 쌓고 버튼은 화면 폭 안에 둔다`() {
        val width = 360.dp
        host.show(manyFields, width = width, fontScale = 2f)

        val summary = composeRule.onNodeWithText("뜻, 예문, 읽기 외 1개", useUnmergedTree = true).bounds()
        val button = backButton().bounds()

        (summary.bottom <= button.top) shouldBe true
        (button.right <= width) shouldBe true
    }

    @Test
    fun `항목 수와 고른 항목 수는 천 단위 쉼표를 넣어 보인다`() {
        val names = List(FIELD_COUNT) { index -> "항목 ${index + 1}" }
        val type =
            SettingsSamples.manyFieldsTable.noteTypes
                .single()
                .copy(fieldNames = names)
        val table = SettingsSamples.manyFieldsTable.copy(noteTypes = listOf(type))
        val mapping = FieldMapping(type.id, mapOf(FieldMapping.FRONT to names.take(1), FieldMapping.BACK to names.drop(1)))
        host.show(SettingsSamples.mappingState(table, mapOf(type.id to mapping)))

        composeRule.onNodeWithText("항목 2, 항목 3, 항목 4 외 1,196개").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒷면에 넣을 항목 고르기, 1,200개 중 1,199개 선택됨").assertIsDisplayed()
    }

    private companion object {
        const val ANNOUNCEMENT_SETTLE_MILLIS = 10_000L
        const val SWIPE_DISTANCE_PX = 2_000f
        const val FIELD_COUNT = 1_200
    }
}
