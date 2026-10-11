package com.whatever.caro.feature.importer.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 단계 상태 보존 키가 Android Bundle에 넣을 수 없는 타입이면 저장 시점에 죽는다. 활동의 저장 가능 판정을 그대로 쓰는 StateRestorationTester로 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDeckScreenRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var state by mutableStateOf(ImportDeckState())

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
        val restorationTester = showWithRestorationTester(ImportDeckState(step = ImportStep.Paste, isPaste = true))

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    @Test
    fun `단계를 바꾼 뒤 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        val restorationTester = showWithRestorationTester(ImportDeckState())

        composeRule.runOnIdle { state = ImportDeckState(step = ImportStep.Paste, isPaste = true) }
        composeRule.runOnIdle { state = ImportDeckState(step = ImportStep.Mapping) }
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    @Test
    fun `읽는 중에 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        composeRule.mainClock.autoAdvance = false
        val restorationTester = showWithRestorationTester(ImportDeckState(loading = ImportLoading.Parsing(fileName = "deck.apkg")))

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertExists()
    }

    private companion object {
        const val BACK_DESCRIPTION = "뒤로 가기"
    }
}
