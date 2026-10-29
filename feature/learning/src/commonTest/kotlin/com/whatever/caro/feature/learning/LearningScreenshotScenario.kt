package com.whatever.caro.feature.learning

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.feature.learning.mvi.LearningState

internal object LearningScreenshotScenario {
    const val ID = "learning.learning.front"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

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
