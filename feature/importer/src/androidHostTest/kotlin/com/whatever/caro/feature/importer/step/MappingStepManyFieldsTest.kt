package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class MappingStepManyFieldsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { MappingStepHost(composeRule) }

    private fun showWide(
        fieldCount: Int = 20,
        isCloze: Boolean = false,
        applyEdits: Boolean = false,
        fontScale: Float = 1f,
        width: Dp = 393.dp,
        height: Dp = 700.dp,
    ) {
        host.show(
            table = MappingStepTables.table(MappingStepTables.wide(fieldCount, isCloze = isCloze)),
            mappings = MappingStepTables.wideMappings(fieldCount),
            applyEdits = applyEdits,
            fontScale = fontScale,
            width = width,
            height = height,
        )
    }

    // 버튼은 글자를 보이는 문구로, 접근성 설명을 별도 문장으로 가진다. 설명으로 쓰는 병합 방식과 상관없이 찾으려고 병합하지 않은 트리의 글자도 쓴다.
    private fun pickButton(
        side: String,
        total: Int,
        chosen: Int,
    ): SemanticsNodeInteraction = composeRule.onNode(isButtonRole and readsAs("${side}에 넣을 항목 고르기, ${total}개 중 ${chosen}개 선택됨"))

    private fun frontButton(): SemanticsNodeInteraction = pickButton("앞면", total = 20, chosen = 1)

    private fun backButton(): SemanticsNodeInteraction = pickButton("뒷면", total = 20, chosen = 4)

    private fun visibleText(text: String): SemanticsNodeInteraction = composeRule.onNodeWithText(text, useUnmergedTree = true)

    private fun bounds(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().boundsInRoot

    private fun doneButton(): SemanticsNodeInteraction = composeRule.onNodeWithText("완료")

    // 다른 면에 있는 줄은 읽는 문구에 어느 면에 있는지가 붙을 수 있어 이름을 포함하는지로 찾는다.
    // 머리 줄의 가져오기 토글도 유형 이름에 같은 낱말이 있으면 체크 역할로 읽히므로 이름이 정확히 같은 줄만 찾는다.
    private fun sheetRow(name: String): SemanticsNodeInteraction = composeRule.onNode(isCheckboxRole and readsAs(name))

    @Test
    fun `항목이 열두 개면 고르기 버튼 없이 칩을 그린다`() {
        showWide(fieldCount = 12)

        composeRule.onAllNodes(hasText("항목 고르기", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(25)
    }

    @Test
    fun `항목이 열세 개가 되면 칩 대신 면마다 고르기 버튼을 보여준다`() {
        showWide(fieldCount = 13)

        visibleText("항목 고르기 (13개 중 1개)").assertIsDisplayed()
        visibleText("항목 고르기 (13개 중 4개)").assertIsDisplayed()
    }

    @Test
    fun `항목이 열세 개가 되면 가져오기 줄 말고는 체크 역할 줄이 없다`() {
        showWide(fieldCount = 13)

        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(1)
    }

    @Test
    fun `항목이 스무 개인 유형의 부제는 항목 20개를 보여준다`() {
        showWide()

        composeRule.onNodeWithText("노트 유형 · 1,204장 · 항목 20개").assertIsDisplayed()
    }

    @Test
    fun `앞면과 뒷면 라벨 아래에 고른 개수가 든 고르기 버튼을 보여준다`() {
        showWide()

        visibleText("앞면에 넣을 항목").assertIsDisplayed()
        visibleText("뒷면에 넣을 항목").assertIsDisplayed()
        visibleText("항목 고르기 (20개 중 1개)").assertIsDisplayed()
        visibleText("항목 고르기 (20개 중 4개)").assertIsDisplayed()
    }

    @Test
    fun `앞면 요약은 고른 항목 하나의 이름만 보여준다`() {
        showWide()

        visibleText("단어").assertIsDisplayed()
    }

    @Test
    fun `뒷면 요약은 고른 순서대로 이름 셋을 쓰고 넘는 수는 외 N개로 줄인다`() {
        showWide()

        visibleText("뜻, 예문, 읽기 외 1개").assertIsDisplayed()
    }

    @Test
    fun `고른 항목이 셋 이하면 외 N개 없이 이름만 쓴다`() {
        host.show(
            table = MappingStepTables.table(MappingStepTables.wide(fieldCount = 20)),
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(MappingStepTables.WIDE_ID, front = listOf("단어"), back = listOf("뜻", "예문", "읽기")),
                ),
        )

        visibleText("뜻, 예문, 읽기").assertIsDisplayed()
    }

    @Test
    fun `요약의 항목 이름은 80자로 줄여 그린다`() {
        val longName = "긴".repeat(100)
        host.show(
            table =
                MappingStepTables.table(
                    MappingStepTables.wide(fieldCount = 13, fieldNames = listOf(longName) + MappingStepTables.wideFieldNames(13).drop(1)),
                ),
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(MappingStepTables.WIDE_ID, front = listOf(longName), back = listOf("뜻")),
                ),
        )

        composeRule.onAllNodes(readsContaining(longName), useUnmergedTree = true).assertCountEquals(0)
        visibleText("긴".repeat(79) + "…").assertIsDisplayed()
    }

    @Test
    fun `뒷면 고르기 버튼은 면을 넣어 개수와 함께 읽는다`() {
        showWide()

        backButton().assertIsDisplayed()
    }

    @Test
    fun `앞면 고르기 버튼은 면을 넣어 개수와 함께 읽는다`() {
        showWide()

        frontButton().assertIsDisplayed()
    }

    @Test
    fun `빈칸 유형의 고르기 버튼은 빈칸 문장과 추가 설명을 면으로 읽는다`() {
        showWide(isCloze = true)

        composeRule.onNode(isButtonRole and readsAs("빈칸 문장에 넣을 항목 고르기, 20개 중 1개 선택됨")).assertIsDisplayed()
        composeRule.onNode(isButtonRole and readsAs("추가 설명에 넣을 항목 고르기, 20개 중 4개 선택됨")).assertIsDisplayed()
    }

    @Test
    fun `고르기 버튼의 높이는 48dp 이상이다`() {
        showWide()

        backButton().assertHeightIsAtLeast(48.dp)
        frontButton().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `보통 글자 크기에서는 요약과 버튼이 한 줄에 놓인다`() {
        showWide()

        val summary = bounds(visibleText("뜻, 예문, 읽기 외 1개"))
        val button = bounds(backButton())

        (summary.center.y >= button.top && summary.center.y <= button.bottom) shouldBe true
        (summary.right <= button.left) shouldBe true
    }

    @Test
    fun `글자를 200퍼센트로 키우면 요약이 위에 버튼이 아래에 쌓인다`() {
        showWide(fontScale = 2f)

        val summary = bounds(visibleText("뜻, 예문, 읽기 외 1개"))
        val button = bounds(composeRule.onNode(isButtonRole and readsContaining("뒷면에 넣을 항목 고르기")))

        (summary.bottom <= button.top + 1f) shouldBe true
    }

    @Test
    fun `글자를 200퍼센트로 키워도 고르기 버튼은 화면 너비 안에 있다`() {
        showWide(fontScale = 2f, width = 360.dp)

        val button = bounds(composeRule.onNode(isButtonRole and readsContaining("뒷면에 넣을 항목 고르기")))

        (button.right <= host.contentWidthPx) shouldBe true
    }

    @Test
    fun `고르기 버튼을 누르기 전에는 시트가 없다`() {
        showWide()

        backButton().assertIsDisplayed()
        doneButton().assertDoesNotExist()
    }

    @Test
    fun `뒷면 고르기 버튼을 누르면 뒷면에 넣을 항목 시트가 열린다`() {
        showWide()

        backButton().performClick()

        composeRule.onNode(hasText("뒷면에 넣을 항목") and isHeading).assertIsDisplayed()
        doneButton().assertIsDisplayed()
    }

    @Test
    fun `앞면 고르기 버튼을 누르면 앞면에 넣을 항목 시트가 열린다`() {
        showWide()

        frontButton().performClick()

        composeRule.onNode(hasText("앞면에 넣을 항목") and isHeading).assertIsDisplayed()
    }

    @Test
    fun `시트는 처음 열 때부터 끝까지 펼쳐 완료 버튼이 화면 안에 보인다`() {
        showWide()

        backButton().performClick()

        doneButton().assertIsDisplayed()
    }

    @Test
    fun `시트 줄은 뒷면 기준으로 고른 순서를 읽는다`() {
        showWide()

        backButton().performClick()

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertIsDisplayed()
    }

    @Test
    fun `시트에서 항목 줄을 누르면 그 유형과 면으로 선택 변경을 요청한다`() {
        showWide()
        backButton().performClick()

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.WIDE_ID, FieldMapping.BACK, "뜻"))
    }

    @Test
    fun `앞면 시트에서 줄을 누르면 앞면 기준으로 요청한다`() {
        showWide()
        frontButton().performClick()

        composeRule.onNode(chosenChip("단어", "앞면", 1)).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.WIDE_ID, FieldMapping.FRONT, "단어"))
    }

    @Test
    fun `완료를 누르면 시트가 닫힌다`() {
        showWide()
        backButton().performClick()

        doneButton().performClick()
        composeRule.waitForIdle()

        doneButton().assertDoesNotExist()
    }

    @Test
    fun `시트를 닫으면 열었던 고르기 버튼으로 포커스가 돌아온다`() {
        showWide()
        backButton().performClick()

        doneButton().performClick()
        composeRule.waitForIdle()

        backButton().assertIsFocused()
    }

    @Test
    fun `시트에서 고른 결과는 완료 뒤 요약과 버튼 개수에 반영된다`() {
        showWide(applyEdits = true)
        backButton().performClick()

        sheetRow("한자").performClick()
        doneButton().performClick()
        composeRule.waitForIdle()

        visibleText("뜻, 예문, 읽기 외 2개").assertIsDisplayed()
        visibleText("항목 고르기 (20개 중 5개)").assertIsDisplayed()
    }

    @Test
    fun `시트에서 앞면 항목을 뒷면으로 옮기면 앞면 요약에서 빠진다`() {
        showWide(applyEdits = true)
        backButton().performClick()

        sheetRow("단어").performClick()
        doneButton().performClick()
        composeRule.waitForIdle()

        visibleText("항목 고르기 (20개 중 0개)").assertIsDisplayed()
        visibleText("항목 고르기 (20개 중 5개)").assertIsDisplayed()
    }

    @Test
    fun `고르기 버튼이 있어도 세로 스크롤 컨테이너는 하나뿐이다`() {
        showWide()

        composeRule.onAllNodes(hasVerticalScrollRange).assertCountEquals(1)
    }

    @Test
    fun `고르기 버튼 유형의 가져오기 줄도 같은 방식으로 켜고 끈다`() {
        showWide()

        composeRule.onNode(hasText("가져오기")).performClick()

        host.typeToggles shouldBe listOf(MappingStepTables.WIDE_ID)
    }

    @Test
    fun `끈 유형의 고르기 버튼은 눌러도 시트를 열지 않는다`() {
        host.show(
            table = MappingStepTables.table(MappingStepTables.wide(20)),
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(
                        MappingStepTables.WIDE_ID,
                        front = listOf("단어"),
                        back = listOf("뜻", "예문", "읽기", "품사"),
                        excluded = true,
                    ),
                ),
        )

        composeRule.onNode(isButtonRole and readsContaining("뒷면에 넣을 항목 고르기")).performClick()

        doneButton().assertDoesNotExist()
    }
}
