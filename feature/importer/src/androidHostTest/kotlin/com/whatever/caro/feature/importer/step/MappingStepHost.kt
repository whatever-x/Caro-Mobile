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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.logic.toggleField
import kotlinx.collections.immutable.PersistentMap

internal data class FieldToggle(
    val noteTypeId: String,
    val target: String,
    val fieldName: String,
)

/**
 * [applyEdits]가 켜져 있으면 ViewModel처럼 요청받은 변경을 연결에 바로 반영해 다시 그린다.
 * 꺼져 있으면 요청만 기록하고 화면은 처음 받은 연결 그대로다.
 */
internal class MappingStepHost(
    private val composeRule: ComposeContentTestRule,
) {
    val typeToggles = mutableListOf<String>()
    val fieldToggles = mutableListOf<FieldToggle>()
    var nextClicks = 0
        private set

    /** 내용 상자의 너비(px)다. 루트는 창 전체 너비라 좁은 화면 확인에는 쓸 수 없다. */
    var contentWidthPx = 0f
        private set

    private var mappings by mutableStateOf<PersistentMap<String, FieldMapping>>(MappingStepTables.mappingsOf())

    fun show(
        table: ParsedTable,
        mappings: PersistentMap<String, FieldMapping> = MappingStepTables.defaultMappings(table),
        applyEdits: Boolean = false,
        width: Dp = 393.dp,
        height: Dp = 700.dp,
        fontScale: Float = 1f,
    ) {
        this.mappings = mappings
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    contentWidthPx = with(LocalDensity.current) { width.toPx() }
                    Box(modifier = Modifier.size(width, height)) {
                        MappingStep(
                            table = table,
                            mappings = this@MappingStepHost.mappings,
                            onToggleNoteType = { id ->
                                typeToggles += id
                                if (applyEdits) flipExcluded(id)
                            },
                            onToggleField = { id, target, fieldName ->
                                fieldToggles += FieldToggle(id, target, fieldName)
                                if (applyEdits) pick(id, target, fieldName)
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

    private fun flipExcluded(id: String) {
        val current = mappings[id] ?: return
        mappings = mappings.put(id, current.copy(excluded = !current.excluded))
    }

    private fun pick(
        id: String,
        target: String,
        fieldName: String,
    ) {
        val current = mappings[id] ?: return
        mappings = mappings.put(id, toggleField(current, target, fieldName).mapping)
    }
}

internal val isCheckboxRole: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

internal val isButtonRole: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

internal val isPoliteRegion: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

internal val hasVerticalScrollRange: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

internal val hasHorizontalScrollRange: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

/** 읽는 문구에 상태 설명까지 더한다. 사용할 수 없음을 글자로 읽든 상태 설명으로 읽든 같은 기준으로 확인하려는 것이다. */
internal fun SemanticsNode.spokenParts(): List<String> = readings() + listOfNotNull(config.getOrNull(SemanticsProperties.StateDescription))

/** 고른 칩. 항목 이름과 면, 고른 순서를 한 문장으로 읽는다. */
internal fun chosenChip(
    name: String,
    side: String,
    order: Int,
): SemanticsMatcher = isCheckboxRole and readsAs("$name, 선택됨, $side ${order}번째")

/** 그 면에 고르지 않은 칩. 이름만 읽는다. */
internal fun plainChip(name: String): SemanticsMatcher = isCheckboxRole and readsAs(name)

/** 끈 유형의 칩. 이름과 사용할 수 없음을 함께 읽는다. */
internal fun unavailableChip(name: String): SemanticsMatcher =
    SemanticsMatcher("스크린 리더가 `$name`과 사용할 수 없음을 함께 읽는 체크 역할") { node ->
        val parts = node.spokenParts()
        node.config.getOrNull(SemanticsProperties.Role) == Role.Checkbox &&
            parts.any { part -> name in part } &&
            parts.any { part -> UNAVAILABLE in part }
    }

internal fun readsContaining(snippet: String): SemanticsMatcher =
    SemanticsMatcher("스크린 리더가 `$snippet`을 포함해 읽는다") { node -> node.spokenParts().any { part -> snippet in part } }

private const val UNAVAILABLE = "사용할 수 없음"
