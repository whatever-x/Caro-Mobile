package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.ImportTestTables
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

// 화면이 rememberSaveable에 Android Bundle에 넣을 수 없는 값을 넣으면 저장 시점에 예외가 나 앱이 종료된다. 활동의 저장 가능 판정을 그대로 쓰는 StateRestorationTester로 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDeckScreenRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var state by mutableStateOf(ImportDeckState())
    private val hasHorizontalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

    private fun horizontalOffset(): Float =
        composeRule
            .onNode(hasHorizontalScroll)
            .fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange]
            .value()

    private fun showWithRestorationTester(initial: ImportDeckState): StateRestorationTester {
        state = initial
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CaroTheme {
                ImportDeckScreen(state = state, onIntent = {})
            }
        }
        return restorationTester
    }

    @Test
    fun `붙여넣기 단계에서 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        val restorationTester = showWithRestorationTester(ImportDeckState(step = ImportStep.Paste, isPaste = true, pasteText = "학교\t학교"))

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    @Test
    fun `단계를 바꾼 뒤 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        val restorationTester = showWithRestorationTester(ImportDeckState())

        composeRule.runOnIdle { state = ImportDeckState(step = ImportStep.Paste, isPaste = true) }
        composeRule.runOnIdle { state = ImportDeckState(step = ImportStep.Source) }
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    @Test
    fun `미리보기와 설정 화면을 오간 뒤 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        val restorationTester = showWithRestorationTester(PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview))

        composeRule.runOnIdle { state = SettingsSamples.mappingState(SettingsSamples.manyFieldsTable, SettingsSamples.manyFieldsMapping) }
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    @Test
    fun `구분 기준 확인에서 가로로 민 표 위치는 상태를 저장했다 복원해도 남는다`() {
        val restorationTester = showWithRestorationTester(SettingsSamples.delimiterState(ImportTestTables.text(rows = 4, columns = 3)))
        composeRule.onNode(hasHorizontalScroll).performTouchInput { swipeLeft() }
        val scrolled = horizontalOffset()
        scrolled shouldBeGreaterThan 0f

        restorationTester.emulateSavedInstanceStateRestore()

        horizontalOffset() shouldBe scrolled
    }

    @Test
    fun `읽는 중에 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        composeRule.mainClock.autoAdvance = false
        val restorationTester = showWithRestorationTester(ImportDeckState(loading = ImportLoading.ReadingFile(fileName = "deck.apkg")))

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    private companion object {
        const val BACK_DESCRIPTION = "뒤로 가기"
    }
}
