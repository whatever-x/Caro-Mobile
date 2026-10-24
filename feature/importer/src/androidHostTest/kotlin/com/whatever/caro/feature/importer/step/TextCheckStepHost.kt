package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable

internal class TextCheckStepHost(
    private val composeRule: ComposeContentTestRule,
) {
    val delimiters = mutableListOf<Char>()
    val headerChanges = mutableListOf<Boolean>()
    var nextClicks = 0
        private set

    /** [manualClock]은 로딩 카드의 무한 반복 Lottie 때문에 자동 시계를 켜 두면 waitForIdle이 끝나지 않을 때 쓴다. */
    fun show(
        table: ParsedTable = TextCheckStepTables.text(),
        delimiter: Char = '\t',
        hasHeaderRow: Boolean = false,
        isReparsing: Boolean = false,
        width: Dp = 393.dp,
        height: Dp = 700.dp,
        fontScale: Float = 1f,
        manualClock: Boolean = false,
    ) {
        if (manualClock) composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        TextCheckStep(
                            table = table,
                            delimiter = delimiter,
                            hasHeaderRow = hasHeaderRow,
                            isReparsing = isReparsing,
                            onSelectDelimiter = { selected -> delimiters += selected },
                            onChangeHeaderRow = { checked -> headerChanges += checked },
                            onNext = { nextClicks += 1 },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        if (manualClock) composeRule.mainClock.advanceTimeByFrame() else composeRule.waitForIdle()
    }
}

/** 병합 여부와 상관없이 스크린 리더가 한 번에 읽는 문구를 모은다. 사용자 지정 설명이 있으면 그것이고 없으면 글자다. */
internal fun SemanticsNode.readings(): List<String> {
    val descriptions = if (SemanticsProperties.ContentDescription in config) config[SemanticsProperties.ContentDescription] else emptyList()
    val texts = if (SemanticsProperties.Text in config) config[SemanticsProperties.Text].map { text -> text.text } else emptyList()
    return descriptions + texts
}

internal fun readsAs(expected: String): SemanticsMatcher =
    SemanticsMatcher("스크린 리더가 `$expected`로 읽는다") { node ->
        expected in
            node.readings()
    }
