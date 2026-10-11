package com.whatever.caro.core.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatever.caro.core.designsystem.themes.CaroTheme

data class CaroFullTextSection(
    val title: String?,
    val body: String,
)

/** [sections]는 인스턴스 동일성으로 비교되므로 호출부가 remember로 같은 목록을 유지한다. */
@Composable
fun CaroFullTextDialog(
    sections: List<CaroFullTextSection>,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(onDismiss) {
                        detectTapGestures { onDismiss() }
                    }.padding(CaroTheme.spacing.l),
            contentAlignment = Alignment.Center,
        ) {
            val dialogWidth = minOf(maxWidth, FullTextDialogWidth)
            val dialogHeight = minOf(maxHeight, FullTextDialogHeight)

            Surface(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .height(dialogHeight)
                        .pointerInput(Unit) { detectTapGestures {} },
                shape = CaroTheme.shape.l,
                color = CaroTheme.color.surface.primary,
                border = BorderStroke(FullTextDialogBorderWidth, CaroTheme.color.border.secondary),
                shadowElevation = FullTextDialogElevation,
            ) {
                FullTextSections(sections = sections)
            }
        }
    }
}

@Composable
private fun FullTextSections(sections: List<CaroFullTextSection>) {
    val focusRequester = remember { FocusRequester() }

    // 포커스 대상이 Dialog 창 안에 붙으므로 requestFocus도 Dialog content의 composition에서 호출해야 한다.
    LaunchedEffect(Unit) {
        if (sections.isNotEmpty()) focusRequester.requestFocus()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = CaroTheme.spacing.s,
                    vertical = CaroTheme.spacing.xl4,
                ).verticalScroll(rememberScrollState()),
    ) {
        sections.forEachIndexed { index, section ->
            if (index > 0) Spacer(Modifier.height(CaroTheme.spacing.xl))

            // 스크린 리더가 위쪽 내용을 건너뛰지 않도록 제목 유무와 관계없이 첫 구역에서 시작한다.
            val anchorModifier =
                Modifier
                    .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                    .focusable()

            if (section.title != null) {
                Text(
                    text = section.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(anchorModifier)
                            .semantics { heading() },
                    style = CaroTheme.typography.caption1.medium,
                    color = CaroTheme.color.text.tertiary,
                )
                Spacer(Modifier.height(CaroTheme.spacing.xs))
            }
            Text(
                text = section.body,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (section.title == null) anchorModifier else Modifier),
                style = CaroTheme.typography.body1,
                color = CaroTheme.color.text.primary,
            )
        }
    }
}

private val FullTextDialogWidth = 362.dp
private val FullTextDialogHeight = 642.dp
private val FullTextDialogBorderWidth = 1.dp
private val FullTextDialogElevation = 16.dp
