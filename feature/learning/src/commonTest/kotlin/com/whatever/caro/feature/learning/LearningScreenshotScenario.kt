package com.whatever.caro.feature.learning

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.learning_front_instruction
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.feature.learning.mvi.LearningState

internal object LearningScreenshotScenario {
    const val ID = "learning.learning.front"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.learning_front_instruction)),
                beforeCapture = { strings ->
                    onNodeWithText(
                        strings[Res.string.learning_front_instruction],
                    ).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            LearningScreen(
                state =
                    LearningState(
                        isLoading = false,
                        sessionId = 7,
                        totalCount = 1,
                        cards = listOf(StudyCard(1, "Apple", "사과")),
                    ),
                onIntent = {
                },
            )
        }
    }
}
