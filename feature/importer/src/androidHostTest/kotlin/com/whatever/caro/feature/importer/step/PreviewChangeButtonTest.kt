package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.support.PreviewSamples
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.absoluteValue

/** 미리보기 설정 박스 줄의 [변경]과 설정 확인 띠의 [구분 기준 변경]이 해당 설정 화면을 여는 입력을 보내는지 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewChangeButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun `Anki 설정 박스는 덱 이름, 하위 덱, 카드 구성 줄에만 변경을 달고 줄 이름을 붙여 읽는다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNode(hasContentDescription("덱 이름 변경") and isButton).assertIsDisplayed()
        composeRule.onNode(hasContentDescription("하위 덱 변경") and isButton).assertIsDisplayed()
        composeRule.onNode(hasContentDescription("카드 구성 변경") and isButton).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("제외 변경").assertDoesNotExist()
    }

    @Test
    fun `덱 이름이 공백뿐이어도 덱 이름 줄과 변경을 보이고 줄은 줄 이름만 읽는다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview).copy(deckName = "   "))

        composeRule.onNode(hasContentDescription("덱 이름 변경") and isButton).assertIsDisplayed()
        composeRule
            .onNodeWithText("덱 이름")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("덱 이름"))))
    }

    @Test
    fun `덱 이름 줄의 변경을 누르면 덱 정보 시트를 여는 입력을 보낸다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNodeWithContentDescription("덱 이름 변경").performClick()

        host.intents shouldBe listOf(ImportDeckIntent.ClickChangeDeckInfo)
    }

    @Test
    fun `변경을 누르면 그 줄의 설정 화면을 여는 입력을 보낸다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.onNodeWithContentDescription("하위 덱 변경").performClick()
        composeRule.onNodeWithContentDescription("카드 구성 변경").performClick()

        host.intents shouldBe
            listOf(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck), ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
    }

    @Test
    fun `엑셀은 시트 줄, 텍스트는 구분 기준 줄의 변경으로 해당 설정을 연다`() {
        host.show(PreviewSamples.state(PreviewSamples.xlsxTable, PreviewSamples.xlsxPreview))
        composeRule.onNodeWithContentDescription("시트 변경").performClick()

        host.update(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.textPreview))
        composeRule.onNodeWithContentDescription("구분 기준 변경").performClick()

        host.intents shouldBe
            listOf(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet), ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
    }

    @Test
    fun `변경은 보이는 글자의 위아래 바깥까지 48dp 높이 안에서 눌리고 줄 높이는 48이다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))
        val button = composeRule.onNodeWithContentDescription("하위 덱 변경").bounds()
        val label = composeRule.onNodeWithText("하위 덱", useUnmergedTree = true).bounds()

        (button.height - SettingsScreenHost.MIN_TOUCH_HEIGHT).value.absoluteValue shouldBeLessThan HEIGHT_TOLERANCE
        host.tapTouchEdges(label.copy(left = button.left, right = button.right))

        host.intents shouldBe List(2) { ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck) }
    }

    @Test
    fun `변경은 보이는 글자의 좌우 바깥까지 48dp 폭 안에서 눌린다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))
        val button = composeRule.onNodeWithContentDescription("하위 덱 변경").bounds()

        (button.width >= SettingsScreenHost.MIN_TOUCH_HEIGHT) shouldBe true
        host.tapTouchSides(button)

        host.intents shouldBe List(2) { ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck) }
    }

    @Test
    fun `큰 글자에서도 변경은 줄 이름 오른쪽에 남는다`() {
        host.show(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview), fontScale = 1.3f)

        val label = composeRule.onNodeWithText("하위 덱", useUnmergedTree = true).bounds()
        val button = composeRule.onNodeWithContentDescription("하위 덱 변경").bounds()
        (button.left >= label.right) shouldBe true
        (button.top < label.bottom && button.bottom > label.top) shouldBe true
    }

    @Test
    fun `설정 확인 띠의 구분 기준 변경은 보이는 모양의 위아래 바깥까지 48dp 높이 안에서 눌린다`() {
        host.show(PreviewSamples.state(PreviewSamples.singleColumnTable, PreviewSamples.singleColumnPreview))
        val button = composeRule.onNode(hasText("구분 기준 변경") and isButton).bounds()
        val visible = composeRule.onNodeWithText("구분 기준 변경", useUnmergedTree = true).bounds()

        (button.height >= SettingsScreenHost.MIN_TOUCH_HEIGHT) shouldBe true
        host.tapTouchEdges(visible)

        host.intents shouldBe List(2) { ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter) }
    }

    private companion object {
        const val HEIGHT_TOLERANCE = 0.01f
    }
}
