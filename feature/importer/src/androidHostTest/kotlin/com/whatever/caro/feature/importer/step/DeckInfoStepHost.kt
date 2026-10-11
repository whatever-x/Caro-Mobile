package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

/** 값이 오면 기록만 하고 화면은 처음 받은 값 그대로다. 입력이 바뀐 뒤의 화면은 Screen을 통한 테스트가 본다. */
internal class DeckInfoStepHost(
    private val composeRule: ComposeContentTestRule,
) {
    val nameChanges = mutableListOf<String>()
    val descriptionChanges = mutableListOf<String>()
    var confirmClicks = 0
        private set

    fun show(
        name: String = NAME,
        description: String = DESCRIPTION,
        cardCount: Int = CARD_COUNT,
        isConfirmEnabled: Boolean = true,
        width: Dp = 393.dp,
        height: Dp = 800.dp,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        DeckInfoStep(
                            name = name,
                            description = description,
                            cardCount = cardCount,
                            isConfirmEnabled = isConfirmEnabled,
                            onNameChange = { nameChanges += it },
                            onDescriptionChange = { descriptionChanges += it },
                            onConfirm = { confirmClicks += 1 },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    companion object {
        const val NAME = "JLPT N5 단어"
        const val DESCRIPTION = "JLPT N5 단어.apkg에서 가져왔어요"
        const val CARD_COUNT = 1_512
        const val SUMMARY = "카드 1,512장으로 새 덱을 만들어요."
        const val CONFIRM = "가져오기"
    }
}

/** 입력칸은 현재 값으로 찾는다. 이름과 설명이 같은 입력 동작을 가지므로 값이 서로 달라야 구분된다. */
internal fun inputFieldWith(value: String): SemanticsMatcher = hasSetTextAction() and hasText(value)

/**
 * 화면에 있는 세로 스크롤 가운데 가장 많이 움직일 수 있는 양이다. 스크롤 영역이 없으면 0이다.
 * 내용이 다 들어가는 화면은 스크롤하지 않는다는 판정은 이 값이 0인지로 한다.
 */
internal fun ComposeContentTestRule.verticalScrollMax(): Float =
    onAllNodes(hasVerticalScrollRange)
        .fetchSemanticsNodes()
        .maxOfOrNull { node -> node.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() } ?: 0f
