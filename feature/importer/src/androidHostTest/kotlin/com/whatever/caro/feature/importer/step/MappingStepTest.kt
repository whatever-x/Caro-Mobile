package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.SettingsReason
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
class MappingStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val imageOcclusion = hasContentDescription("Image Occlusion, 12장, 가져올 수 없어요")

    private fun list() =
        composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))

    private val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    private fun typeHeader(name: String) = hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    private fun hasState(description: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    @Test
    fun `유형이 여럿이면 유형마다 이름과 장수, 항목 수 태그와 가져오기 체크를 보인다`() {
        host.show(SettingsSamples.mappingState())

        composeRule
            .onNode(typeHeader("일본어 단어") and hasText("1,204장") and hasText("항목 4개") and hasText("가져오기"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertIsDisplayed()
    }

    @Test
    fun `가져오기 체크를 누르면 유형 끄기 입력을, 칩을 누르면 항목 입력을 보낸다`() {
        host.show(SettingsSamples.mappingState())

        composeRule.onNode(typeHeader("일본어 단어")).performClick()
        composeRule.onAllNodesWithText("예문")[0].performClick()

        host.intents shouldBe
            listOf(
                ImportDeckIntent.ToggleNoteType(SettingsSamples.VOCAB_TYPE_ID),
                ImportDeckIntent.ToggleField(SettingsSamples.VOCAB_TYPE_ID, FieldMapping.FRONT, "예문"),
            )
    }

    @Test
    fun `항목 칩은 보이는 모양의 위아래 바깥까지 48dp 높이 안에서 눌린다`() {
        host.show(SettingsSamples.mappingState())

        host.tapTouchEdges(composeRule.onAllNodes(hasText("예문") and isCheckbox)[0].bounds())

        host.intents shouldBe List(2) { ImportDeckIntent.ToggleField(SettingsSamples.VOCAB_TYPE_ID, FieldMapping.FRONT, "예문") }
    }

    @Test
    fun `고른 칩은 이름만 글자로 읽고 선택됨과 면, 순서는 상태 설명 하나로 읽는다`() {
        host.show(SettingsSamples.mappingState())

        val chip = composeRule.onNode(hasText("읽기") and hasState("선택됨, 뒷면 3번째")).fetchSemanticsNode()

        chip.config[SemanticsProperties.Text].map { text -> text.text } shouldBe listOf("읽기")
        (SemanticsProperties.ContentDescription in chip.config) shouldBe false
    }

    @Test
    fun `끈 유형의 칩은 꺼진 상태로만 알리고 사용할 수 없다는 상태 설명을 겹쳐 붙이지 않는다`() {
        val mappings = SettingsSamples.mappingState().draftMappings.toMutableMap()
        mappings[SettingsSamples.VOCAB_TYPE_ID] = mappings.getValue(SettingsSamples.VOCAB_TYPE_ID).copy(excluded = true)
        host.show(SettingsSamples.mappingState(mappings = mappings))

        composeRule.onNode(hasText("단어") and hasState("선택됨, 앞면 1번째")).assertIsNotEnabled()
        val unchosen = composeRule.onAllNodes(hasText("예문") and isCheckbox)[0].assertIsNotEnabled().fetchSemanticsNode()
        (SemanticsProperties.StateDescription in unchosen.config) shouldBe false
    }

    @Test
    fun `켜진 유형은 첫 카드 미리보기를 보이고 끈 유형은 숨기며 칩을 누를 수 없다`() {
        val mappings = SettingsSamples.mappingState().draftMappings.toMutableMap()
        mappings[SettingsSamples.VOCAB_TYPE_ID] = mappings.getValue(SettingsSamples.VOCAB_TYPE_ID).copy(excluded = true)
        host.show(SettingsSamples.mappingState(mappings = mappings))

        composeRule.onNode(hasText("学校") and hasText("학교", substring = true)).assertDoesNotExist()
        composeRule.onAllNodesWithText("예문")[0].assertIsNotEnabled()
        list().performScrollToNode(hasText("水"))
        composeRule.onNode(hasText("水") and hasText("물")).assertIsDisplayed()
    }

    @Test
    fun `Image Occlusion 묶음은 맨 아래에서 이름, 장수, 가져올 수 없다는 상태를 한 문장으로 읽는다`() {
        host.show(SettingsSamples.mappingState())
        list().performScrollToNode(imageOcclusion)

        val basic = composeRule.onNode(typeHeader("기본")).bounds()
        val io = composeRule.onNode(imageOcclusion).assertIsDisplayed().bounds()
        (io.top > basic.bottom) shouldBe true
    }

    // 묶음은 clearAndSetSemantics로 하위를 지워 병합 트리에는 접근성 설명만 남으므로, 화면에 그린 글자는 병합 전 트리에서만 찾을 수 있다.
    @Test
    fun `Image Occlusion 묶음 안에 가져올 수 없다는 글자가 화면에 보인다`() {
        host.show(SettingsSamples.mappingState())
        list().performScrollToNode(imageOcclusion)

        composeRule
            .onNode(hasText("가져올 수 없어요") and hasAnyAncestor(imageOcclusion), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `가져올 수 있는 유형이 하나면 Image Occlusion이 있어도 유형 이름은 보이고 가져오기 체크는 두지 않는다`() {
        host.show(SettingsSamples.mappingState(SettingsSamples.imageOcclusionMappingTable))

        composeRule.onNodeWithText("기본").assertIsDisplayed()
        composeRule.onNodeWithText("200장").assertIsDisplayed()
        composeRule.onNodeWithText("가져오기").assertDoesNotExist()
        composeRule.onNode(typeHeader("기본")).assertDoesNotExist()
    }

    @Test
    fun `유형이 하나면 유형 머리와 가져오기 체크 없이 항목과 첫 카드만 보인다`() {
        host.show(SettingsSamples.mappingState(SettingsSamples.singleTypeTable))

        composeRule.onNodeWithText("가져오기").assertDoesNotExist()
        composeRule.onNode(hasText("abandon") and hasText("버리다", substring = true)).assertIsDisplayed()
    }

    @Test
    fun `모든 유형을 끄면 버튼 위에 가져오기를 체크하라는 사유를 보이고 적용을 끈다`() {
        host.show(SettingsSamples.mappingState(mappings = SettingsSamples.excluded(SettingsSamples.mappingTable)))

        composeRule.onNodeWithText("‘가져오기’를 하나 이상 체크해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("적용").assertIsNotEnabled()
    }

    @Test
    fun `적용 뒤 카드가 없거나 상한을 넘으면 그 사유를 보인다`() {
        host.show(SettingsSamples.withReason(SettingsSamples.mappingState(), SettingsReason.NoCards))
        composeRule.onNodeWithText("가져올 카드가 없어요. 내용이 있는 항목을 앞뒷면에 넣어 주세요.").assertIsDisplayed()

        host.update(SettingsSamples.withReason(SettingsSamples.mappingState(), SettingsReason.TooManyCards))
        composeRule.onNodeWithText("한 번에 10,000장까지 가져올 수 있어요.").assertIsDisplayed()
        composeRule.onNodeWithText("적용").assertIsNotEnabled()
    }

    @Test
    fun `항목이 많은 유형은 고른 항목 요약과 항목 고르기 버튼을 보인다`() {
        host.show(SettingsSamples.mappingState(SettingsSamples.manyFieldsTable, SettingsSamples.manyFieldsMapping))

        composeRule.onNodeWithText("뜻, 예문, 읽기 외 1개").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒷면에 넣을 항목 고르기, 20개 중 4개 선택됨").assertIsDisplayed()
    }

    @Test
    fun `항목 고르기 시트를 열어 항목을 고르면 항목 입력을 보내고 완료를 누르면 닫힌다`() {
        host.show(SettingsSamples.mappingState(SettingsSamples.manyFieldsTable, SettingsSamples.manyFieldsMapping))

        composeRule.onNodeWithContentDescription("뒷면에 넣을 항목 고르기, 20개 중 4개 선택됨").performClick()
        composeRule.onNodeWithText("고른 순서대로 줄을 바꿔 합쳐요.").assertIsDisplayed()
        composeRule.onNode(hasText("한자") and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)).performClick()
        composeRule.onNodeWithText("완료").performClick()

        composeRule.onNodeWithText("고른 순서대로 줄을 바꿔 합쳐요.").assertDoesNotExist()
        host.intents shouldBe listOf(ImportDeckIntent.ToggleField(SettingsSamples.MANY_TYPE_ID, FieldMapping.BACK, "한자"))
    }
}
