package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.focusable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.whatever.caro.core.designsystem.themes.CaroTheme

/** 제목에 붙지 않은 FocusRequester에 포커스를 요청하면 경고만 남고 포커스가 가지 않으므로, 제목이 붙어 있을 때만 요청한다. */
internal class ImportTitleFocus {
    private var requester: FocusRequester? = null

    fun attach(focusRequester: FocusRequester) {
        requester = focusRequester
    }

    fun detach(focusRequester: FocusRequester) {
        if (requester === focusRequester) requester = null
    }

    fun requestFocus() {
        requester?.requestFocus()
    }
}

internal val LocalImportTitleFocus = compositionLocalOf<ImportTitleFocus?> { null }

@Composable
internal fun ImportStepTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val titleFocus = LocalImportTitleFocus.current
    DisposableEffect(titleFocus, focusRequester) {
        titleFocus?.attach(focusRequester)
        onDispose { titleFocus?.detach(focusRequester) }
    }

    Text(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .focusable()
                .semantics { heading() },
        text = text,
        style = CaroTheme.typography.heading1,
        color = CaroTheme.color.text.primary,
    )
}
