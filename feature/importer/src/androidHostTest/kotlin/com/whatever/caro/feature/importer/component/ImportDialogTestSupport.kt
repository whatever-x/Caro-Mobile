package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.window.DialogProperties
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog

/** 대화상자 창 없이 내용을 그 자리에 그리고, 호스트가 넘긴 닫기 콜백과 창 속성을 붙잡는다. */
internal class RecordingDialogFrame {
    var onDismissRequest: (() -> Unit)? = null
        private set
    var properties: DialogProperties? = null
        private set

    val frame: ImportDialogFrame =
        { dismissRequest, dialogProperties, title, buttons, content ->
            SideEffect {
                onDismissRequest = dismissRequest
                properties = dialogProperties
            }
            Column {
                title()
                content()
                buttons()
            }
        }
}

internal fun ComposeContentTestRule.showImportDialog(
    dialog: ImportDialog?,
    recording: RecordingDialogFrame = RecordingDialogFrame(),
    onIntent: (ImportDeckIntent) -> Unit = {},
): RecordingDialogFrame {
    setContent {
        CaroTheme {
            ImportDialogs(dialog = dialog, onIntent = onIntent, frame = recording.frame)
        }
    }
    waitForIdle()
    return recording
}

internal val isHeading: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

internal val isButton: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

internal fun ComposeContentTestRule.titleNode(): SemanticsNodeInteraction = onNode(isHeading)

/** 왼쪽에 놓인 버튼부터 라벨을 돌려준다. */
internal fun ComposeContentTestRule.buttonLabelsLeftToRight(): List<String> =
    onAllNodes(isButton)
        .fetchSemanticsNodes()
        .sortedBy { node -> node.boundsInRoot.left }
        .map { node -> node.config[SemanticsProperties.Text].joinToString("") { text -> text.text } }

/** [snippet]을 포함하는 글자 노드 하나의 전체 글자를 돌려준다. */
internal fun ComposeContentTestRule.textContaining(snippet: String): String =
    onNode(hasText(snippet, substring = true))
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString("") { text -> text.text }
