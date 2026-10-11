package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 가져오기 줄은 머리 줄 어디를 눌러도 켜고 꺼지고, 스크린 리더가 어느 유형의 토글인지 함께 읽어야 한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class MappingStepHeaderToggleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { MappingStepHost(composeRule) }

    private val vocabTable = MappingStepTables.table(MappingStepTables.vocab())

    private fun headerToggle(): SemanticsNodeInteraction = composeRule.onNode(isCheckboxRole and hasText("가져오기"))

    private fun subtitle(): SemanticsNodeInteraction = composeRule.onNode(hasText("노트 유형", substring = true))

    @Test
    fun `가져오기 줄을 읽으면 노트 유형 이름이 함께 들어간다`() {
        host.show(vocabTable)

        headerToggle().fetchSemanticsNode().spokenParts() shouldContain MappingStepTables.VOCAB_NAME
    }

    @Test
    fun `가져오기 줄을 읽으면 노트 수와 항목 수 부제도 함께 들어간다`() {
        host.show(vocabTable)

        headerToggle().fetchSemanticsNode().spokenParts() shouldContain "노트 유형 · 1,204장 · 항목 4개"
    }

    @Test
    fun `노트 유형 이름을 눌러도 가져오기를 켜고 끈다`() {
        host.show(vocabTable)

        composeRule.onNode(hasText(MappingStepTables.VOCAB_NAME)).performClick()

        host.typeToggles shouldBe listOf(MappingStepTables.VOCAB_ID)
    }

    @Test
    fun `노트 수와 항목 수 부제를 눌러도 가져오기를 켜고 끈다`() {
        host.show(vocabTable)

        subtitle().performClick()

        host.typeToggles shouldBe listOf(MappingStepTables.VOCAB_ID)
    }

    @Test
    fun `머리 줄 전체가 하나의 체크 역할 줄이라 상태가 한 곳에서 바뀐다`() {
        host.show(vocabTable)

        headerToggle().fetchSemanticsNode().config[SemanticsProperties.ToggleableState] shouldBe ToggleableState.On
    }

    @Test
    fun `머리 줄의 눌리는 높이는 48dp 이상이다`() {
        host.show(vocabTable)

        headerToggle().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `큰 글자에서도 노트 유형 이름을 누르면 가져오기를 켜고 끈다`() {
        host.show(vocabTable, fontScale = LARGE_FONT_SCALE)

        composeRule.onNode(hasText(MappingStepTables.VOCAB_NAME)).performClick()

        host.typeToggles shouldBe listOf(MappingStepTables.VOCAB_ID)
    }

    @Test
    fun `큰 글자에서도 가져오기 줄을 읽으면 노트 유형 이름이 함께 들어간다`() {
        host.show(vocabTable, fontScale = LARGE_FONT_SCALE)

        headerToggle().fetchSemanticsNode().spokenParts() shouldContain MappingStepTables.VOCAB_NAME
    }

    private companion object {
        const val LARGE_FONT_SCALE = 1.6f
    }
}
