package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.support.PreviewSamples
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 미리보기의 출처 요약, 가져오기 설정 박스, 경고 줄, 설정 확인 띠를 화면에서 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepSummaryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = PreviewScreenHost(composeRule)

    private val banner = "3줄이 칸으로 나뉘지 않았어요.\n구분 기준이 맞는지 확인해 주세요."

    @Test
    fun `파일 이름과 가져올 카드 장수를 보이고 장수 제목은 라벨과 함께 한 문장으로 읽는다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNodeWithText(PreviewSamples.ANKI_FILE).assertIsDisplayed()
        composeRule
            .onNode(hasContentDescription("가져올 카드 6장") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rUS-w393dp-h852dp-xxhdpi")
    fun `영어에서는 장수 제목을 영어 어순의 한 문장으로 읽는다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule
            .onNode(hasContentDescription("6 cards to import") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
    }

    @Test
    fun `붙여넣기는 파일 이름 대신 붙여 넣은 내용이라고 보인다`() {
        host.show(PreviewSamples.state(PreviewSamples.pasteTable, PreviewSamples.pastePreview, isPaste = true))

        composeRule.onNodeWithText("붙여 넣은 내용").assertIsDisplayed()
        composeRule.onNodeWithText(PreviewSamples.PASTE_DECK_NAME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `Anki 설정 박스는 덱 이름, 하위 덱, 카드 구성, 제외 줄을 줄 이름과 값 태그로 보인다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNode(hasText("덱 이름") and hasText("JLPT N5")).assertIsDisplayed()
        composeRule.onNode(hasText("하위 덱") and hasText("2개 모두") and hasText("한 덱으로 합침")).assertIsDisplayed()
        composeRule.onNode(hasText("카드 구성") and hasText("2개 유형") and hasText("앞뒷면 자동 지정")).assertIsDisplayed()
        composeRule.onNode(hasText("제외") and hasText("빈 카드 2장") and hasText("Image Occlusion 카드 12장")).assertIsDisplayed()
    }

    @Test
    fun `엑셀 설정 박스는 시트 이름과 제목 행, 앞뒷면 항목, 다른 시트 수를 보인다`() {
        host.show(PreviewSamples.state(PreviewSamples.xlsxTable, PreviewSamples.xlsxPreview))

        composeRule.onNode(hasText("덱 이름") and hasText("토익 단어장 - Day1")).assertIsDisplayed()
        composeRule.onNode(hasText("시트") and hasText("Day1") and hasText("첫 줄은 열 이름")).assertIsDisplayed()
        composeRule.onNode(hasText("카드 구성") and hasText("앞면 단어") and hasText("뒷면 뜻, 예문")).assertIsDisplayed()
        composeRule.onNode(hasText("제외") and hasText("빈 카드 3장") and hasText("다른 시트 2개")).assertIsDisplayed()
    }

    @Test
    fun `텍스트 설정 박스는 이름 없는 열을 번호로 보이고 제외할 카드가 없으면 제외 줄을 숨긴다`() {
        host.show(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.textPreview))

        composeRule.onNode(hasText("구분 기준") and hasText("쉼표") and hasText("첫 줄도 카드")).assertIsDisplayed()
        composeRule.onNode(hasText("카드 구성") and hasText("앞면 열 1") and hasText("뒷면 열 2")).assertIsDisplayed()
        composeRule.onNodeWithText("제외").assertDoesNotExist()
    }

    @Test
    fun `보통 글자에서는 값이 줄 이름 오른쪽에 놓인다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        val label = composeRule.onNodeWithText("하위 덱", useUnmergedTree = true).bounds()
        val value = composeRule.onNodeWithText("2개 모두", useUnmergedTree = true).bounds()
        (value.left >= label.right) shouldBe true
        (value.top < label.bottom) shouldBe true
    }

    @Test
    fun `큰 글자에서는 값을 줄 이름 아래로 내린다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview), fontScale = 1.3f)

        val label = composeRule.onNodeWithText("하위 덱", useUnmergedTree = true).bounds()
        val value = composeRule.onNodeWithText("2개 모두", useUnmergedTree = true).bounds()
        (value.top >= label.bottom) shouldBe true
    }

    @Test
    fun `덱 이름이 길면 한 줄로 말줄임한다`() {
        val longName = "JLPT N5 단어 2026년 봄학기 기초 반 복습용 모음 ".repeat(2).trim()
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview).copy(deckName = longName))

        val layout = composeRule.onNodeWithText(longName, useUnmergedTree = true).textLayout()
        layout.lineCount shouldBe 1
        layout.hasVisualOverflow shouldBe true
    }

    @Test
    fun `경고 줄은 정해진 순서로 설정 박스와 카드 목록 사이에 놓인다`() {
        val preview = PreviewSamples.preview(PreviewSamples.xlsxCards, excluded = 3, warnings = PreviewSamples.xlsxWarnings)
        host.show(PreviewSamples.state(PreviewSamples.xlsxTable, preview))

        val settings = composeRule.onNodeWithText("가져오기 설정").bounds()
        val formula = composeRule.onNodeWithText("값이 저장되지 않은 수식 3개는 비워서 가져와요.", substring = true).bounds()
        val number = composeRule.onNodeWithText("일부 숫자는 엑셀에서 보이던 모양과 다르게", substring = true).bounds()
        val listTitle = composeRule.onNodeWithText("카드 목록").bounds()
        (settings.bottom < formula.top) shouldBe true
        (formula.bottom <= number.top) shouldBe true
        (number.bottom < listTitle.top) shouldBe true
    }

    @Test
    fun `절반 이상의 줄이 한 칸인 텍스트는 설정 확인 띠를 출처 요약과 설정 박스 사이에 보인다`() {
        host.show(PreviewSamples.state(PreviewSamples.singleColumnTable, PreviewSamples.singleColumnPreview))

        val count = composeRule.onNodeWithText("3장", useUnmergedTree = true).bounds()
        val bannerBounds = composeRule.onNodeWithText(banner).assertIsDisplayed().bounds()
        val settings = composeRule.onNodeWithText("가져오기 설정").bounds()
        (count.bottom < bannerBounds.top) shouldBe true
        (bannerBounds.bottom < settings.top) shouldBe true
    }

    @Test
    fun `칸이 잘 나뉜 텍스트에는 설정 확인 띠가 없다`() {
        host.show(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.textPreview))
        composeRule.onNodeWithContentDescription("안내 닫기").assertDoesNotExist()
    }

    @Test
    fun `띠를 닫으면 닫기 입력을 보내고 필터를 바꾸거나 카드 전체 보기를 열고 닫아도 다시 보이지 않는다`() {
        host.show(PreviewSamples.state(PreviewSamples.singleColumnTable, PreviewSamples.singleColumnPreview))

        composeRule.onNodeWithContentDescription("안내 닫기").performClick()
        composeRule.onNodeWithText(banner).assertDoesNotExist()
        composeRule.onNodeWithText("빈 면").performClick()
        composeRule.onNode(cardItem(PreviewSamples.singleColumnCards[0])).performClick()
        composeRule.onNodeWithText("닫기").performClick()

        composeRule.onNodeWithText(banner).assertDoesNotExist()
        host.intents.first() shouldBe ImportDeckIntent.DismissDelimiterBanner
        host.state.previewFilter shouldBe ImportPreviewFilter.EmptyReplaced
    }
}
