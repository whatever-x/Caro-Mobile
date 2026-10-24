package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.model.importing.FieldMapping
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.PersistentMap
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
class MappingStepBlockedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { MappingStepHost(composeRule) }

    private val vocabTable = MappingStepTables.table(MappingStepTables.vocab())

    private fun showVocab(
        mappings: PersistentMap<String, FieldMapping> = MappingStepTables.defaultMappings(vocabTable),
        applyEdits: Boolean = false,
    ) = host.show(table = vocabTable, mappings = mappings, applyEdits = applyEdits)

    private fun vocabMapping(
        front: List<String> = listOf("단어"),
        back: List<String> = listOf("뜻", "예문", "읽기"),
        excluded: Boolean = false,
    ) = MappingStepTables.mappingsOf(MappingStepTables.mapping(MappingStepTables.VOCAB_ID, front, back, excluded))

    private fun previewButton(): SemanticsNodeInteraction = composeRule.onNodeWithText(PREVIEW)

    private fun bounds(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().boundsInRoot

    @Test
    fun `가져올 유형을 모두 끄면 미리보기 버튼이 꺼진다`() {
        showVocab(mappings = vocabMapping(excluded = true))

        previewButton().assertIsNotEnabled()
    }

    @Test
    fun `가져올 유형을 모두 끄면 가져올 노트 유형을 고르라는 이유 줄을 보여준다`() {
        showVocab(mappings = vocabMapping(excluded = true))

        composeRule.onNodeWithText(REASON_NO_TYPE).assertIsDisplayed()
    }

    @Test
    fun `켠 유형의 양쪽 면이 모두 비어 있으면 미리보기 버튼이 꺼진다`() {
        showVocab(mappings = vocabMapping(front = emptyList(), back = emptyList()))

        previewButton().assertIsNotEnabled()
    }

    @Test
    fun `켠 유형의 양쪽 면이 모두 비어 있으면 항목을 하나 이상 고르라는 이유 줄을 보여준다`() {
        showVocab(mappings = vocabMapping(front = emptyList(), back = emptyList()))

        composeRule.onNodeWithText(REASON_NO_FIELD).assertIsDisplayed()
    }

    @Test
    fun `켠 유형이 모두 양쪽 면이 비어 있어도 하나라도 항목이 있는 유형이 있으면 막지 않는다`() {
        host.show(
            table = MappingStepTables.table(MappingStepTables.vocab(), MappingStepTables.vocab(id = "grammar", name = "문법")),
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(MappingStepTables.VOCAB_ID, front = emptyList(), back = emptyList()),
                    MappingStepTables.mapping("grammar", front = listOf("단어"), back = emptyList()),
                ),
        )

        previewButton().assertIsEnabled()
        composeRule.onNodeWithText(REASON_NO_FIELD).assertDoesNotExist()
    }

    @Test
    fun `가져올 수 있는 유형이 하나도 없으면 미리보기 버튼이 꺼진다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        previewButton().assertIsNotEnabled()
    }

    @Test
    fun `가져올 수 있는 유형이 하나도 없으면 가져올 수 있는 노트 유형이 없다는 이유 줄을 보여준다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.imageOcclusion()))

        composeRule.onNodeWithText(REASON_NOTHING_IMPORTABLE).assertIsDisplayed()
    }

    @Test
    fun `한쪽 면만 비어 있으면 막지 않고 이유 줄도 없다`() {
        showVocab(mappings = vocabMapping(front = listOf("단어"), back = emptyList()))

        previewButton().assertIsEnabled()
        composeRule.onNodeWithText(REASON_NO_FIELD).assertDoesNotExist()
    }

    @Test
    fun `연결이 있으면 이유 줄이 하나도 없다`() {
        showVocab()

        previewButton().assertIsEnabled()
        listOf(REASON_NO_TYPE, REASON_NO_FIELD, REASON_NOTHING_IMPORTABLE).forEach { reason ->
            composeRule.onNodeWithText(reason).assertDoesNotExist()
        }
    }

    @Test
    fun `미리보기 버튼을 누르면 다음을 한 번 요청한다`() {
        showVocab()

        previewButton().assertIsEnabled()
        previewButton().performClick()

        host.nextClicks shouldBe 1
    }

    @Test
    fun `꺼진 미리보기 버튼을 눌러도 다음을 요청하지 않는다`() {
        showVocab(mappings = vocabMapping(excluded = true))

        previewButton().performClick()

        host.nextClicks shouldBe 0
    }

    @Test
    fun `꺼진 미리보기 버튼은 이유를 상태 설명으로 함께 읽는다`() {
        showVocab(mappings = vocabMapping(excluded = true))

        previewButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe REASON_NO_TYPE
    }

    @Test
    fun `켜진 미리보기 버튼은 상태 설명이 없다`() {
        showVocab()

        previewButton().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `이유 줄은 polite live region이라 나타날 때 알린다`() {
        showVocab(mappings = vocabMapping(excluded = true))

        composeRule.onNode(hasText(REASON_NO_TYPE) and isPoliteRegion).assertIsDisplayed()
    }

    @Test
    fun `이유 줄은 미리보기 버튼 바로 위에 있다`() {
        showVocab(mappings = vocabMapping(excluded = true))

        val reason = bounds(composeRule.onNodeWithText(REASON_NO_TYPE))
        val button = bounds(previewButton())

        (reason.bottom <= button.top) shouldBe true
    }

    @Test
    fun `이유 줄은 목록과 함께 스크롤되지 않고 고정되어 있다`() {
        host.show(
            table = MappingStepTables.table(*manyTypes().toTypedArray()),
            mappings = MappingStepTables.mappingsOf(*manyTypes().map { type -> excludedMapping(type.id) }.toTypedArray()),
            height = 520.dp,
        )
        val before = bounds(composeRule.onNodeWithText(REASON_NO_TYPE))

        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        bounds(composeRule.onNodeWithText(REASON_NO_TYPE)) shouldBe before
    }

    @Test
    fun `가져오기를 다시 켜면 이유 줄이 사라지고 미리보기 버튼이 켜진다`() {
        showVocab(mappings = vocabMapping(excluded = true), applyEdits = true)

        composeRule.onNode(hasText(IMPORT_TOGGLE)).performClick()

        composeRule.onNodeWithText(REASON_NO_TYPE).assertDoesNotExist()
        previewButton().assertIsEnabled()
    }

    @Test
    fun `마지막 켠 유형을 끄면 이유 줄이 나타나고 미리보기 버튼이 꺼진다`() {
        showVocab(applyEdits = true)

        composeRule.onNode(hasText(IMPORT_TOGGLE)).performClick()

        composeRule.onNodeWithText(REASON_NO_TYPE).assertIsDisplayed()
        previewButton().assertIsNotEnabled()
    }

    @Test
    fun `이미지 가리기 유형이 있어도 가져올 유형이 있으면 막지 않는다`() {
        val table = MappingStepTables.table(MappingStepTables.imageOcclusion(), MappingStepTables.vocab())
        host.show(table = table)

        previewButton().assertIsEnabled()
    }

    private fun manyTypes() = List(6) { index -> MappingStepTables.vocab(id = "type$index", name = "유형 $index") }

    private fun excludedMapping(id: String) = MappingStepTables.mapping(id, front = listOf("단어"), back = listOf("뜻"), excluded = true)

    private companion object {
        const val PREVIEW = "미리보기"
        const val IMPORT_TOGGLE = "가져오기"
        const val REASON_NO_TYPE = "가져올 노트 유형을 하나 이상 골라 주세요."
        const val REASON_NO_FIELD = "앞면이나 뒷면에 넣을 항목을 하나 이상 골라 주세요."
        const val REASON_NOTHING_IMPORTABLE = "가져올 수 있는 노트 유형이 없어요."
    }
}
