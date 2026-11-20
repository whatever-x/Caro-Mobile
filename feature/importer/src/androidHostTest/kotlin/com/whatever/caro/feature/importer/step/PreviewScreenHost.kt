package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.logic.applyingDeckInfo
import com.whatever.caro.feature.importer.logic.filterCards
import com.whatever.caro.feature.importer.logic.openingDeckInfo
import com.whatever.caro.feature.importer.logic.withDeckInfoDescription
import com.whatever.caro.feature.importer.logic.withDeckInfoName
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.transform.ImportCard
import io.kotest.matchers.shouldBe

/** 미리보기 입력과 덱 정보 시트 입력을 ViewModel과 같은 방식으로 상태에 반영해 다시 그린다. 보낸 입력은 [intents]에 남는다. */
internal class PreviewScreenHost(
    private val composeRule: ComposeContentTestRule,
) {
    val intents = mutableListOf<ImportDeckIntent>()
    var state by mutableStateOf(ImportDeckState())
        private set
    private var inputModeManager: InputModeManager? = null

    /** 테스트의 키 입력은 터치 모드를 끝내지 않으므로, 하드웨어 키보드를 쓸 때처럼 키보드 입력 방식으로 직접 바꾼다. */
    fun useKeyboard() {
        composeRule.runOnIdle { inputModeManager?.requestInputMode(InputMode.Keyboard) shouldBe true }
        composeRule.waitForIdle()
    }

    fun show(
        initial: ImportDeckState,
        width: Dp = 393.dp,
        height: Dp = 852.dp,
        fontScale: Float = 1f,
    ) {
        state = initial
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        ImportDeckScreen(state = state, onIntent = ::onIntent)
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun onIntent(intent: ImportDeckIntent) {
        intents += intent
        state =
            when (intent) {
                is ImportDeckIntent.SelectPreviewFilter -> {
                    val cards = state.preview?.cards ?: return
                    state.copy(previewFilter = intent.filter, filteredCards = filterCards(cards, intent.filter))
                }

                is ImportDeckIntent.ClickCard -> {
                    state.copy(detailCard = intent.card)
                }

                ImportDeckIntent.CloseCardDetail -> {
                    state.copy(detailCard = null)
                }

                ImportDeckIntent.DismissDelimiterBanner -> {
                    state.copy(isDelimiterBannerDismissed = true)
                }

                else -> {
                    state.withDeckInfoIntent(intent)
                }
            }
    }

    private fun ImportDeckState.withDeckInfoIntent(intent: ImportDeckIntent): ImportDeckState =
        when (intent) {
            ImportDeckIntent.ClickChangeDeckInfo -> openingDeckInfo()
            is ImportDeckIntent.UpdateDeckInfoName -> withDeckInfoName(intent.name)
            is ImportDeckIntent.UpdateDeckInfoDescription -> withDeckInfoDescription(intent.description)
            ImportDeckIntent.ApplyDeckInfo -> applyingDeckInfo()
            ImportDeckIntent.DismissDeckInfo -> copy(deckInfoDraft = null)
            else -> this
        }
}

/** 카드 항목이다. 뒷면이 같은 빈 면 문구인 카드가 여럿이라 앞면과 뒷면 글로 함께 찾는다. */
internal fun cardItem(card: ImportCard): SemanticsMatcher = hasClickAction() and hasText(card.front) and hasText(card.back)

/** 카드 전체 보기 대화상자 안의 노드만 찾는다. 같은 글이 목록의 카드에도 있다. */
internal fun inDialog(matcher: SemanticsMatcher): SemanticsMatcher = matcher and hasAnyAncestor(isDialog())

internal fun SemanticsNodeInteraction.bounds(): DpRect = getUnclippedBoundsInRoot()

internal fun SemanticsNodeInteraction.horizontalScrollMax(): Float =
    fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue()

/** 글자 노드 하나의 줄 배치 결과다. 카드와 설정 줄은 병합되므로 병합하지 않은 트리에서 찾아야 글자 노드가 나온다. */
internal fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.single()
}
