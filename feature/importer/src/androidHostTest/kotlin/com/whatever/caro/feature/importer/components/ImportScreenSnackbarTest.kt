package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.designsystem.components.CaroSnackbarVisuals
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.support.SettingsSamples
import io.kotest.matchers.floats.shouldBeLessThan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// 전역 스낵바 호스트는 홈에만 하단 여백을 주므로, 가져오기 화면의 스낵바는 화면 안 호스트가 하단 버튼 위 16에 띄운다.
// 화면 안 스낵바는 미리보기의 입력 거절 안내라 덱 만드는 중 덮개나 다른 단계 위에 남지 않아야 한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportScreenSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var screenState by mutableStateOf(ImportDeckState())

    // 값이 바뀔 때마다 스낵바를 한 번 띄운다. 0이면 띄우지 않는다.
    private var snackbarRequest by mutableStateOf(0)
    private val preview = PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview)

    private fun show(state: ImportDeckState) {
        screenState = state
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val hostState = remember { SnackbarHostState() }
            CaroTheme {
                Box(modifier = Modifier.size(393.dp, 852.dp)) {
                    // 화면보다 먼저 두어 같은 구성에서 바뀌면 화면의 효과보다 먼저 스낵바를 띄운다.
                    LaunchedEffect(snackbarRequest) {
                        if (snackbarRequest > 0) hostState.showSnackbar(CaroSnackbarVisuals(MESSAGE, style = CaroSnackbarStyle.Error))
                    }
                    ImportDeckScreen(state = screenState, onIntent = {}, snackbarHostState = hostState)
                }
            }
        }
        settle()
    }

    private fun showWithSnackbar(state: ImportDeckState) {
        snackbarRequest = 1
        show(state)
    }

    /** [withSnackbar]면 상태를 바꾸는 같은 구성에서 스낵바도 띄운다. */
    private fun update(
        next: ImportDeckState,
        withSnackbar: Boolean = false,
    ) {
        composeRule.runOnIdle {
            screenState = next
            if (withSnackbar) snackbarRequest += 1
        }
        settle()
    }

    // 시계를 멈춘 동안 바꾼 상태는 먼저 idle을 기다려야 다음 프레임의 구성에 들어간다.
    private fun settle() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SLIDE_IN_MILLIS)
        composeRule.waitForIdle()
    }

    private fun snackbarBottom(): Dp =
        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss) and hasAnyDescendant(hasText(MESSAGE)))
            .getUnclippedBoundsInRoot()
            .bottom

    private fun buttonTop(label: String): Dp = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot().top

    @Test
    fun `미리보기의 오류 스낵바는 가져오기 위 16에 뜬다`() {
        showWithSnackbar(preview)

        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
        abs((buttonTop("가져오기") - snackbarBottom() - GAP).value) shouldBeLessThan TOLERANCE
    }

    @Test
    fun `덱 만드는 중 덮개가 뜨면 화면 안 스낵바를 닫는다`() {
        showWithSnackbar(preview)
        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()

        update(preview.copy(loading = ImportLoading.CreatingDeck))

        composeRule.onNodeWithText(MESSAGE).assertDoesNotExist()
    }

    @Test
    fun `단계가 바뀌면 화면 안 스낵바를 닫는다`() {
        showWithSnackbar(preview)
        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()

        update(ImportDeckState(step = ImportStep.Saving, saving = SavingProgress(deckName = "JLPT N5", totalCards = 6)))

        composeRule.onNodeWithText(MESSAGE).assertDoesNotExist()
    }

    @Test
    fun `덮개를 걷으면서 띄운 입력 거절 스낵바는 닫지 않는다`() {
        show(preview.copy(loading = ImportLoading.CreatingDeck))

        update(preview, withSnackbar = true)

        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `사유 줄로 하단 영역이 높아져도 스낵바는 하단 버튼 위 16에 뜬다`() {
        showWithSnackbar(SettingsSamples.sheetState(sheetIndex = 2))

        composeRule.onNodeWithText("비어 있는 시트예요. 다른 시트를 골라 주세요.").assertIsDisplayed()
        abs((buttonTop("적용") - snackbarBottom() - GAP).value) shouldBeLessThan TOLERANCE
    }

    private companion object {
        const val MESSAGE = "덱을 만들지 못했어요. 이름과 설명을 확인해 주세요."
        const val SLIDE_IN_MILLIS = 500L
        val GAP = 16.dp
        const val TOLERANCE = 0.5f
    }
}
