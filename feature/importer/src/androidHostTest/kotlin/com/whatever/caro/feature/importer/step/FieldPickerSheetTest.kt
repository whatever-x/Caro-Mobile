package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.FieldPickerSheet
import com.whatever.caro.feature.importer.component.FieldPickerSheetFrame
import com.whatever.caro.feature.importer.component.isHeading
import com.whatever.caro.feature.importer.logic.PickerRow
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 하단 시트 창 없이 내용을 그 자리에 그리고, 호스트가 넘긴 닫기 요청 콜백을 붙잡는다. 밀어 닫기와 뒤로 가기는 모두 이 콜백으로 들어온다. */
internal class RecordingSheetFrame {
    var onDismissRequest: (() -> Unit)? = null
        private set

    val frame: FieldPickerSheetFrame =
        { dismissRequest, content ->
            SideEffect { onDismissRequest = dismissRequest }
            Column(modifier = Modifier.fillMaxSize()) { content(dismissRequest) }
        }
}

// 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class FieldPickerSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<String>()
    private val recording = RecordingSheetFrame()

    // Figma 항목 고르기 시트의 줄이다. 뒷면에 네 개를 골랐고 단어는 앞면에 있다.
    private fun figmaRows(extraCount: Int = 0): ImmutableList<PickerRow> =
        (
            listOf(
                PickerRow(name = "뜻", order = 1, isInOtherSide = false),
                PickerRow(name = "예문", order = 2, isInOtherSide = false),
                PickerRow(name = "읽기", order = 3, isInOtherSide = false),
                PickerRow(name = "품사", order = 4, isInOtherSide = false),
                PickerRow(name = "단어", order = null, isInOtherSide = true),
                PickerRow(name = "한자", order = null, isInOtherSide = false),
            ) + List(extraCount) { index -> PickerRow(name = "항목 ${index + 7}", order = null, isInOtherSide = false) }
        ).toImmutableList()

    private fun show(
        rows: ImmutableList<PickerRow> = figmaRows(),
        height: Dp = 700.dp,
    ) {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.size(393.dp, height)) {
                    FieldPickerSheet(
                        title = TITLE,
                        sideLabel = "뒷면",
                        otherSideLabel = "앞면",
                        rows = rows,
                        onToggleField = { name -> events += "toggle:$name" },
                        onDismiss = { events += "dismiss" },
                        frame = recording.frame,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** 실제 하단 시트 창으로 그린다. */
    private fun showInRealSheet(rows: ImmutableList<PickerRow> = figmaRows(extraCount = 14)) {
        composeRule.setContent {
            CaroTheme {
                FieldPickerSheet(
                    title = TITLE,
                    sideLabel = "뒷면",
                    otherSideLabel = "앞면",
                    rows = rows,
                    onToggleField = { name -> events += "toggle:$name" },
                    onDismiss = { events += "dismiss" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    // 다른 면에 있는 줄은 읽는 문구에 어느 면에 있는지가 붙을 수 있어 이름을 포함하는지로 찾는다.
    private fun row(name: String): SemanticsNodeInteraction = composeRule.onNode(isCheckboxRole and readsContaining(name))

    private fun doneButton(): SemanticsNodeInteraction = composeRule.onNodeWithText("완료")

    private fun visibleText(text: String): SemanticsNodeInteraction = composeRule.onNodeWithText(text, useUnmergedTree = true)

    private fun SemanticsNodeInteraction.toggleState(): ToggleableState? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState)

    @Test
    fun `시트 제목을 제목으로 읽히게 한다`() {
        show()

        composeRule.onNode(hasText(TITLE) and isHeading).assertIsDisplayed()
    }

    @Test
    fun `열리면 제목에 포커스를 둔다`() {
        show()

        composeRule.onNode(hasText(TITLE) and isHeading).assertIsFocused()
    }

    @Test
    fun `고른 순서대로 줄을 바꿔 합친다는 안내를 보여준다`() {
        show()

        visibleText("고른 순서대로 줄을 바꿔 합쳐요.").assertIsDisplayed()
    }

    @Test
    fun `항목마다 줄 하나를 원본 순서대로 그린다`() {
        show()

        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(6)
    }

    @Test
    fun `고른 항목 줄 오른쪽에 고른 순서를 보여준다`() {
        show()

        listOf("1번째", "2번째", "3번째", "4번째").forEach { order -> visibleText(order).assertIsDisplayed() }
    }

    @Test
    fun `다른 면에 놓인 항목 줄에는 어느 면에 있는지 보여준다`() {
        show()

        visibleText("앞면에 있어요").assertIsDisplayed()
    }

    @Test
    fun `고른 항목 줄은 항목 이름과 면과 순서를 한 문장으로 읽는다`() {
        show()

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("품사", "뒷면", 4)).assertIsDisplayed()
    }

    @Test
    fun `고른 줄은 켜진 상태이고 고르지 않은 줄과 다른 면의 줄은 꺼진 상태다`() {
        show()

        listOf(row("뜻").toggleState(), row("한자").toggleState(), row("단어").toggleState()) shouldBe
            listOf(ToggleableState.On, ToggleableState.Off, ToggleableState.Off)
    }

    @Test
    fun `줄의 눌리는 높이는 48dp 이상이다`() {
        show()

        row("한자").assertHeightIsAtLeast(48.dp)
        row("뜻").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `고른 줄을 누르면 그 항목으로 선택 변경을 요청한다`() {
        show()

        row("뜻").performClick()

        events shouldBe listOf("toggle:뜻")
    }

    @Test
    fun `고르지 않은 줄을 누르면 그 항목으로 선택 변경을 요청한다`() {
        show()

        row("한자").performClick()

        events shouldBe listOf("toggle:한자")
    }

    @Test
    fun `다른 면의 항목을 고르면 이동했다는 안내를 polite live region으로 알린다`() {
        show()

        row("단어").performClick()

        composeRule.onNode(isPoliteRegion and readsAs("앞면에서 빼고 뒷면 5번째로 넣었어요")).assertExists()
    }

    @Test
    fun `이동 안내의 번호는 이미 고른 항목 수 다음 번호다`() {
        show(rows = figmaRows().filterNot { row -> row.name == "품사" }.toImmutableList())

        row("단어").performClick()

        composeRule.onNode(isPoliteRegion and readsAs("앞면에서 빼고 뒷면 4번째로 넣었어요")).assertExists()
    }

    @Test
    fun `다른 면의 항목을 고르기 전에는 이동 안내가 없다`() {
        show()

        row("단어").assertIsDisplayed()
        composeRule.onAllNodes(readsContaining("빼고")).assertCountEquals(0)
    }

    @Test
    fun `이미 고른 항목을 풀 때는 이동 안내가 없다`() {
        show()

        row("뜻").performClick()

        composeRule.onAllNodes(readsContaining("빼고")).assertCountEquals(0)
    }

    @Test
    fun `어느 면에도 없던 항목을 고를 때는 이동 안내가 없다`() {
        show()

        row("한자").performClick()

        composeRule.onAllNodes(readsContaining("빼고")).assertCountEquals(0)
    }

    @Test
    fun `완료 버튼을 누르면 시트를 닫아 달라고 한 번 요청한다`() {
        show()

        doneButton().performClick()

        events shouldBe listOf("dismiss")
    }

    @Test
    fun `완료 버튼은 버튼 역할이고 높이가 48dp 이상이다`() {
        show()

        doneButton().assertHeightIsAtLeast(48.dp)
        composeRule.onNode(isButtonRole and hasText("완료")).assertExists()
    }

    @Test
    fun `밀어 닫거나 뒤로 가기를 눌러 온 닫기 요청도 시트를 닫아 달라고 요청한다`() {
        show()

        composeRule.runOnIdle { recording.onDismissRequest?.invoke() }

        events shouldBe listOf("dismiss")
    }

    @Test
    fun `밀어 닫기 전에 고른 항목은 이미 선택 변경 요청으로 전달되어 있다`() {
        show()

        row("한자").performClick()
        composeRule.runOnIdle { recording.onDismissRequest?.invoke() }

        events shouldBe listOf("toggle:한자", "dismiss")
    }

    @Test
    fun `줄이 많으면 목록은 시트 안에서 세로로 스크롤한다`() {
        show(rows = figmaRows(extraCount = 14), height = 520.dp)

        composeRule.onAllNodes(hasVerticalScrollRange).assertCountEquals(1)
    }

    @Test
    fun `줄이 많아도 완료 버튼은 스크롤하지 않고 화면 안에 고정된다`() {
        show(rows = figmaRows(extraCount = 14), height = 520.dp)

        doneButton().assertIsDisplayed()
        composeRule.onNode(hasVerticalScrollRange).performScrollToNode(readsContaining("항목 20"))

        row("항목 20").assertIsDisplayed()
        doneButton().assertIsDisplayed()
    }

    @Test
    fun `80자를 넘는 항목 이름은 그리기 전에 80자로 줄여 말줄임을 붙인다`() {
        val longName = "긴".repeat(100)
        show(rows = (figmaRows() + PickerRow(name = longName, order = null, isInOtherSide = false)).toImmutableList())

        composeRule.onAllNodes(readsContaining(longName), useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodes(readsContaining("긴".repeat(79) + "…")).assertCountEquals(1)
    }

    @Test
    fun `실제 하단 시트는 열자마자 끝까지 펼쳐 줄이 많아도 완료 버튼이 화면 안에 보인다`() {
        showInRealSheet()

        doneButton().assertIsDisplayed()
    }

    @Test
    fun `실제 하단 시트에서 완료 버튼을 누르면 시트를 닫아 달라고 요청한다`() {
        showInRealSheet()

        doneButton().performClick()
        composeRule.waitForIdle()

        events shouldBe listOf("dismiss")
    }

    private companion object {
        const val TITLE = "뒷면에 넣을 항목"
    }
}
