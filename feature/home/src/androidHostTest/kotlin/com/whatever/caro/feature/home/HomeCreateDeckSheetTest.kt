package com.whatever.caro.feature.home

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
    fun `시트의 직접 만들기는 직접 만들기 선택을 보낸다`() {
        showHome(isSheetVisible = true)

        composeRule.onNodeWithText("직접 만들기").performClick()

        intents shouldBe listOf(HomeIntent.ClickCreateDeck)
    }

    @Test
    fun `시트의 자료 가져오기는 가져오기 선택을 보낸다`() {
        showHome(isSheetVisible = true)

        composeRule.onNodeWithText("자료 가져오기").performClick()

        intents shouldBe listOf(HomeIntent.ClickImportDeck)
    }

    @Test
    fun `시트가 숨겨져 있으면 선택지를 그리지 않는다`() {
        showHome(isSheetVisible = false)

        composeRule.onNodeWithText("자료 가져오기").assertDoesNotExist()
    }
}
