package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.FieldPickerSheet
import com.whatever.caro.feature.importer.logic.PickerRow
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 고른 칩과 고른 줄은 선택됨을 한 번만 읽어야 한다. 글자는 이름이고 선택됨과 면, 순서는 상태 설명이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class FieldChosenSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { MappingStepHost(composeRule) }
    private val events = mutableListOf<String>()

    private val rows =
        listOf(
            PickerRow(name = "뜻", order = 1, isInOtherSide = false),
            PickerRow(name = "한자", order = null, isInOtherSide = false),
        ).toImmutableList()

    private fun showRealSheet() {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.size(393.dp, 700.dp)) {
                    FieldPickerSheet(
                        title = "뒷면에 넣을 항목",
                        sideLabel = "뒷면",
                        otherSideLabel = "앞면",
                        rows = rows,
                        onToggleField = {},
                        onDismiss = { events += "dismiss" },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `고른 칩은 이름만 글자로 읽고 선택됨은 상태 설명에만 들어간다`() {
        host.show(MappingStepTables.table(MappingStepTables.vocab()))

        val node = composeRule.onNode(chosenChip("뜻", "뒷면", 1)).fetchSemanticsNode()

        node.readings() shouldBe listOf("뜻")
        node.config.contains(SemanticsProperties.ContentDescription) shouldBe false
    }

    @Test
    fun `고른 칩의 순서 번호는 따로 읽히지 않는다`() {
        host.show(MappingStepTables.table(MappingStepTables.vocab()))

        composeRule.onNode(chosenChip("읽기", "뒷면", 3)).fetchSemanticsNode().readings() shouldBe listOf("읽기")
    }

    @Test
    fun `고른 줄은 이름만 글자로 읽고 선택됨은 상태 설명에만 들어간다`() {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.size(393.dp, 700.dp)) {
                    FieldPickerSheet(
                        title = "뒷면에 넣을 항목",
                        sideLabel = "뒷면",
                        otherSideLabel = "앞면",
                        rows = rows,
                        onToggleField = {},
                        onDismiss = {},
                        frame = RecordingSheetFrame().frame,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val node = composeRule.onNode(chosenChip("뜻", "뒷면", 1)).fetchSemanticsNode()

        node.config.contains(SemanticsProperties.ContentDescription) shouldBe false
    }

    @Test
    fun `완료를 누르면 시트를 밀어 내리는 동안에는 닫기를 요청하지 않고 끝난 뒤에 요청한다`() {
        showRealSheet()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("완료").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        events shouldBe emptyList()

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        events shouldBe listOf("dismiss")
    }
}
