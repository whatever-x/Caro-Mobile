package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.mvi.SavingProgress

internal object ImportSavingPausedScreenshotScenario {
    const val ID = "importer.saving.paused"

    private const val SAVED_CARDS = 1_000

    // 저장한 장수가 있으면 막대는 진행률 모양이라 동작 줄이기 설정과 무관하게 움직이지 않는다.
    @Composable
    fun Render() {
        ImportScreenshotScenario.Render(
            ImportDeckState(
                step = ImportStep.Saving,
                saving =
                    SavingProgress(
                        deckName = ImportSaveScreenshotScenario.DECK_NAME,
                        totalCards = ImportSaveScreenshotScenario.TOTAL_CARDS,
                        savedCards = SAVED_CARDS,
                    ),
                dialog = ImportDialog.Paused(saved = SAVED_CARDS, reason = PauseReason.NETWORK),
            ),
        )
    }
}
