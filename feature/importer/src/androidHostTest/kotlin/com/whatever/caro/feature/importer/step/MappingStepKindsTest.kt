package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.model.importing.FieldMapping
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
class MappingStepKindsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { MappingStepHost(composeRule) }

    private fun showCloze() = host.show(table = MappingStepTables.table(MappingStepTables.cloze()))

    private fun showImageOcclusionFirst() =
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion(), MappingStepTables.vocab()))

    // 카드 전체를 한 문장으로 읽게 하면 안쪽 글자 semantics가 병합 트리에서 빠지므로 글자는 병합하지 않은 트리로 찾는다.
    private fun textNode(text: String): SemanticsNodeInteraction = composeRule.onNodeWithText(text, useUnmergedTree = true)

    private fun top(text: String): Float = textNode(text).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun `빈칸 유형은 빈칸 카드로 자동으로 만든다는 안내 상자를 보여준다`() {
        showCloze()

        composeRule.onNodeWithText("빈칸 카드로 자동으로 만들어요").assertIsDisplayed()
        composeRule.onNodeWithText("앞면은 빈칸을 [...]로 가린 문장, 뒷면은 빈칸을 채운 문장과 추가 설명이에요.").assertIsDisplayed()
    }

    @Test
    fun `빈칸 유형이 아니면 안내 상자가 없다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.vocab()))

        composeRule.onNode(chosenChip("단어", "앞면", 1)).assertIsDisplayed()
        composeRule.onNodeWithText("빈칸 카드로 자동으로 만들어요").assertDoesNotExist()
    }

    @Test
    fun `빈칸 유형은 앞면과 뒷면 대신 빈칸 문장과 추가 설명 라벨을 쓴다`() {
        showCloze()

        composeRule.onNodeWithText("빈칸 문장에 넣을 항목").assertIsDisplayed()
        composeRule.onNodeWithText("추가 설명에 넣을 항목").assertIsDisplayed()
        composeRule.onNodeWithText("앞면에 넣을 항목").assertDoesNotExist()
        composeRule.onNodeWithText("뒷면에 넣을 항목").assertDoesNotExist()
    }

    @Test
    fun `빈칸 유형의 안내 상자는 라벨 위에 있다`() {
        showCloze()

        (top("빈칸 카드로 자동으로 만들어요") < top("빈칸 문장에 넣을 항목")) shouldBe true
    }

    @Test
    fun `빈칸 유형의 부제는 노트 수와 항목 수를 보여준다`() {
        showCloze()

        composeRule.onNodeWithText("노트 유형 · 110장 · 항목 2개").assertIsDisplayed()
    }

    @Test
    fun `빈칸 유형의 첫 항목은 빈칸 문장 1번째로 읽는다`() {
        showCloze()

        composeRule.onNode(chosenChip("텍스트", "빈칸 문장", 1)).assertIsDisplayed()
    }

    @Test
    fun `빈칸 유형의 나머지 항목은 추가 설명 1번째로 읽는다`() {
        showCloze()

        composeRule.onNode(chosenChip("뒷면 추가", "추가 설명", 1)).assertIsDisplayed()
    }

    @Test
    fun `빈칸 문장에도 칩을 여러 개 골라 고른 순서를 읽는다`() {
        host.show(
            table = MappingStepTables.table(MappingStepTables.cloze()),
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(MappingStepTables.CLOZE_ID, front = listOf("뒷면 추가", "텍스트"), back = emptyList()),
                ),
        )

        composeRule.onNode(chosenChip("뒷면 추가", "빈칸 문장", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("텍스트", "빈칸 문장", 2)).assertIsDisplayed()
    }

    @Test
    fun `빈칸 유형의 칩을 눌러도 앞면 뒷면 기준 키로 요청한다`() {
        showCloze()

        composeRule.onNode(plainChip("텍스트")).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.CLOZE_ID, FieldMapping.BACK, "텍스트"))
    }

    @Test
    fun `이미지 가리기 유형은 이름과 노트 수를 보여준다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        textNode("Image Occlusion").assertIsDisplayed()
        textNode("노트 유형 · 40장").assertIsDisplayed()
    }

    @Test
    fun `이미지 가리기 유형은 가져올 수 없다는 이유만 보여준다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        textNode("이미지 가리기 카드는 가져올 수 없어요.").assertIsDisplayed()
    }

    @Test
    fun `이미지 가리기 유형에는 가져오기 토글이 없다`() {
        showImageOcclusionFirst()

        composeRule.onAllNodes(hasText("가져오기")).assertCountEquals(1)
    }

    @Test
    fun `이미지 가리기 유형만 있으면 체크 역할 줄이 하나도 없다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        textNode("이미지 가리기 카드는 가져올 수 없어요.").assertIsDisplayed()
        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(0)
    }

    @Test
    fun `이미지 가리기 유형만 있으면 항목 연결 라벨도 없다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        textNode("이미지 가리기 카드는 가져올 수 없어요.").assertIsDisplayed()
        composeRule.onNodeWithText("앞면에 넣을 항목").assertDoesNotExist()
        composeRule.onNodeWithText("뒷면에 넣을 항목").assertDoesNotExist()
    }

    @Test
    fun `이미지 가리기 카드는 이름과 노트 수와 이유를 한 번에 읽는다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        composeRule.onNode(readsAs("Image Occlusion, 노트 유형 40장, 이미지 가리기 카드는 가져올 수 없어요.")).assertIsDisplayed()
    }

    @Test
    fun `이미지 가리기 카드는 누를 수 있는 요소가 없다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        composeRule.onNode(readsAs("Image Occlusion, 노트 유형 40장, 이미지 가리기 카드는 가져올 수 없어요.")).assertHasNoClickAction()
    }

    @Test
    fun `이미지 가리기 유형은 표의 순서와 상관없이 유형 목록 맨 위에 둔다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.vocab(), MappingStepTables.imageOcclusion()))

        (top("Image Occlusion") < top("일본어 단어")) shouldBe true
    }

    @Test
    fun `이미지 가리기 유형은 고른 범위의 노트 수를 쓴다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion(noteCount = 40, selectedNoteCount = 12)))

        textNode("노트 유형 · 12장").assertIsDisplayed()
    }

    @Test
    fun `이미지 가리기 유형이 있어도 다른 유형은 그대로 연결을 그린다`() {
        showImageOcclusionFirst()

        composeRule.onNode(chosenChip("단어", "앞면", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertIsDisplayed()
    }
}
