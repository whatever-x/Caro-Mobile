package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.support.SettingsSamples
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 설정 단계가 Screen에서 종류별 화면과 제목, 실패 대화상자, 덮개, 단계별 보존 상태로 이어지는지 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDeckScreenSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)

    private val longSubdecks =
        SettingsSamples.subdeckState(
            SettingsSamples.subdeckTable.copy(subdecks = List(LONG_LIST) { index -> ParsedSubdeck("deck$index", "JLPT::덱 $index", 10) }),
        )
    private val longSheets =
        SettingsSamples.sheetState(
            SettingsSamples.sheetTable.copy(sheets = List(LONG_LIST) { index -> ParsedSheet(index, "시트 $index", 10) }),
        )

    private fun verticalOffset(): Float =
        composeRule
            .onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()

    private fun scrollUp() {
        composeRule
            .onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()
    }

    @Test
    fun `설정 종류마다 상단 제목이 다르다`() {
        val titles =
            listOf(
                SettingsSamples.subdeckState() to "하위 덱 고르기",
                SettingsSamples.sheetState() to "시트 고르기",
                SettingsSamples.delimiterState() to "구분 기준 확인",
                SettingsSamples.mappingState() to "카드 구성 정하기",
            )
        host.show(titles.first().first)

        titles.forEach { (state, title) ->
            host.update(state)
            composeRule.onNodeWithText(title).assertIsDisplayed()
        }
    }

    @Test
    fun `적용 뒤 미리보기를 만드는 동안 덮개에 만드는 중 문구와 파일 이름을 보인다`() {
        host.show(SettingsSamples.subdeckState().copy(loading = ImportLoading.BuildingPreview(SettingsSamples.ANKI_FILE)))
        composeRule.mainClock.advanceTimeBy(LOADING_VISIBLE_MILLIS)

        composeRule.onNodeWithText("미리보기를 만들고 있어요").assertIsDisplayed()
        composeRule.onNodeWithText(SettingsSamples.ANKI_FILE).assertIsDisplayed()
    }

    @Test
    fun `미리보기를 만들지 못하면 실패 대화상자를 보이고 확인을 누르면 닫기 입력을 보낸 뒤 그 화면에 머문다`() {
        val settings = SettingsSamples.mappingState()
        host.show(settings.copy(dialog = ImportDialog.PreviewFailed))

        composeRule.onNodeWithText("미리보기를 만들지 못했어요").assertIsDisplayed()
        composeRule.onNodeWithText("확인").performClick()
        host.update(settings)

        host.intents shouldBe listOf(ImportDeckIntent.DismissDialog)
        composeRule.onNodeWithText("카드 구성 정하기").assertIsDisplayed()
    }

    @Test
    fun `설정 종류가 바뀌면 지난 종류의 스크롤을 쓰지 않는다`() {
        host.show(longSubdecks)
        scrollUp()
        verticalOffset() shouldBeGreaterThan 0f

        host.update(longSheets)

        verticalOffset() shouldBe 0f
    }

    @Test
    fun `같은 설정 화면에서 다시 읽어 표가 바뀌어도 스크롤 위치를 보존한다`() {
        host.show(longSheets)
        scrollUp()
        val scrolled = verticalOffset()

        host.update(longSheets.copy(draftTable = longSheets.draftTable?.copy(sourceName = "다시 읽음.xlsx")))

        verticalOffset() shouldBe scrolled
    }

    @Test
    fun `바꾼 내용 없이 설정에서 돌아오면 보던 미리보기 자리로 돌아가고 새 미리보기면 처음부터 보인다`() {
        val preview = PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview)
        val settings = SettingsSamples.subdeckState().copy(preview = preview.preview)
        host.show(preview)
        scrollUp()
        val scrolled = verticalOffset()
        scrolled shouldBeGreaterThan 0f

        host.update(settings)
        host.update(preview)
        verticalOffset() shouldBe scrolled

        host.update(settings)
        host.update(preview.copy(preview = PreviewSamples.ankiPreview))
        verticalOffset() shouldBe 0f
    }

    @Test
    fun `먼저 고칠 것에서 처음으로 돌아가면 가져오기 방법 화면을 처음부터 보인다`() {
        host.show(ImportDeckState(), height = SHORT_SCREEN)
        scrollUp()
        verticalOffset() shouldBeGreaterThan 0f

        host.update(SettingsSamples.subdeckState(SettingsSamples.overLimitSubdeckTable, entry = SettingsEntry.Gate))
        host.update(ImportDeckState())

        verticalOffset() shouldBe 0f
    }

    private companion object {
        val SHORT_SCREEN = 400.dp
        const val LONG_LIST = 30
        const val LOADING_VISIBLE_MILLIS = 300L
    }
}
