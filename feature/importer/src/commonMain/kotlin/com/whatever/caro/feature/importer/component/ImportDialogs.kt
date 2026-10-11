package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportDismissPolicy
import kotlinx.collections.immutable.ImmutableList

/** 창을 띄우지 않고 내용만 확인하는 테스트가 틀을 바꿔 끼울 수 있도록 분리했다. */
internal typealias ImportDialogFrame =
    @Composable (
        onDismissRequest: () -> Unit,
        properties: DialogProperties,
        title: @Composable () -> Unit,
        buttons: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) -> Unit

private val CaroImportDialogFrame: ImportDialogFrame = { onDismissRequest, properties, title, buttons, content ->
    CaroDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        title = title,
        buttons = buttons,
        content = content,
    )
}

@Composable
internal fun ImportDialogs(
    dialog: ImportDialog?,
    onIntent: (ImportDeckIntent) -> Unit,
    frame: ImportDialogFrame = CaroImportDialogFrame,
) {
    if (dialog == null) return
    val content = importDialogContent(dialog) ?: return
    val policy = dialog.dismissPolicy
    val properties =
        remember(policy) {
            DialogProperties(
                dismissOnBackPress = policy.dismissOnBackPress,
                dismissOnClickOutside = policy.dismissOnClickOutside,
                usePlatformDefaultWidth = false,
            )
        }

    frame(
        {
            // 닫을 수 없는 대화상자의 닫기 요청은 ViewModel까지 보내지 않는다.
            if (policy != ImportDismissPolicy.NotDismissible) onIntent(ImportDeckIntent.DismissDialog)
        },
        properties,
        { ImportDialogTitle(content.title) },
        { ImportDialogButtons(content.buttons, onIntent) },
        { ImportDialogBody(content.body) },
    )
}

@Composable
private fun ImportDialogTitle(text: String) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(text) { focusRequester.requestFocus() }

    Text(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .semantics { heading() },
        text = text,
        color = CaroTheme.color.text.primary,
        style = CaroTheme.typography.heading2,
    )
}

@Composable
private fun ImportDialogBody(text: String) {
    Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        color = CaroTheme.color.text.secondary,
        style = CaroTheme.typography.body2.medium,
    )
    Spacer(modifier = Modifier.size(CaroTheme.spacing.l))
}

@Composable
private fun ImportDialogButtons(
    buttons: ImmutableList<ImportDialogButton>,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        buttons.forEach { button ->
            CaroDialogButton(
                modifier = Modifier.weight(1f),
                text = button.label,
                backgroundColor = button.style.backgroundColor(),
                textColor = button.style.textColor(),
                onClick = { onIntent(button.intent) },
            )
        }
    }
}

@Composable
private fun ImportDialogButtonStyle.backgroundColor(): Color =
    when (this) {
        ImportDialogButtonStyle.Brand -> CaroTheme.color.surface.brand
        ImportDialogButtonStyle.Dangerous -> CaroTheme.color.surface.dangerous
        ImportDialogButtonStyle.Tertiary -> CaroTheme.color.surface.tertiary
    }

@Composable
private fun ImportDialogButtonStyle.textColor(): Color =
    when (this) {
        ImportDialogButtonStyle.Brand -> CaroTheme.color.text.inverse
        ImportDialogButtonStyle.Dangerous -> CaroTheme.color.text.dangerous
        ImportDialogButtonStyle.Tertiary -> CaroTheme.color.text.brand
    }
