package com.whatever.caro.feature.importer.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_title_saving
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.ImportTopBar
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.step.SavingStep
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewStats
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

internal object ImportSaveScreenshotScenario {
    const val DECK_INFO_PREFILLED_ID = "importer.deck_info.prefilled"
    const val SAVING_PROGRESS_ID = "importer.saving.progress"

    private const val DECK_NAME = "JLPT N5 단어"
    private const val TOTAL_CARDS = 1_512

    // 덱 정보는 카드 장수만 읽으므로 카드 목록은 비워 둔다.
    private val PREVIEW =
        ImportPreview(
            cards = persistentListOf(),
            stats =
                ImportPreviewStats(
                    cardCount = TOTAL_CARDS,
                    excludedCount = 2,
                    emptyReplacedCount = 0,
                    truncatedCount = 0,
                    mediaRemovedCount = 0,
                ),
            warnings = emptyList(),
            limitExceeded = false,
        )

    @Composable
    fun RenderDeckInfoPrefilled() {
        ImportScreenshotScenario.Render(
            ImportDeckState(
                step = ImportStep.DeckInfo,
                preview = PREVIEW,
                deckName = DECK_NAME,
                deckDescription = "$DECK_NAME.apkg에서 가져왔어요",
            ),
        )
    }

    // 동작 줄이기는 기기 설정을 따라 달라지므로 화면 대신 단계를 직접 그려 값을 고정한다. 진행률이 정해져 있어 흐르는 막대는 나오지 않는다.
    @Composable
    fun RenderSavingProgress() {
        CaroTheme {
            Column(modifier = Modifier.fillMaxSize().background(CaroTheme.color.background.primary)) {
                ImportTopBar(title = stringResource(Res.string.import_title_saving), onBack = {})
                SavingStep(
                    progress = SavingProgress(deckName = DECK_NAME, totalCards = TOTAL_CARDS, savedCards = 1_000),
                    reduceMotion = true,
                    onStop = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
