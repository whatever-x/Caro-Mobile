package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_hint_paste_columns
import caromobile.core.designsystem.generated.resources.import_hint_paste_row
import caromobile.core.designsystem.generated.resources.import_hint_paste_sides_later
import caromobile.core.designsystem.generated.resources.import_reason_paste_too_long
import caromobile.core.designsystem.generated.resources.import_subtitle_paste
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportBottomBar
import com.whatever.caro.feature.importer.components.ImportBulletList
import com.whatever.caro.feature.importer.components.ImportStepTitle
import com.whatever.caro.feature.importer.logic.PasteInputMinHeight
import com.whatever.caro.feature.importer.logic.shouldHidePasteGuide
import org.jetbrains.compose.resources.stringResource

/** 화면은 글을 자르지 않고 [다음]만 막는다. ViewModel은 상한보다 1자 많은 데까지 보관하므로, 넘친 글이 상한 길이로 잘려 [다음]이 열리는 일이 없다. */
@Composable
internal fun PasteStep(
    text: String,
    maxPasteChars: Int,
    isOverflowed: Boolean,
    isNextEnabled: Boolean,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tooLongReason = if (isOverflowed) stringResource(Res.string.import_reason_paste_too_long, maxPasteChars.formatWithComma()) else null

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
    ) {
        PasteInput(
            text = text,
            errorText = tooLongReason,
            onTextChange = onTextChange,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = CaroTheme.spacing.xl2,
                        end = CaroTheme.spacing.xl2,
                        top = CaroTheme.spacing.xl,
                        bottom = CaroTheme.spacing.xl2,
                    ),
        )
        ImportBottomBar(
            buttonText = stringResource(Res.string.import_button_next),
            onClick = onNext,
            enabled = isNextEnabled,
            disabledReason = tooLongReason,
        )
    }
}

/**
 * 길이 초과 오류 문구는 키보드가 열려도, 화면이 작아도 입력칸 바로 아래에 보여야 한다.
 * 그래서 오류 문구는 제목과 안내를 숨기는 조건을 따르지 않고, 오류 상태에서는 입력칸 최소 높이를 풀어 입력칸이 줄어들게 한다.
 */
@Composable
private fun PasteInput(
    text: String,
    errorText: String?,
    onTextChange: (String) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val isOverflowed = errorText != null
    val fieldLabel = stringResource(Res.string.import_subtitle_paste)

    // 제목과 안내를 보였을 때 잰 높이다. 숨긴 동안 입력칸이 이만큼 커지므로 보였다면 남았을 높이를 구하는 데 쓴다.
    var titleHeight by remember { mutableIntStateOf(0) }
    var guideHeight by remember { mutableIntStateOf(0) }
    var inputHeightWithGuide by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val hideGuide = shouldHidePasteGuide(rememberImeVisible(), with(density) { inputHeightWithGuide.toDp() })
    val hiddenHeight = titleHeight + if (isOverflowed) 0 else guideHeight

    Column(modifier = modifier) {
        if (!hideGuide) {
            ImportStepTitle(
                text = fieldLabel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size -> titleHeight = size.height }
                        .padding(bottom = CaroTheme.spacing.xl2),
            )
        }
        // 남는 높이는 weight가 준 자리를 Box로 재야 한다. requiredHeightIn 안쪽에서 재면 최소 높이로 올린 값이 나와 숨김 판정이 서지 않는다.
        // heightIn은 weight가 준 높이를 넘지 못해 쓰지 않는다.
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .onSizeChanged { size -> inputHeightWithGuide = if (hideGuide) size.height - hiddenHeight else size.height },
        ) {
            CaroTextArea(
                value = text,
                onValueChange = onTextChange,
                modifier = if (isOverflowed) Modifier else Modifier.requiredHeightIn(min = PasteInputMinHeight),
                fillHeight = true,
                semanticsLabel = fieldLabel,
            )
        }
        PasteError(text = errorText)
        if (!isOverflowed && !hideGuide) {
            PasteGuide(modifier = Modifier.onSizeChanged { size -> guideHeight = size.height }.padding(top = CaroTheme.spacing.s))
        }
    }
}

/** 키보드가 올라오는 애니메이션 동안 매 프레임 바뀌는 높이를 구성에서 읽으면 최대 100만 자 입력칸까지 다시 구성하므로 보이는지 여부만 읽는다. */
@Composable
private fun rememberImeVisible(): Boolean {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val isVisible by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    return isVisible
}

/** 새로 생긴 노드에는 live region 알림이 가지 않을 수 있어 오류 줄을 늘 두고 글자만 바꾼다. 오류가 없으면 높이를 차지하지 않는다. */
@Composable
private fun PasteError(text: String?) {
    Text(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (text == null) Modifier.height(0.dp) else Modifier.padding(top = CaroTheme.spacing.s))
                .semantics { liveRegion = LiveRegionMode.Polite },
        text = text.orEmpty(),
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.dangerous,
    )
}

@Composable
private fun PasteGuide(modifier: Modifier) {
    val row = stringResource(Res.string.import_hint_paste_row)
    val columns = stringResource(Res.string.import_hint_paste_columns)
    val sidesLater = stringResource(Res.string.import_hint_paste_sides_later)
    val lines = remember(row, columns, sidesLater) { listOf(row, columns, sidesLater) }

    ImportBulletList(lines = lines, modifier = modifier)
}
