package com.whatever.caro.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.repository.streak.StreakRepository
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.model.streak.StreakStatus
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.ImportDeckEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.route.HomeRoute
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class HomeRouteNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val emittedCommands = mutableListOf<NavCommand>()

    private val navDispatcher =
        object : NavigationDispatcher {
            override val commands: Flow<NavCommand> = emptyFlow()

            override suspend fun emit(command: NavCommand) {
                emittedCommands += command
            }
        }

    private val snackbarController =
        object : SnackbarController {
            override val messages: Flow<SnackBarMessage> = emptyFlow()

            override suspend fun show(message: SnackBarMessage) = Unit
        }

    private fun showHomeAndOpenSheet() {
        val viewModel =
            HomeViewModel(
                deckRepository = mock<DeckRepository> { everySuspend { getDecks() } returns emptyList() },
                streakRepository =
                    mock<StreakRepository> {
                        everySuspend { getStreak() } returns Streak(status = StreakStatus.NOT_STARTED, currentDays = 0)
                    },
                profileRepository = mock<ProfileRepository> { everySuspend { getMyNickname() } returns "캐로" },
                exceptionFilter = ExceptionFilter.None,
            )
        composeRule.setContent {
            CaroTheme {
                HomeRoute(
                    viewModel = viewModel,
                    navDispatcher = navDispatcher,
                    snackbarController = snackbarController,
                )
            }
        }
        composeRule.waitUntil { composeRule.onAllNodesWithText("덱 만들기").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("덱 만들기").performClick()
    }

    @Test
    fun `시트의 자료 가져오기는 가져오기 화면으로 이동한다`() {
        showHomeAndOpenSheet()

        composeRule.onNodeWithText("자료 가져오기").performClick()
        composeRule.waitUntil { emittedCommands.isNotEmpty() }

        emittedCommands shouldBe listOf(NavCommand.To(ImportDeckEntry))
    }

    @Test
    fun `시트의 직접 만들기는 덱 만들기 화면으로 이동한다`() {
        showHomeAndOpenSheet()

        composeRule.onNodeWithText("직접 만들기").performClick()
        composeRule.waitUntil { emittedCommands.isNotEmpty() }

        emittedCommands shouldBe listOf(NavCommand.To(CreateDeckEntry))
    }

    @Test
    fun `시트를 선택하면 시트가 닫힌다`() {
        showHomeAndOpenSheet()

        composeRule.onNodeWithText("자료 가져오기").performClick()
        composeRule.waitUntil { composeRule.onAllNodesWithText("자료 가져오기").fetchSemanticsNodes().isEmpty() }
    }
}
