package com.whatever.caro.feature.learning

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.feature.learning.mvi.LearningState

internal object LearningScreenshotScenario {
    const val ID = "learning.learning.front"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

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
