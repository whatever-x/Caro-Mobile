package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
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
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview

/**
 * [applyEdits]가 켜져 있으면 ViewModel처럼 요청받은 필터와 카드 전체 보기를 바로 반영해 다시 그린다.
 * 꺼져 있으면 요청만 기록하고 화면은 처음 받은 값 그대로다.
 */
internal class PreviewStepHost(
    private val composeRule: ComposeContentTestRule,
) {
    val filterRequests = mutableListOf<ImportPreviewFilter>()
    val cardClicks = mutableListOf<ImportCard>()
    var dismissDetailCount = 0
        private set
    var nextClicks = 0
        private set

    private var filter by mutableStateOf(ImportPreviewFilter.All)
    private var detailCard by mutableStateOf<ImportCard?>(null)

    fun show(
        preview: ImportPreview = PreviewStepTables.preview(),
        format: ImportFormat = ImportFormat.APKG,
        isPaste: Boolean = false,
        filter: ImportPreviewFilter = ImportPreviewFilter.All,
        detailCard: ImportCard? = null,
        maxCards: Int = MAX_CARDS,
        applyEdits: Boolean = false,
        width: Dp = 393.dp,
        height: Dp = 800.dp,
        fontScale: Float = 1f,
    ) {
        this.filter = filter
        this.detailCard = detailCard
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        PreviewStep(
                            preview = preview,
                            format = format,
                            isPaste = isPaste,
                            filter = this@PreviewStepHost.filter,
                            detailCard = this@PreviewStepHost.detailCard,
                            maxCards = maxCards,
                            onSelectFilter = { selected ->
                                filterRequests += selected
                                if (applyEdits) this@PreviewStepHost.filter = selected
                            },
                            onClickCard = { card ->
                                cardClicks += card
                                if (applyEdits) this@PreviewStepHost.detailCard = card
                            },
                            onDismissDetail = {
                                dismissDetailCount += 1
                                if (applyEdits) this@PreviewStepHost.detailCard = null
                            },
                            onNext = { nextClicks += 1 },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    companion object {
        const val MAX_CARDS = 10_000
    }
}

internal val isTabRole: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

/** 카드 항목이다. 앞면과 뒷면 글이 모두 같은 항목이어야 하므로 두 글로 찾는다. 뒷면이 같은 빈 문구인 카드가 여럿이다. */
internal fun cardItem(card: ImportCard): SemanticsMatcher = hasClickAction() and hasText(card.front) and hasText(card.back)

/** 카드 전체 보기 대화상자 안의 노드만 찾는다. 같은 글이 목록의 카드에도 있다. */
internal fun inDialog(matcher: SemanticsMatcher): SemanticsMatcher = matcher and hasAnyAncestor(isDialog())

internal fun SemanticsNodeInteraction.bounds(): DpRect = getUnclippedBoundsInRoot()

internal fun SemanticsNodeInteraction.horizontalScrollMax(): Float =
    fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue()

/** 목록에서 벗어나 합성에서 빠졌거나 잘려 나간 노드는 보이지 않는 것으로 센다. 잘린 경계 상자의 크기가 0이면 화면에 닿지 않은 것이다. */
internal fun ComposeContentTestRule.hasNoVisibleNode(matcher: SemanticsMatcher): Boolean =
    onAllNodes(matcher).fetchSemanticsNodes().none { node -> node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f }

/** 글자 노드 하나의 줄 배치 결과다. 카드 안의 글은 병합하지 않은 트리에서 찾아야 글자 노드가 나온다. */
internal fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.single()
}
