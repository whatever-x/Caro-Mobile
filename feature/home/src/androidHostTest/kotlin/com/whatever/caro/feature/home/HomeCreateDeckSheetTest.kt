package com.whatever.caro.feature.home

import androidx.activity.ComponentDialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class HomeCreateDeckSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<HomeIntent>()

    private fun showHome(isSheetVisible: Boolean) {
        composeRule.setContent {
            CaroTheme {
                HomeScreen(
                    state =
                        HomeState(
                            nickname = "승우",
                            streakState = HomeStreakState.NotStarted,
                            isLoading = false,
                            isCreateDeckBottomSheetVisible = isSheetVisible,
                        ),
                    onIntent = { intents += it },
                )
            }
        }
    }

    @Test
    fun `덱 만들기 버튼은 시트 열기를 보낸다`() {
        showHome(isSheetVisible = false)

        composeRule.onNodeWithText("덱 만들기").performClick()

        intents shouldBe listOf(HomeIntent.ClickCreateDeckButton)
    }

    @Test
    fun `시트는 직접 만들기와 가져와서 만들기를 설명과 함께 보여준다`() {
        showHome(isSheetVisible = true)

        composeRule.onNodeWithText("직접 만들기").assertIsDisplayed()
        composeRule.onNodeWithText("카드를 한 장씩 입력해 덱을 만들어요").assertIsDisplayed()
        composeRule.onNodeWithText("가져와서 만들기").assertIsDisplayed()
        composeRule.onNodeWithText("Anki, Quizlet, 엑셀 단어장으로 덱을 만들어요").assertIsDisplayed()
    }

    @Test
    fun `시트의 직접 만들기는 직접 만들기 선택을 보낸다`() {
        showHome(isSheetVisible = true)

        composeRule.onNodeWithText("직접 만들기").performClick()

        intents shouldBe listOf(HomeIntent.ClickCreateDeck)
    }

    @Test
    fun `시트의 가져와서 만들기는 가져오기 선택을 보낸다`() {
        showHome(isSheetVisible = true)

        composeRule.onNodeWithText("가져와서 만들기").performClick()

        intents shouldBe listOf(HomeIntent.ClickImportDeck)
    }

    @Test
    fun `시트가 열린 상태에서 뒤로 가기를 누르면 닫기만 보내고 화면은 옮기지 않는다`() {
        showHome(isSheetVisible = true)
        composeRule.waitForIdle()

        val sheetDialog = ShadowDialog.getLatestDialog() as ComponentDialog
        composeRule.runOnUiThread { sheetDialog.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        intents shouldBe listOf(HomeIntent.DismissCreateDeckBottomSheet)
    }

    @Test
    fun `시트가 숨겨져 있으면 선택지를 그리지 않는다`() {
        showHome(isSheetVisible = false)

        composeRule.onNodeWithText("가져와서 만들기").assertDoesNotExist()
    }
}
